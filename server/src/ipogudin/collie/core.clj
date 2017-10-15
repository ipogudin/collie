(ns ipogudin.collie.core
  (:require [ring.util.response :as ring-resp]))

(defn response [body]
  (ring-resp/response body))

(defn api
  [request]
  (response {:value "Hello world!"}))
