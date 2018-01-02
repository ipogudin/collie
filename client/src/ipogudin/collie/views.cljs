(ns ipogudin.collie.views
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.protocol :as p]))

(defn button []
  [:input {:type "button" :value "click" :on-click #(do
                                                      (re-frame/dispatch [:clear-value])
                                                      (re-frame/dispatch [:api-request (p/->Request "348r2d7h31478f" [])]))}])

(defn display []
  (let [value (re-frame/subscribe [:value-to-display])]
    (fn []
      [:h1 @value])))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div "Hello from " @name
       [:br]
       [button]
       [:br]
       [display]])))