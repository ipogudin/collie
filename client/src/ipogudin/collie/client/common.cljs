(ns ipogudin.collie.client.common
  (:require
    [goog.string :as gstring]
    [goog.string.format]))

(defn format [& args]
  (apply gstring/format args))
