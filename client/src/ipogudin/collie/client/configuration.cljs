(ns ipogudin.collie.client.configuration
  (:require [mount.core :refer [defstate]]))

(def default-configuration
  {:api-root "/api/"
   :pagination {
                :limit 25
                }})

(defstate configuration :start default-configuration)
