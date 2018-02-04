(ns ipogudin.collie.client.core
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [ipogudin.collie.client.events]
    [ipogudin.collie.client.subs]
    [ipogudin.collie.client.views :as views]
    [ipogudin.collie.validation :as validation]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "collie-app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
