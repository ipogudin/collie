(ns ipogudin.collie.dao.sql
  (:require [clojure.java.jdbc :as j]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.dao.common :as dao-common]))

(defn get-by-id
  [db schema-map entity-kw id]
  (let [pk-name (-> schema-map entity-kw schema/find-primary-key ::schema/name)]
    (->>
      (j/get-by-id
        db
        entity-kw
        id
        pk-name)
      (dao-common/dress entity-kw))))

(defn upsert
  [db schema-map {entity-kw ::entity/type :as entity}]
  (let [pk-field (schema/find-primary-key (entity-kw schema-map))
        pk-kw (::schema/name pk-field)
        pk (get entity pk-kw)
        stripped-entity (dao-common/strip entity)]
    (if (nil? pk)
      (->
        (j/insert! db entity-kw stripped-entity)
        first
        vals
        first)
      (do
        (j/update! db entity-kw stripped-entity [(str (name pk-kw) " = ? ") pk])
        pk))))

(defn delete
  [db schema-map {entity-kw ::entity/type :as entity}]
  (let [pk-field (schema/find-primary-key (entity-kw schema-map))
        pk-kw (::schema/name pk-field)
        pk (get entity pk-kw)]
    (j/delete! db entity-kw [(str (name pk-kw) " = ? ") pk])))
