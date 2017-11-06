(ns ipogudin.collie.dao.common
  (:require [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]))

(defn strip [entity]
  "Strips an entity as a preparation process for dao layer. In other words, it removes technical fields."
  (dissoc entity ::entity/type))

(defn dress [entity-kw entity]
  "Dresses an entity with special technical fields."
  (if entity
    (assoc entity ::entity/type entity-kw)))