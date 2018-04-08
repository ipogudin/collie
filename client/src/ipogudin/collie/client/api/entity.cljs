(ns ipogudin.collie.client.api.entity
  (:require [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity :as e]))

(defn all-options-for-relation
  [field-schema]
  (let [field-name (::schema/name field-schema)
        dep-type (::schema/related-entity field-schema)]
    {:name field-name
     :command (protocol/get-entities-command dep-type)}))

(defn options-for-dependencies
  "Generates a sequences of commands to receive dependencies for the entity."
  [entity-schema]
  (->> (::schema/fields entity-schema) (filterv schema/relation?) (mapv all-options-for-relation)))
