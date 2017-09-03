(ns ipogudin.collie.core
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [ipogudin.collie.events]
    [ipogudin.collie.subs]
    [ipogudin.collie.views :as views]
    [ipogudin.collie.validation :as validation]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "collie-app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
