(ns ipogudin.collie.client.view.entity-renders
  (:require [clojure.string :refer [join]]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [cljs-time.format :as f]
            [cljs-time.core :refer [today now]]
            [ipogudin.collie.common :refer [deep-merge format]]
            [ipogudin.collie.client.view.common :refer [raw-html]]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity-helpers :as entity-helpers]))

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
  [{precision ::schema/precision scale ::schema/scale} value]
  (if (number? value)
    (format
      (str
        "%"
        (if scale
          (str "." scale))
        "f")
      value)))

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
  ::schema/date
  [schema field-schema entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (entity-helpers/date-to-string field-schema)))

(defmethod
  render-field
  ::schema/timestamp
  [schema field-schema entity-value]
  (->>
    field-schema
    ::schema/name
    (get entity-value)
    (entity-helpers/timestamp-to-string field-schema)))

(defmethod
  render-field
  ::schema/boolean
  [schema field-schema entity-value]
  (let [v (->> field-schema ::schema/name (get entity-value))]
    [:input.form-control
       {:defaultChecked v
        :disabled true
        :type "checkbox"}]))

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
    (render-decimal field-schema)))

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

(def empty-cell [:td])

;ordering control elements
(defn ordering-button
  [ordering {name ::schema/name :as field}]
  (if (schema/multi-relation? field)
    empty-cell
    [:td
     [:button.btn.btn-primary
      {:on-click (fn [_] (re-frame/dispatch [:switch-ordering name]))}
      (case (get ordering name)
        :asc "↓"
        :desc "↑"
        "x")]]))
;end of ordering control elements

;filtering control elements
;TODO refactor the following coed and code from the field editor

(defn value-handler
  "Returns a function which invokes f with a value of an element with id as an argument."
  [id f]
  (fn []
    (apply f [(-> js/document (.getElementById id) .-value)])))

(defn checked?
  "Returns whether checkbox is checked or not."
  [id]
  (-> js/document (.getElementById id) .-checked))

(defn create-field-filter
  [schema field-schema filtering {:keys [input-type string-to-value value-to-string generate-default-value]}]
  (let [rendered-name (render-name field-schema)
        field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-value (r/atom nil)
        editing-value (r/atom nil)
        field-name (::schema/name field-schema)]
    (fn []
      (let [value (get @filtering field-name)]
        (if @editing-value
          (reset! rendered-value @editing-value)
          (reset! rendered-value (value-to-string field-schema value)))
       [:input.form-control.col-10
        {:id id
         :type "text"
         :value @rendered-value
         :on-change (fn [event] (reset! editing-value (-> event .-target .-value)))
         :on-blur (value-handler
                    id
                    (fn [v]
                      (reset! editing-value nil)
                      (re-frame/dispatch
                        [:set-filtering
                         {field-name (string-to-value field-schema v)}])))}]))))

(defmulti
  render-field-filter
  (fn [schema field-schema filtering]
    (::schema/field-type field-schema)))

(defmethod
  render-field-filter
  ::schema/serial
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-int
                        :value-to-string entity-helpers/int-to-string
                        :generate-default-value #(identity 0)}))

(defmethod
  render-field-filter
  ::schema/date
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-date
                        :value-to-string entity-helpers/date-to-string
                        :generate-default-value today}))

(defmethod
  render-field-filter
  ::schema/timestamp
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-timestamp
                        :value-to-string entity-helpers/timestamp-to-string
                        :generate-default-value now}))

(defmethod
  render-field-filter
  ::schema/boolean
  [schema field-schema filtering]
  (let [field-name (::schema/name field-schema)
        id (str (protocol/gen-id) field-name)
        rendered-name (render-name field-schema)
        field-name (::schema/name field-schema)]
    (fn []
      (let [value (get @filtering field-name)]
       [:input.form-control.col-auto
        {:id id
         :type "checkbox"
         :checked (true? value)
         :on-change (value-handler
                      id
                      (fn [_]
                        (re-frame/dispatch
                          [:set-filtering
                           {field-name (checked? id)}])))}]))))

(defmethod
  render-field-filter
  ::schema/int
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-int
                        :value-to-string entity-helpers/int-to-string
                        :generate-default-value #(identity 0)}))

(defmethod
  render-field-filter
  ::schema/decimal
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-decimal
                        :value-to-string entity-helpers/decimal-to-string
                        :generate-default-value #(identity (entity-helpers/string-to-decimal field-schema "0"))}))

(defmethod
  render-field-filter
  ::schema/string
  [schema field-schema filtering]
  (create-field-filter schema field-schema filtering
                       {:input-type "text"
                        :string-to-value entity-helpers/string-to-string
                        :value-to-string entity-helpers/string-to-string
                        :generate-default-value #(identity "")}))

(defn render-filter
  [schema filtering {field-name ::schema/name :as field-schema}]
  (let [id (str (protocol/gen-id) field-name)
        rendered-value (r/atom nil)
        editing-value (r/atom nil)]
    [(fn []
      (if (schema/relation? field-schema)
        empty-cell
        [:td
         [:div.input-group
           [render-field-filter schema field-schema filtering]
           [:button.btn.btn-primary.form-control.col-4
            {:on-click (fn [_] (re-frame/dispatch [:disable-filtering field-name]))}
            "x"]]]))]))

;end filtering control elements

(defn render-header
  "Renders a header of a table with field names/titles for entity schema."
  [schema type]
  (let [entity-schema (get schema type)
        headers (map
                  (fn [{{min-width ::schema/min-width} ::schema/ui :as field-schema}]
                    [:th (if min-width
                           {:style {:min-width min-width}}
                           {}) (render-name field-schema)])
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
                (fn [field-schema] [:td (render-field schema field-schema entity)])
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
             {:id "delete-button"
              :on-click #(re-frame/dispatch [:delete-entity entity])}
             (raw-html "&times;"))]]
         [:td
          [:button.btn.btn-light
           (deep-merge
             {:id "edit-button"
              :on-click #(re-frame/dispatch [:edit-entity entity])}
             (raw-html "&equiv;"))]]]))))

(defn render-control
  [schema type ordering filtering]
  (let [entity-schema (get schema type)
        visible-field-schemas (->>
                                (::schema/fields entity-schema)
                                (filter visible?))
        add-button [:td
                    [:button.btn.btn-primary
                      {:type "button"
                       :on-click (fn [] (re-frame/dispatch [:edit-entity (entity-helpers/create-empty-entity type entity-schema)]))}
                      "+"]]
        ordering-row (concat
                       [:tr
                        {:key (protocol/gen-id)}]
                       [add-button
                        empty-cell]
                       (mapv
                         (partial ordering-button ordering)
                         visible-field-schemas))
        filtering-row (concat
                        [:tr
                         {:key (protocol/gen-id)}]
                        [empty-cell
                         empty-cell]
                        (mapv
                          (partial render-filter schema filtering)
                          visible-field-schemas))]
    [(vec ordering-row)
     (vec filtering-row)]))

(defn render-pagination
  [pagination]
  (let [limit-input [:td
                      [:input.form-control
                       {:type "text"
                        :size 2
                        :default-value (:limit pagination)
                        :on-blur (fn [event] (let [limit (js/parseInt (-> event .-target .-value))]
                                                 (re-frame/dispatch [:set-pagination {:limit limit}])))
                        }]]
        previous-page [:td
                       [:button.btn.btn-primary
                        {:type     "button"
                         :on-click (fn [] (re-frame/dispatch [:previous-page]))}
                        "<"]]
        page-input [:td
                    [:input.form-control
                     {:type "text"
                      :size 2
                      :default-value (:page pagination)
                      :on-blur (fn [event] (let [page (js/parseInt (-> event .-target .-value))]
                                             (re-frame/dispatch [:set-pagination {:page page}])))
                      }]]
        next-page [:td
                   [:button.btn.btn-primary
                    {:type     "button"
                     :on-click (fn [] (re-frame/dispatch [:next-page]))}
                    ">"]]]
    (into
      []
      (concat
        [:tr
         {:key (protocol/gen-id)}]
         [limit-input previous-page page-input next-page]))))