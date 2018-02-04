(ns ipogudin.collie.client.views
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.protocol :as p]))

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
         [:button.btn.btn-link.col-12
          {:key n
           :type "button"
           :on-click #(re-frame/dispatch [:select-entity-type n])}
          (name n)])])))

(defn show-entities []
  (let [entities (re-frame/subscribe [:opened-entities])]
    (fn []
      [:div (str @entities)])))

(defn app []
  [:div.row
   [:div.col-3
    [list-of-entities]]
   [:div.col-9
    [show-entities]]])