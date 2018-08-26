(ns ipogudin.collie.client.views
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.client.view.common :as common-view]
            [ipogudin.collie.protocol :as p]
            [ipogudin.collie.entity-helpers :refer [sort-entities-by-pk]]
            [ipogudin.collie.client.view.entity-renders :refer [render-name
                                                                render-header
                                                                render-row
                                                                render-control]]
            [ipogudin.collie.client.view.entity-editors :refer [render-entity-editor]]))

(defn list-of-entities []
  (let [schema (re-frame/subscribe [:schema])]
    (fn []
      (let [sorted-schema (sort-by first @schema)]
        [:div.row
         (for [[key entity-schema] sorted-schema]
           [:button.btn.btn-link.col-24
            {:key key
             :type "button"
             :on-click #(re-frame/dispatch [:select-entity key])}
            (render-name entity-schema)])]))))

(defn show-selected-entities []
  (let [selecting (re-frame/subscribe [:selecting])
        schema (re-frame/subscribe [:schema])]
    (fn []
      (if (= :sync (:status @selecting))
        (let [{:keys [entities type]} @selecting
              sorted-entities (sort-entities-by-pk @schema entities)]
          (into []
                (concat
                  [:table]
                  [[:thead (render-header @schema type)]]
                  [(into
                     [:tbody]
                     (concat
                       [(render-control @schema type)]
                       (mapv
                         (partial render-row @schema type)
                         sorted-entities)
                       ))])))))))

(defn show-entity-editor []
  (let [editing (re-frame/subscribe [:editing])
        schema (re-frame/subscribe [:schema])]
    (fn []
      (if (= :sync (:status @editing))
        [common-view/modal-dialog
         [render-entity-editor @schema editing]
         :title "Editor"
         :buttons {:ok
                   {:title "Save"
                    :handler #(re-frame/dispatch [:save-entity (:entity @editing)])}
                   :cancel
                   {:title "Cancel"
                    :handler #(re-frame/dispatch [:edit-entity nil])}}
         :handler #(re-frame/dispatch [:edit-entity nil])]))))

(defn app []
  [:div
   [show-entity-editor]
   [:div.row
     [:div.col-6
      [list-of-entities]]
     [:div.col-18
      [show-selected-entities]]]])