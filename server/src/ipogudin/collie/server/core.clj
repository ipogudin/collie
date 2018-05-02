(ns ipogudin.collie.server.core
  (:require [mount.core :as mount]
            [ring.util.response :as ring-resp]
            [ipogudin.collie.server.api :as api]
            [ipogudin.collie.server.dao.sql :as sql]
            [ipogudin.collie.schema]))

(defn response [body]
  (ring-resp/response body))

(defn api-endpoint
  [request]
  (->
    request
    :edn-params
    api/handle
    response))

(defn init-states [states schema]
  (->
    states
    (mount/swap {#'ipogudin.collie.schema/schema schema})
    sql/setup-dao))
