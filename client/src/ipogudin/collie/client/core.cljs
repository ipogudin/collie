(ns ipogudin.collie.client.core
  (:require
    [mount.core :as mount]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [ipogudin.collie.edn]
    [ipogudin.collie.client.events]
    [ipogudin.collie.client.subs]
    [ipogudin.collie.client.views :as views]
    [ipogudin.collie.validation :as validation]
    [ipogudin.collie.schema]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "collie-app")))

(defn ^:export init
  ([]
    (re-frame/dispatch-sync [:initialize-db])
    (mount-root))
  ([schema]
   (->
     (mount/find-all-states)
     (mount/swap {#'ipogudin.collie.schema/schema schema})
     mount/start)
   (init)))
