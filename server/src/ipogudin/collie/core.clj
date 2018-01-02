(ns ipogudin.collie.core
  (:require [mount.core :as mount]
            [ring.util.response :as ring-resp]
            [ipogudin.collie.api :as api]
            [ipogudin.collie.dao.sql :as sql]))

(defn response [body]
  (ring-resp/response body))

(defn api-endpoint
  [request]
  (->
    request
    :edn-params
    api/handle
    response))

(defn init []
  (->
    (mount/find-all-states)
    sql/setup-dao
    mount/start))
