(ns ipogudin.collie.client.view.entity-renders
  (:require [clojure.string :refer [join]]
            [re-frame.core :as re-frame]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.client.common :refer [format]]
            [ipogudin.collie.client.view.common :refer [raw-html]]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.entity :as entity]))

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

(defn render-decimal
  "Renders a decimal value."
  [{precision ::precision scale ::scale} value]
  (if (number? value)
    (format
      (str
        "%"
        (if scale
          (str "." scale))
        "f")
      value)))

(defmulti
  render-field-editor
  (fn [schema field-schema entity-value]
    (::schema/field-type field-schema)))

(defmethod
  render-field-editor
  ::schema/serial
  [schema field-schema entity-value]
  (->> field-schema ::schema/name (get entity-value)))

(defmethod
  render-field-editor
  ::schema/boolean
  [schema field-schema entity-value]
  (let [v (->> field-schema ::schema/name (get entity-value))]
    [:input.form-control
       {:defaultChecked v
        :disabled true
        :type "checkbox"}]))

(defmethod
  render-field-editor
  ::schema/int
  [schema field-schema entity-value]
  (->> field-schema ::schema/name (get entity-value)))

(defmethod
  render-field-editor
  ::schema/decimal
  [schema
   {scale ::schema/scale :as field-schema}
   entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (render-decimal field-schema)))

(defmethod
  render-field-editor
  ::schema/string
  [schema field-schema entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (render-text field-schema)))

(defmethod
  render-field-editor
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
  render-field-editor
  ::schema/one-to-many
  [& args]
  (apply render-many args))

(defmethod
  render-field-editor
  ::schema/many-to-many
  [& args]
  (apply render-many args))

(defn render-header
  "Renders a header of a table with field names/titles for entity schema."
  [schema type]
  (let [entity-schema (get schema type)
        headers (map
                  (comp (fn [n] [:th n]) render-name)
                  (filter
                    visible?
                    (::schema/fields entity-schema)))]
    (into
      [:tr]
      (into headers [[:th] [:th]]))))

(defn render-row
  "Renders a row of a table with field values for a particular entity."
  [schema type entity]
  (let [entity-schema (get schema type)
        cells (map
                (fn [field-schema] [:td (render-field-editor schema field-schema entity)])
                (filter
                  visible?
                  (::schema/fields entity-schema)))]
    (into
      [:tr {:key (entity-helpers/find-primary-key-value entity-schema entity)}]
      (into
        cells
        [[:td
          [:button.btn.btn-light
           (deep-merge
             {:id "edit-button"
              :on-click #(re-frame/dispatch [:delete-entity entity])}
             (raw-html "&times;"))]]
         [:td
          [:button.btn.btn-light
           (deep-merge
             {:id "edit-button"
              :on-click #(re-frame/dispatch [:edit-entity entity])}
             (raw-html "&equiv;"))]]]))))

(defn render-control
  [schema type]
  (let [entity-schema (get schema type)
        number-of-cells (->>
                          (::schema/fields entity-schema)
                          (filter visible?)
                          count)
        add-button [:td
                    [:button.btn.btn-primary
                      {:type     "button"
                       :on-click (fn [] (re-frame/dispatch [:edit-entity (entity/create-empty-entity type)]))}
                      "+"]]
        empty-cell [:td]]
    (into
      []
      (concat
        [:tr
         {:key (protocol/gen-id)}]
        [add-button
         empty-cell]
        (mapv (fn [_] empty-cell) (range number-of-cells))))))