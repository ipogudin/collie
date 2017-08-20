(ns collie.core
  (:require [collie.validation :as validation]))

(defn init! []
  (println (validation/validate)))
