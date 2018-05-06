(ns ipogudin.collie.entity-helpers
  "This namespace contains helper functions to work with entities respecting schema api."
  (:require [ipogudin.collie.entity :refer [get-entity-type]]
            [ipogudin.collie.schema :as schema]
            #?(:clj [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj [clj-time.core :as c]
               :cljs [cljs-time.core :as c])))

(defn find-entity-schema
  [schema-map entity-value]
  (get schema-map (get-entity-type entity-value)))

(defn find-primary-key-value
  [entity-schema entity]
  (let [pk-name (::schema/name (schema/find-primary-key entity-schema))]
    (get entity pk-name)))

(defn get-entity-id
  "Returns entity id based on a schema name and its primary key value. It doesn't return entity primary key."
  [schema-map entity-value]
  (if (map? entity-value)
    (let [s (find-entity-schema schema-map entity-value)]
      (str
        (-> s ::schema/name name)
        "."
        (find-primary-key-value
          s
          entity-value)))))

(defmulti
  set-field-value
  (fn [schema-map field-schema entity field-value]
    (let [field-type (::schema/field-type field-schema)]
      (if (schema/relation? field-schema)
        field-type
        ::by-value))))

(defmethod
  set-field-value
  ::by-value
  [schema-map field-schema entity field-value]
  (assoc entity (::schema/name field-schema) field-value))

(defmethod
  set-field-value
  ::schema/one-to-one
  [schema-map field-schema entity field-value]
  (let [{field-name ::schema/name
         related-entity-field ::schema/related-entity-field} field-schema]
    (-> entity
        (assoc field-name (get field-value related-entity-field))
        (assoc-in [:deps field-name] field-value))))

(defmethod
  set-field-value
  ::schema/one-to-many
  [schema-map field-schema entity field-value]
  (let [entity-type (get-entity-type entity)
        entity-schema (get schema-map entity-type)
        pk-value (find-primary-key-value entity-schema entity)
        {field-name ::schema/name
         related-entity-field ::schema/related-entity-field} field-schema
        processed-field-value (mapv #(assoc % related-entity-field pk-value) field-value)]
    (assoc-in entity [:deps field-name] processed-field-value)))

(defmethod
  set-field-value
  ::schema/many-to-many
  [schema-map field-schema entity field-value]
  (let [{field-name ::schema/name
         related-entity-field ::schema/related-entity-field} field-schema]
    (assoc-in entity [:deps field-name] field-value)))

(defn
  sort-entities-by-pk
  [schema-map entities]
  (let [entity-sample (->> entities first)
        entity-schema (find-entity-schema schema-map entity-sample)
        pk-field (-> entity-schema schema/find-primary-key ::schema/name)]
    (sort-by #(get % pk-field) entities)))

; fields encoding

(defn timestamp-to-string
  "Transform timestamp to string according to format."
  [{ts-format ::schema/ts-format tz-disabled ::schema/tz-disabled :as field-schema} field-value]
  (->
    (f/formatter
      ts-format
      (if tz-disabled
        c/utc
        (c/default-time-zone)))
    (f/unparse field-value)))

(defn string-to-timestamp
  "Transform timestamp to string according to format."
  [{ts-format ::schema/ts-format tz-disabled ::schema/tz-disabled :as field-schema} string-value]
  (->
    (f/formatter
      ts-format
      (if tz-disabled
        c/utc
        (c/default-time-zone)))
    (f/parse string-value)))

(defn date-to-string
  "Transform timestamp to string according to format."
  [{ts-format ::schema/ts-format :as field-schema} field-value]
  (->
    (f/formatter ts-format)
    (f/unparse-local-date field-value)))

(defn string-to-date
  "Transform timestamp to string according to format."
  [{ts-format ::schema/ts-format :as field-schema} string-value]
  (->
    (f/formatter ts-format)
    (f/parse-local-date string-value)))