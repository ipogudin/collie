(ns ipogudin.collie.client.views
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.protocol :as p]
            [ipogudin.collie.client.view.entity-renders :refer [render-name
                                                                render-header
                                                                render-row]]))

(defn button []
  [:input {:type "button" :value "click" :on-click #(do
                                                      (re-frame/dispatch [:clear-value])
                                                      (re-frame/dispatch [:api-request (p/request)]))}])

(defn display []
  (let [value (re-frame/subscribe [:value-to-display])]
    (fn []
      [:h1 @value])))

(defn list-of-entities []
  (let [schema (re-frame/subscribe [:schema])]
    (fn []
      [:div.row
       (for [n (keys @schema)]
         [:button.btn.btn-link.col-24
          {:key n
           :type "button"
           :on-click #(re-frame/dispatch [:select-entity-type n])}
          (name n)])])))

(defn show-selected-entities []
  (let [selected (re-frame/subscribe [:selected])]
    (fn []
      (if (:filled @selected)
        (let [{:keys [entities type]} @selected]
          (into []
                (concat
                  [:table]
                  [[:thead (render-header type)]]
                  [[:tbody (map
                            (partial render-row type)
                            entities)]])))))))

(defn app []
  [:div.row
   [:div.col-6
    [list-of-entities]]
   [:div.col-18
    [show-selected-entities]]])