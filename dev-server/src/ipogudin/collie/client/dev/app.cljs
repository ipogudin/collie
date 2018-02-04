(ns ^:figwheel-no-load ipogudin.collie.client.dev.app
  (:require [mount.core :as mount]
            [ipogudin.collie.client.core :as core]
            [ipogudin.collie.schema]
            [ipogudin.collie.dev.schema :refer [schema]]))

(defn init[]
  (->
    (mount/find-all-states)
    (mount/swap {#'ipogudin.collie.schema/schema schema})
    mount/start)
  (core/init))