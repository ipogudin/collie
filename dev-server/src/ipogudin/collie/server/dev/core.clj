(ns ipogudin.collie.server.dev.core
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [ipogudin.collie.server.dev.service :as service]
            [ipogudin.collie.server.dev.db :refer [setup-db]]
            [ipogudin.collie.dev.schema :refer [schema]]
            [ipogudin.collie.server.core :refer [init]]))

;; This is an adapted service map, that can be started and stopped
;; From the REPL you can call server/start and server/stop on this service
;(defonce runnable-service (server/create-server service/service))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating your [DEV] server...")
  (init schema)
  (setup-db)
  (-> service/service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::server/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::server/routes #(route/expand-routes (deref #'service/routes))
              ;; all origins are allowed in dev mode
              ::server/allowed-origins {:creds true :allowed-origins (constantly true)}})
      ;; Wire up interceptor chains
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))

(defn stop
  [s]
  (server/stop s))

