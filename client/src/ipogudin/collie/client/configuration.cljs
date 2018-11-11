(ns ipogudin.collie.client.configuration
  (:require [mount.core :refer [defstate]]))

(def default-configuration
  {:api-root "/api/"})

(defstate configuration :start default-configuration)
