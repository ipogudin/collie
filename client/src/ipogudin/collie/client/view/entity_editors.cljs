(ns ipogudin.collie.client.view.entity-editors
  (:require [clojure.string :refer [join]]
            [reagent.core :as r]
            [cljs-time.core :refer [today now]]
            [re-frame.core :as re-frame]
            [ipogudin.collie.common :refer [deep-merge format]]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.client.view.entity-renders :refer [render-name render-text render-decimal visible?]]))

(defn value-handler
  "Returns a function which invokes f with a value of an element with id as an argument."
  [id f]
  (fn []
    (apply f [(-> js/document (.getElementById id) .-value)])))

(defn default-value-parameter
  [value]
  (if value
    {:default-value value}
    {}))

(defn checked?
  "Returns whether checkbox is checked or not."
  [id]
  (-> js/document (.getElementById id) .-checked))

(defmulti
  render-field-editor
  (fn [storage schema field-schema editing]
    (::schema/field-type field-schema)))

(defmethod
  render-field-editor
  ::schema/serial
  [storage schema field-schema editing]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        value (get-in @editing [:entity field-name])]
    (fn [] [:label {:for id} rendered-name]
     [:input.form-control
      {:id id
       :type "text"
       :readOnly true
       :defaultValue value}])))

(defn null-switcher [field-schema value default-value]
  (let [id (str "null-switcher" (protocol/gen-id))]
    [:span
      " "
      [:input
        {:id id
         :type "checkbox"
         :checked (some? value)
         :on-change (fn [_]
                      (re-frame/dispatch
                        [:change-entity-field
                         field-schema
                         (if (checked? id)
                           default-value)]))}]]))

(defn create-field-editor
  [storage schema field-schema editing {:keys [input-type string-to-value value-to-string generate-default-value]}]
  (let [rendered-name (render-name field-schema)
        field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-value (r/atom nil)
        editing-value (r/atom nil)
        field-name (::schema/name field-schema)
        nullable (::schema/nullable field-schema)]
    (fn []
      (let [value (get-in @editing [:entity field-name])
            disabled (nil? value)]
        (if @editing-value
          (reset! rendered-value @editing-value)
          (reset! rendered-value (value-to-string field-schema value)))
        [:form-group [:label {:for id} rendered-name]
         (if nullable
           [null-switcher field-schema value (generate-default-value)]
           [:span ""])
         [:input.form-control
          {:id id
           :type "text"
           :disabled disabled
           :value @rendered-value
           :on-change (fn [event] (reset! editing-value (-> event .-target .-value)))
           :on-blur (value-handler
                      id
                      (fn [v]
                        (reset! editing-value nil)
                        (re-frame/dispatch
                          [:change-entity-field
                           field-schema
                           (string-to-value field-schema v)])))}]]))))

(defmethod
  render-field-editor
  ::schema/date
  [storage schema field-schema editing]
  (create-field-editor storage schema field-schema editing
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-date
                        :value-to-string entity-helpers/date-to-string
                        :generate-default-value today}))

(defmethod
  render-field-editor
  ::schema/timestamp
  [storage schema field-schema editing]
  (create-field-editor storage schema field-schema editing
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-timestamp
                        :value-to-string entity-helpers/timestamp-to-string
                        :generate-default-value now}))

(defmethod
  render-field-editor
  ::schema/boolean
  [storage schema field-schema editing]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        field-name (::schema/name field-schema)
        nullable (::schema/nullable field-schema)]
    (fn []
      (let [value (get-in @editing [:entity field-name])
            disabled (nil? value)]
        [:form-group [:label {:for id} rendered-name]
         (if nullable
           [null-switcher field-schema value true]
           [:span ""])
         [:input.form-control
          {:id id
           :type "checkbox"
           :disabled disabled
           :checked (true? value)
           :on-change (value-handler
                        id
                        (fn [_]
                          (re-frame/dispatch
                            [:change-entity-field
                             field-schema
                             (checked? id)])))}]]))))

(defmethod
  render-field-editor
  ::schema/int
  [storage schema field-schema editing]
  (create-field-editor storage schema field-schema editing
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-int
                        :value-to-string entity-helpers/int-to-string
                        :generate-default-value #(identity 0)}))

(defmethod
  render-field-editor
  ::schema/decimal
  [storage schema field-schema editing]
  (create-field-editor storage schema field-schema editing
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-decimal
                        :value-to-string entity-helpers/decimal-to-string
                        :generate-default-value #(identity (entity-helpers/string-to-decimal field-schema "0"))}))

(defmethod
  render-field-editor
  ::schema/string
  [storage schema field-schema editing]
  (create-field-editor storage schema field-schema editing
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-string
                        :value-to-string entity-helpers/string-to-string
                        :generate-default-value #(identity "")}))

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
   editing]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        {{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        renderer (fn [e]
                   (if show-fn
                     (show-fn e)
                     (str e)))
        nullable (::schema/nullable field-schema)]
    (fn []
      (let [related-entity-value (get-in @editing [:entity :deps field-name])
            value-options (get-in @editing [:dep-options field-name])
            options-with-ids (mapv
                               (fn [entity]
                                 [(entity-helpers/get-entity-id schema entity) entity])
                               value-options)
            default-value-id (entity-helpers/get-entity-id schema related-entity-value)
            default-related-entity-value (->> options-with-ids first second)
            disabled (nil? related-entity-value)]
        [:form-group [:label {:for id} rendered-name]
         (if nullable
            [null-switcher field-schema related-entity-value default-related-entity-value]
            [:span ""])
         (into
           [:select.form-control
            (deep-merge
              {:id id
               :disabled disabled
               :on-change (value-handler
                            id
                            (comp
                              (fn [value] (re-frame/dispatch [:change-entity-field field-schema value]))
                              (partial get-editable-entity storage id)))}
              (default-value-parameter default-value-id))]
           (if-not disabled
             (mapv
               (partial render-option id storage renderer)
               options-with-ids)
             []))]))))

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
   editing]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        {{show-fn ::schema/show-fn} ::schema/ui
         :as related-entity-schema} (get schema related-entity)
        renderer (fn [e]
                   (if show-fn
                     (show-fn e)
                     (str e)))]
    (fn []
      (let [related-entity-value (get-in @editing [:entity :deps field-name])
            value-options (get-in @editing [:dep-options field-name])
            options-with-ids (mapv
                               (fn [entity]
                                 [(entity-helpers/get-entity-id schema entity) entity])
                               value-options)
            default-value (map (partial entity-helpers/get-entity-id schema) related-entity-value)]
        [:form-group [:label {:for id} rendered-name]
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
             options-with-ids))]))))

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
  [schema editing]
  (let [storage (atom {})
        entity-schema (get schema (get-in @editing [:entity ::entity/type]))]
    (fn []
      (into
        [:form]
        (map
          (fn [field-schema] [render-field-editor storage schema field-schema editing])
          (filter
            visible?
            (::schema/fields entity-schema)))))))