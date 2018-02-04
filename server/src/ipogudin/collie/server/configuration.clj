(ns ipogudin.collie.server.configuration
  (:require [mount.core :refer [defstate]]
            [ipogudin.collie.edn :refer [read-edn]]
            [ipogudin.collie.common :refer [deep-merge]]))

(defstate default-configuration-path :start "default-configuration.edn")
(defstate configuration-path :start "configuration.edn")

(defn load-configuration-by-path [path]
  (read-edn path))

(defn load-configuration []
  (let [
        default-configuration (load-configuration-by-path default-configuration-path)
        configuration (load-configuration-by-path configuration-path)
        ]
    (deep-merge default-configuration configuration)))

(defstate configuration :start (load-configuration))