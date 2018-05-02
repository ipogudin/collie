(ns ^:figwheel-no-load ipogudin.collie.client.dev.app
                (:require [ipogudin.collie.client.core :as core]
                          [ipogudin.collie.schema]
                          [ipogudin.collie.dev.schema :refer [schema]]))

(defn ^:export init[]
  (core/init schema))