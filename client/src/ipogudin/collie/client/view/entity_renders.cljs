(ns ipogudin.collie.client.view.entity-renders
  (:require [clojure.string :refer [join]]
            [ipogudin.collie.client.common :refer [format]]
            [ipogudin.collie.schema :as schema]))

(defn visible?
  "Returns true if a field schema allows showing this field."
  [{{hidden ::schema/hidden} ::schema/ui :as field-schema}]
  (not hidden))

(defn render-name
  "Renders a field name with respect to ui directives."
  [{name ::schema/name {title ::schema/title} ::schema/ui}]
  (if title
    title
    name))

(defn render-text
  "Renders a text field with respect to ui directives."
  [{{text-length ::schema/preview-text-length} ::schema/ui}
   value]
  (if (and text-length value)
    (let [length (count value)
          end (if
                (> text-length length)
                length
                text-length)]
      (format
        "%s%s"
        (subs value 0 end)
        (if (< end length) "..." "")))
    value))

(defmulti
  render-field
  (fn [schema field-schema entity-value]
    (::schema/field-type field-schema)))

(defmethod
  render-field
  ::schema/serial
  [schema field-schema entity-value]
  (->> field-schema ::schema/name (get entity-value)))

(defmethod
  render-field
  ::schema/int
  [schema field-schema entity-value]
  (->> field-schema ::schema/name (get entity-value)))

(defmethod
  render-field
  ::schema/decimal
  [schema
   {scale ::schema/scale :as field-schema}
   entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (format
      (str
        "%"
        (if scale
          (str "." scale))
        "f"))))

(defmethod
  render-field
  ::schema/string
  [schema field-schema entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (render-text field-schema)))

(defmethod
  render-field
  ::schema/one-to-one
  [schema
   {field-name ::schema/name
    related-entity ::schema/related-entity
    :as field-schema}
   entity-value]
  (let [{{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        related-entity-value (-> entity-value :deps (get field-name))]
    (or
      (if
        show-fn
        (show-fn related-entity-value))
      (get entity-value field-name))))

(defn render-many
  [schema
   {field-name ::schema/name
    related-entity ::schema/related-entity
    :as field-schema}
   entity-value]
  (let [{{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        related-entity-values (-> entity-value :deps (get field-name))
        generated-text (if
                         show-fn
                         (->> related-entity-values
                              (mapv show-fn)
                              (join ", ")))]
    (render-text field-schema generated-text)))

(defmethod
  render-field
  ::schema/one-to-many
  [& args]
  (apply render-many args))

(defmethod
  render-field
  ::schema/many-to-many
  [& args]
  (apply render-many args))

(defn render-header
  "Renders a header of a table with field names/titles for entity schema."
  [schema type]
  (let [entity-schema (get schema type)]
    (into
      [:tr]
      (map
        (comp (fn [n] [:th n]) render-name)
        (filter
          visible?
          (::schema/fields entity-schema))))))

(defn render-row
  "Renders a row of a table with field values for a particular entity."
  [schema type entity]
  (let [entity-schema (get schema type)]
    (into
      [:tr {:key (schema/find-primary-key-value entity-schema entity)}]
      (map
        (fn [field-schema] [:td (render-field schema field-schema entity)])
        (filter
          visible?
          (::schema/fields entity-schema))))))