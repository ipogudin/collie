(ns ipogudin.collie.client.view.entity-editors
  (:require [clojure.string :refer [join]]
            [re-frame.core :as re-frame]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.client.common :refer [format]]
            [ipogudin.collie.client.view.entity-renders :refer [render-name render-text render-decimal]]))

(defn visible?
  "Returns true if a field schema allows showing this field."
  [{{hidden ::schema/hidden} ::schema/ui :as field-schema}]
  (not hidden))

(defn value-handler
  "Returns a function which invokes f with a value of an element with id as an argument."
  [id f]
  (fn []
    (apply f [(-> js/document (.getElementById id) .-value)])))

(defn default-value-parameter
  [value]
  (if value
    {:defaultValue value}
    {}))

(defmulti
  render-field-editor
  (fn [storage schema field-schema entity-value dep-options]
    (::schema/field-type field-schema)))

(defmethod
  render-field-editor
  ::schema/serial
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value)
                (render-text field-schema))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :readOnly true}
        (default-value-parameter value))]]))

(defmethod
  render-field-editor
  ::schema/date
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value)
                (entity-helpers/date-to-string field-schema))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :defaultValue value
         :onBlur (value-handler
                      id
                      (fn [v]
                        (re-frame/dispatch
                          [:change-entity-field
                           field-schema
                           (entity-helpers/string-to-date field-schema v)])))}
        (default-value-parameter value))]]))

(defmethod
  render-field-editor
  ::schema/timestamp
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value)
                (entity-helpers/timestamp-to-string field-schema))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :defaultValue value
         :onBlur (value-handler
                      id
                      (fn [v]
                        (re-frame/dispatch
                          [:change-entity-field
                           field-schema
                           (entity-helpers/string-to-timestamp field-schema v)])))}
        (default-value-parameter value))]]))

(defn checked?
  "Returns whether checkbox is checked or not."
  [id]
  (-> js/document (.getElementById id) .-checked))

(defmethod
  render-field-editor
  ::schema/boolean
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      {:id id
       :type "checkbox"
       :checked value
       :on-change (value-handler
                    id
                    (fn [_]
                      (re-frame/dispatch
                        [:change-entity-field
                         field-schema
                         (checked? id)])))}]]))

(defmethod
  render-field-editor
  ::schema/int
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :defaultValue value
         :onBlur (value-handler
                      id
                      (fn [v]
                        (re-frame/dispatch [:change-entity-field field-schema v])))}
        (default-value-parameter value))]]))

(defmethod
  render-field-editor
  ::schema/decimal
  [storage
   schema
   {scale ::schema/scale :as field-schema}
   entity-value
   dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value)
                (render-decimal field-schema))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :onBlur (value-handler
                      id
                      (fn [v]
                        (re-frame/dispatch [:change-entity-field field-schema v])))}
        (default-value-parameter value))]]))

(defmethod
  render-field-editor
  ::schema/string
  [storage schema field-schema entity-value dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (->>
                field-name
                (get entity-value)
                (render-text field-schema))]
    [[:label {:for id} rendered-name]
     [:input.form-control
      (deep-merge
        {:id id
         :type "text"
         :onBlur (value-handler
                      id
                      (fn [v]
                        (re-frame/dispatch [:change-entity-field field-schema v])))}
        (default-value-parameter value))]]))

(defn add-editable-entity
  "Adds an entity being edited into the storage."
  [storage editor-id entity-id entity-value]
  (swap! storage assoc-in [:editors editor-id entity-id] entity-value))

(defn get-editable-entity
  "Gets an entity from the storage."
  [storage editor-id entity-id]
  (get-in
    @storage
    [:editors editor-id entity-id]))

(defn render-option
  [editor-id storage renderer [entity-id entity-value]]
  (add-editable-entity storage editor-id entity-id entity-value)
  [:option {:value entity-id} (renderer entity-value)])

(defmethod
  render-field-editor
  ::schema/one-to-one
  [storage
   schema
   {field-name ::schema/name
    related-entity ::schema/related-entity
    :as field-schema}
   entity-value
   dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        {{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        related-entity-value (-> entity-value :deps (get field-name))
        renderer (fn [e]
                   (if show-fn
                     (show-fn e)
                     (str e)))
        options-with-ids (mapv
                           (fn [entity]
                             [(entity-helpers/get-entity-id schema entity) entity])
                           (get dep-options field-name))
        default-value (entity-helpers/get-entity-id schema related-entity-value)]
    [[:label {:for id} rendered-name]
     (into
       [:select.form-control
        (deep-merge
          {:id id
           :on-change (value-handler
                        id
                        (comp
                          (fn [value] (re-frame/dispatch [:change-entity-field field-schema value]))
                          (partial get-editable-entity storage id)))}
          (default-value-parameter default-value))]
       (mapv
         (partial render-option id storage renderer)
         options-with-ids))]))

(defn get-selected-options
  [id]
  (let [options (-> js/document (.getElementById id) .-options)
        l (.-length options)]
    (->>
      (for [i (range l)] (.item options i))
      (filter #(.-selected %))
      (map #(.-value %)))))

(defn render-many
  [storage
   schema
   {field-name ::schema/name
    related-entity ::schema/related-entity
    {selector-size ::schema/selector-size} ::schema/ui
    :as field-schema}
   entity-value
   dep-options]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        {{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        related-entity-value (-> entity-value :deps (get field-name))
        renderer (fn [e]
                   (if show-fn
                     (show-fn e)
                     (str e)))
        options-with-ids (mapv
                           (fn [entity]
                             [(entity-helpers/get-entity-id schema entity) entity])
                           (get dep-options field-name))
        default-value (map (partial entity-helpers/get-entity-id schema) related-entity-value)]
    [[:label {:for id} rendered-name]
     (into
       [:select.form-control
        (deep-merge
          {:id id
           :multiple true
           :size (or selector-size 5)
           :on-change (value-handler
                        id
                        (fn []
                          (let [value (mapv
                                        (partial get-editable-entity storage id)
                                        (get-selected-options id))]
                            (re-frame/dispatch [:change-entity-field field-schema value]))))}
          (default-value-parameter default-value))]
       (mapv
         (partial render-option id storage renderer)
         options-with-ids))]))

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


(defn render-entity-editor
  "Renders am entity editor."
  [schema entity dep-options]
  (let [entity-schema (get schema (::entity/type entity))
        storage (atom {})]
    (into
      [:form]
      (map
        (fn [field-schema] (into [:form-group] (render-field-editor storage schema field-schema entity dep-options)))
        (filter
          visible?
          (::schema/fields entity-schema))))))