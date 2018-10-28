(ns ipogudin.collie.entity-helpers
  "This namespace contains helper functions to work with entities respecting schema api."
  (:require [ipogudin.collie.entity :as entity]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.common :as common]
            #?(:clj [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj [clj-time.core :as c]
               :cljs [cljs-time.core :as c])))

(defn find-entity-schema
  [schema-map entity-value]
  (get schema-map (entity/get-entity-type entity-value)))

(defn find-primary-key-value
  [entity-schema entity]
  (let [pk-name (::schema/name (schema/find-primary-key entity-schema))]
    (get entity pk-name)))

(defn get-entity-id
  "Returns entity id based on a schema name and its primary key value. It doesn't return entity primary key."
  [schema-map entity-value]
  (if (map? entity-value)
    (let [s (find-entity-schema schema-map entity-value)]
      (if (some? s)
        (str
          (-> s ::schema/name name)
          "."
          (find-primary-key-value
            s
            entity-value))))))

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
  (let [entity-type (entity/get-entity-type entity)
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
  (if field-value
    (->
      (f/formatter
        ts-format
        (if tz-disabled
          c/utc
          (c/default-time-zone)))
      (f/unparse field-value))
    ""))

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
  (if field-value
    (->
      (f/formatter ts-format)
      (f/unparse-local-date field-value))
    ""))

(defn string-to-date
  "Transform timestamp to string according to format."
  [{ts-format ::schema/ts-format :as field-schema} string-value]
  (->
    (f/formatter ts-format)
    (f/parse-local-date string-value)))

(defn string-to-int
  [field-schema string-value]
  #?(:clj (Integer/parseInt string-value)
     :cljs (js/parseInt string-value)))

(defn int-to-string
  [field-schema field-value]
  (str field-value))

(declare decimal-to-string)

(defn string-to-decimal
  [field-schema string-value]
  #?(:clj (->> string-value bigdec (decimal-to-string field-schema) bigdec)
     :cljs (->> string-value js/parseFloat (decimal-to-string field-schema) js/parseFloat)))

(defn decimal-to-string
  [{precision ::schema/precision scale ::schema/scale :as field-schema} field-value]
  (if (number? field-value)
    (common/format
      (str
        "%"
        (if scale
          (str "." scale))
        "f")
      field-value)))

(defn string-to-string
  [{max-length ::schema/max-length :as field-schema} value]
  (if (string? value)
    (if max-length
      (subs value 0 max-length)
      value)))

(defn default-value
  [{field-name ::schema/name field-type ::schema/field-type :as field-schema}]
  (case (::schema/field-type field-schema)
    ::schema/serial {field-name nil}
    ::schema/boolean {field-name true}
    ::schema/int {field-name 0}
    ::schema/decimal {field-name 0M}
    ::schema/string {field-name ""}
    ::schema/date {field-name (c/today)}
    ::schema/timestamp {field-name (c/now)}
    ::schema/one-to-one {:deps {field-name {}}}
    ::schema/one-to-many {:deps {field-name []}}
    ::schema/many-to-many {:deps {field-name []}}))

(defn create-empty-entity
  [entity-type entity-schema]
  (reduce
    common/deep-merge
    {::entity/type entity-type}
    (->>
      (mapv
        (fn [{field-name ::schema/name default ::schema/default :as field-schema}]
          (if default
            (if (= ::schema/empty default)
              (default-value field-schema)
              {field-name default})))
        (::schema/fields entity-schema))
      (filterv some?))))
