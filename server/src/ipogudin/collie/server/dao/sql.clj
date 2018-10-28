(ns ipogudin.collie.server.dao.sql
  (:require [clj-time.coerce :as tc]
            [clojure.string :as string]
            [clojure.java.jdbc :as j]
            [mount.core :as mount]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.server.dao.common :as dao-common]
            [ipogudin.collie.server.dao.sql-common :as sql-common]
            [ipogudin.collie.server.configuration :refer [configuration]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol j/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (tc/from-sql-time v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (tc/to-local-date (tc/from-sql-date v)))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (org.joda.time.DateTime. v))
  java.util.Date
  (result-set-read-column [v _2 _3]
    (tc/from-date v)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
(extend-protocol j/ISQLValue
  org.joda.time.DateTime
  (sql-value [v]
    (tc/to-sql-time v))
  org.joda.time.LocalDate
  (sql-value [v]
    (tc/to-sql-date v)))

(def ^:private MAX_NUMBER_OF_NESTED_ENTITIES 256)
(declare get-dep)
(declare upsert)
(declare delete)

(defn pool
  [db-conf pool-conf]
  (let [{:keys
          [min-size max-size acquire-retry-attempts acquire-retry-delay
           max-idle-time-excess-connections max-idle-time]
          } pool-conf
        cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname db-conf))
               (.setJdbcUrl (str "jdbc:" (:subprotocol db-conf) ":" (:subname db-conf)))
               (.setUser (:user db-conf))
               (.setPassword (:password db-conf))
               (.setMinPoolSize min-size)
               (.setMaxPoolSize max-size)
               (.setAcquireRetryAttempts acquire-retry-attempts)
               (.setAcquireRetryDelay acquire-retry-delay)
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections max-idle-time-excess-connections)
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime max-idle-time))]
    {:datasource cpds}))

(defn init-db
  []
  (let [{:keys [db db-pool]} configuration]
    (pool db db-pool)))

(defn get-by-key
  "Get an entity by an arbitrary key name and a key value."
  [db entity-type-kw k-name-kw k-value]
  (->>
    (j/get-by-id
      db
      entity-type-kw
      k-value
      k-name-kw)
    (dao-common/dress entity-type-kw)))

(defn get-by-pk
  "Get an entity by a primary key."
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (get-by-key db entity-type-kw pk-name-kw pk-value)))

(defmulti
  upsert-dep
  (fn [db schema-map entity-value field-schema field-value]
    (::schema/field-type field-schema)))

(defmethod
  upsert-dep
  ::schema/one-to-one
  [db schema-map entity-value field-schema field-value]
  ;nothing to do relation is stored in the main entity
  )


(defmethod
  upsert-dep
  ::schema/one-to-many
  [db schema-map entity-value field-schema field-value]
  (let [pk-value (entity-helpers/find-primary-key-value
                   (entity-helpers/find-entity-schema schema-map entity-value)
                   entity-value)
        {field-name           ::schema/name
         related-entity-type  ::schema/related-entity
         related-entity-field ::schema/related-entity-field} field-schema
        related-entity-schema (get schema-map related-entity-type)
        stored-field-value (get-dep db schema-map entity-value field-schema)
        related-entity-pk-name (::schema/name (schema/find-primary-key related-entity-schema))
        get-pk-value (partial entity-helpers/find-primary-key-value related-entity-schema)
        field-pk-values-set (->>
                              field-value
                              (map get-pk-value)
                              (into #{}))
        stored-related-entity-pk-values-for-deletion (->>
                                                       stored-field-value
                                                       (map get-pk-value)
                                                       (filter (comp not field-pk-values-set)))]
    ;deletion of existing relations
    (doseq [pk stored-related-entity-pk-values-for-deletion]
      (j/update!
        db
        related-entity-type
        {related-entity-field nil}
        [(str (name related-entity-pk-name) " = ? ") pk]))
    (doseq [related-entity-value (mapv #(assoc % related-entity-field pk-value) field-value)]
      (upsert db schema-map related-entity-value))))

(defmethod
  upsert-dep
  ::schema/many-to-many
  [db schema-map entity-value field-schema field-value]
  (let [pk-value (entity-helpers/find-primary-key-value
                   (entity-helpers/find-entity-schema schema-map entity-value)
                   entity-value)
        {field-name           ::schema/name
         relation ::schema/relation
         left ::schema/left
         right ::schema/right
         related-entity-type  ::schema/related-entity
         related-entity-field ::schema/related-entity-field} field-schema
        related-entity-schema (get schema-map related-entity-type)
        get-pk-value (partial entity-helpers/find-primary-key-value related-entity-schema)
        field-pk-values-set (->>
                              field-value
                              (map get-pk-value)
                              (into #{}))]
    ;deletion of existing relations
    (sql-common/delete!
      db
      relation
      [(str (name left) " = ? ") pk-value])
    (doseq [related-entity-pk field-pk-values-set]
      (sql-common/insert! db relation {left pk-value right related-entity-pk}))))

(defn upsert-deps
  [db schema-map entity-value deps]
  (let [entity-schema (entity-helpers/find-entity-schema schema-map entity-value)
        fields-and-deps (->>
                     entity-schema
                     ::schema/fields
                     (map (fn [{field-name ::schema/name :as field-schema}]
                               [field-schema (get deps field-name)]))
                     (filter second))]
    (doseq [[field-schema field-value] fields-and-deps]
      (upsert-dep db schema-map entity-value field-schema field-value))))

(defn upsert
  "Insert or update an entity. If the entity contains a defined primary key then it will be updated.
  Otherwise it will be inserted. This implementation respects :deps"
  [db schema-map {entity-type-kw ::entity/type :as entity-value}]
  (let [pk-field (schema/find-primary-key (entity-type-kw schema-map))
        pk-kw (::schema/name pk-field)
        pk (get entity-value pk-kw)
        deps (:deps entity-value)
        stripped-entity (dao-common/strip entity-value)
        pk-value (if (nil? pk)
                   (sql-common/insert! db entity-type-kw stripped-entity pk-kw)
                   (do
                     (sql-common/update! db entity-type-kw stripped-entity [(str (name pk-kw) " = ? ") pk])
                     pk))]
    (upsert-deps db schema-map (assoc entity-value pk-kw pk-value) deps)
    pk-value))

(defn delete
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (->
      (sql-common/delete! db entity-type-kw [(str (name pk-name-kw) " = ? ") pk-value])
      first)))

(defn- db-name
  [n]
  (name n))

(defn- limit-stmt
  [limit]
  (if (int? limit)
    " limit ?"
    ""))

(defn- offset-stmt
  [offset]
  (if (int? offset)
    " offset ?"
    ""))

(defn- order
  [o]
  (case o
    :asc "asc"
    :desc "desc"))

(defn- order-by-stmt
  [order-by]
  (if (not-empty order-by)
    (str
      " order by "
      (string/join
        ", "
        (mapv
          (fn [[n o]]
            (str (db-name n) " " (order o)))
          order-by)))
    ""))

(defn- filter-stms
  [fltr]
  (if (not-empty fltr)
    (str
      " where "
      (string/join
        " and "
        (mapv
          (fn [[c _]]
            (str (db-name c) " = ? "))
          fltr)))
    ""))

(defn- filter-prmts
  [fltr]
  (if (not-empty fltr)
    (mapv second fltr)
    []))

(declare resolve-dependencies)

(defn get-entities
  "opts  :filter [[:column1 10] [:column2 \"string\"]]
         :order-by [[:column1 :asc] [:column2 :desc]]
         :limit 10
         :offset 0
         :resolved-dependencies true"
  [db schema-map entity-type-kw & opts]
  (let [{:keys [filter order-by limit offset resolved-dependencies]}
        (apply hash-map opts)
        deps-resolver
        (fn [entity]
          (if resolved-dependencies
            (resolve-dependencies db schema-map entity)
            entity))
        sql
        (str
          "select * from "
          (db-name entity-type-kw)
          (filter-stms filter)
          (order-by-stmt order-by)
          (limit-stmt limit)
          (offset-stmt offset))
        parameters
        (filterv
          some?
          (into (filter-prmts filter) [limit offset]))
        root-entities (->>
                        (j/query
                          db
                          (vec (cons sql parameters)))
                        (mapv
                          (fn [entity]
                            (->> entity
                                 (dao-common/dress entity-type-kw)
                                 deps-resolver))))]
    root-entities))

(defmulti get-dep (fn [_ _ _ field] (::schema/field-type field)))
(defmethod get-dep ::schema/one-to-one
  [db schema-map entity
   {field-name ::schema/name
    related-entity ::schema/related-entity
    related-entity-field ::schema/related-entity-field
    :as field}]
  (if (nil? related-entity-field)
    (get-by-pk db schema-map related-entity (get entity field-name))
    (get-by-key db related-entity related-entity-field (get entity field-name))))

(defmethod get-dep ::schema/one-to-many
  [db schema-map entity-value
   {related-entity ::schema/related-entity
    related-entity-field ::schema/related-entity-field
    :as field}]
  (get-entities
    db
    schema-map
    related-entity
    :filter [[related-entity-field
              (entity-helpers/find-primary-key-value
                (entity-helpers/find-entity-schema
                  schema-map
                  entity-value)
                entity-value)]]
    :limit MAX_NUMBER_OF_NESTED_ENTITIES
    :resolved-dependencies false))

(defmethod get-dep ::schema/many-to-many
  [db schema-map entity-value
   {related-entity ::schema/related-entity
    related-entity-field ::schema/related-entity-field
    relation ::schema/relation
    left ::schema/left
    right ::schema/right
    :as field}]
  (let [entity-schema (->> entity-value ::entity/type (get schema-map))
        pk-name-kw (::schema/name (schema/find-primary-key entity-schema))
        pk-value (entity-helpers/find-primary-key-value entity-schema entity-value)
        entities (j/query
                   db
                   [(format
                      "select r.* from %s as l inner join %s as rl on l.%s = rl.%s inner join %s as r on rl.%s = r.%s WHERE l.%s = ?"
                      (db-name (::entity/type entity-value))
                      (db-name relation)
                      (db-name pk-name-kw)
                      (db-name left)
                      (db-name related-entity)
                      (db-name right)
                      (db-name related-entity-field)
                      (db-name pk-name-kw))
                    pk-value])]
    (->>
      entities
      (mapv (partial dao-common/dress related-entity)))))

(defn resolve-dependencies
  [db schema-map entity]
  (let [entity-schema (->> entity ::entity/type (get schema-map))
        relation-fields (filter schema/relation? (::schema/fields entity-schema))
        deps (->>
               (mapv
                 (fn [field]
                   [(::schema/name field)
                    (get-dep db schema-map entity field)])
                 relation-fields)
               (into {}))]
    (assoc entity :deps deps)))

(defn setup-dao
  [states]
  (->
    states
    (mount/swap {#'ipogudin.collie.server.dao.common/init-db init-db})
    (mount/swap {#'ipogudin.collie.server.dao.common/get-by-pk get-by-pk})
    (mount/swap {#'ipogudin.collie.server.dao.common/upsert upsert})
    (mount/swap {#'ipogudin.collie.server.dao.common/delete delete})
    (mount/swap {#'ipogudin.collie.server.dao.common/get-entities get-entities})))