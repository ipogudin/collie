(ns ipogudin.collie.core
  (:require [ipogudin.collie.validation :as validation]))

(defn init! []
  (println (validation/validate)))
