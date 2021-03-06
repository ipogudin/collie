(ns ipogudin.collie.server.dao.h2-sql-common
  (:require [clojure.java.jdbc :as j]
            [mount.core :as mount]
            [ipogudin.collie.server.dao.sql-common :as sql-common]))

(defn insert!
  ([db entity-type-kw entity] (j/insert! db entity-type-kw entity))
  ([db entity-type-kw entity primary-key-kw]
    (->
      (j/insert! db entity-type-kw entity)
      first
      vals
      first)))

(defn update!
  [db entity-type-kw entity where-clause ]
  (j/update! db entity-type-kw entity where-clause))

(defn delete!
  [db entity-type-kw where-clause]
  (j/delete! db entity-type-kw where-clause))

(defn setup-h2
  [states]
  (->
    states
    (mount/swap {#'ipogudin.collie.server.dao.sql-common/insert! insert!})
    (mount/swap {#'ipogudin.collie.server.dao.sql-common/update! update!})
    (mount/swap {#'ipogudin.collie.server.dao.sql-common/delete! delete!})))
