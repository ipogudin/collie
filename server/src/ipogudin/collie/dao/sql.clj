(ns ipogudin.collie.dao.sql
  (:require [clojure.java.jdbc :as j]
            [mount.core :as mount]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.dao.common :as dao-common]
            [ipogudin.collie.configuration :refer [configuration]])
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
  (let [{:keys [db-conf pool-conf]} configuration]
    (pool db-conf pool-conf)))

(defn get-by-id
  [db schema-map entity-type-kw id]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (->>
      (j/get-by-id
        db
        entity-type-kw
        id
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
  [db schema-map entity-type-kw id]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (j/delete! db entity-type-kw [(str (name pk-name-kw) " = ? ") id])))

(defn setup-dao
  [states]
  (->
    states
    (mount/swap {#'ipogudin.collie.dao.common/init-db init-db})
    (mount/swap {#'ipogudin.collie.dao.common/get-by-id get-by-id})
    (mount/swap {#'ipogudin.collie.dao.common/upsert upsert})
    (mount/swap {#'ipogudin.collie.dao.common/delete delete})))