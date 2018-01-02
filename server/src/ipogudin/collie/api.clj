(ns ipogudin.collie.api
  (:require [ipogudin.collie.protocol :as p]))

(defn handle
  [^ipogudin.collie.protocol.Request r]
  {:request r})
