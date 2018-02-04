(ns ipogudin.collie.server.dao.sql
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as j]
            [mount.core :as mount]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.server.dao.common :as dao-common]
            [ipogudin.collie.server.configuration :refer [configuration]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

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

(defn get-by-pk
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (->>
      (j/get-by-id
        db
        entity-type-kw
        pk-value
        pk-name-kw)
      (dao-common/dress entity-type-kw))))

(defn upsert
  [db schema-map {entity-type-kw ::entity/type :as entity}]
  (let [pk-field (schema/find-primary-key (entity-type-kw schema-map))
        pk-kw (::schema/name pk-field)
        pk (get entity pk-kw)
        stripped-entity (dao-common/strip entity)]
    (if (nil? pk)
      (->
        (j/insert! db entity-type-kw stripped-entity)
        first
        vals
        first)
      (do
        (j/update! db entity-type-kw stripped-entity [(str (name pk-kw) " = ? ") pk])
        pk))))

(defn delete
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (->
      (j/delete! db entity-type-kw [(str (name pk-name-kw) " = ? ") pk-value])
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

(defn get-entities
  "opts  :filter [[:column1 10] [:column2 \"string\"]]
         :order-by [[:column1 :asc] [:column2 :desc]]
         :limit 10
         :offset 0"
  [db schema-map entity-type-kw & opts]
  (let [{:keys [fltr order-by limit offset]}
         (apply hash-map opts)
        sql
        (str
          "select * from "
          (db-name entity-type-kw)
          (filter-stms fltr)
          (order-by-stmt order-by)
          (limit-stmt limit)
          (offset-stmt offset))
        parameters
        (filterv
          some?
          (into (filter-prmts fltr) [limit offset]))]
    (->>
      (j/query
        db
        (vec (cons sql parameters)))
      vec)))

(defn setup-dao
  [states]
  (->
    states
    (mount/swap {#'ipogudin.collie.server.dao.common/init-db init-db})
    (mount/swap {#'ipogudin.collie.server.dao.common/get-by-pk get-by-pk})
    (mount/swap {#'ipogudin.collie.server.dao.common/upsert upsert})
    (mount/swap {#'ipogudin.collie.server.dao.common/delete delete})
    (mount/swap {#'ipogudin.collie.server.dao.common/get-entities get-entities})))