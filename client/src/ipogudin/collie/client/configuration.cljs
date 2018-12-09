(ns ipogudin.collie.client.configuration
  (:require [mount.core :refer [defstate]]))

(def default-configuration
  {:api-root "/api/"
   :pagination {
                :limit 50
                }})

(defstate configuration :start default-configuration)
