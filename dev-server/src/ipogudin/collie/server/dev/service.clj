(ns ipogudin.collie.server.dev.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.cors :as cors]
            [io.pedestal.http.secure-headers :as sec-headers]
            [io.pedestal.interceptor.helpers :as interceptor]
            [ring.middleware.resource :as resource]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [ring.middleware.resource :as resource]
            [ipogudin.collie.server.core :refer [api-endpoint]]
            [ipogudin.collie.edn :as collie-edn]))

(defn home-page
  [request]
  (->
    (ring-resp/resource-response "index.html")
    (ring-resp/content-type "text/html")))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def edn-interceptors
  [(body-params/body-params (body-params/default-parser-map :edn-options collie-edn/edn-options))])

(def html-interceptors [http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj html-interceptors `home-page)]
              ["/api/" :post (conj edn-interceptors `api-endpoint)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by pedestal-example.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::http/resource-paths ["/public" "/META-INF/resources"]

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port 9080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

(defn resource
  "Interceptor for resource ring middleware"
  [root-path]
  (interceptor/handler
    ::resource
    #(resource/resource-request % root-path)))

(defn default-interceptors
  "Builds interceptors given an options map with keyword keys prefixed by namespace e.g.
  :io.pedestal.http/routes or ::bootstrap/routes if the namespace is aliased to bootstrap.

  Note:
    No additional interceptors are added if :interceptors key is set.

  Options:

  * :routes: Something that satisfies the io.pedestal.http.route/ExpandableRoutes protocol
    a function that returns routes when called, or a seq of route maps that defines a service's routes.
    If passing in a seq of route maps, it's recommended to use io.pedestal.http.route/expand-routes.
  * :router: The router implementation to to use. Can be :linear-search, :map-tree
    :prefix-tree, or a custom Router constructor function. Defaults to :map-tree, which fallsback on :prefix-tree
  * :file-path: File path used as root by the middlewares/file interceptor. If nil, this interceptor
    is not added. Default is nil.
  * :resource-path: File path used as root by the middlewares/resource interceptor. If nil, this interceptor
    is not added. Default is nil.
  * :method-param-name: Query string parameter used to set the current HTTP verb. Default is _method.
  * :allowed-origins: Determines what origins are allowed for the cors/allow-origin interceptor. If
     nil, this interceptor is not added. Default is nil.
  * :not-found-interceptor: Interceptor to use when returning a not found response. Default is
     the not-found interceptor.
  * :mime-types: Mime-types map used by the middlewares/content-type interceptor. Default is {}.
  * :enable-session: A settings map to include the session middleware interceptor. If nil, this interceptor
     is not added.  Default is nil.
  * :enable-csrf: A settings map to include the csrf-protection interceptor. This implies
     sessions are enabled. If nil, this interceptor is not added. Default is nil.
  * :secure-headers: A settings map for various secure headers.
     Keys are: [:hsts-settings :frame-options-settings :content-type-settings :xss-protection-settings]
     If nil, this interceptor is not added.  Default is the default secure-headers settings"
  [service-map]
  (let [{interceptors ::http/interceptors
         routes ::http/routes
         router ::http/router
         file-path ::http/file-path
         resource-paths ::http/resource-paths
         method-param-name ::http/method-param-name
         allowed-origins ::http/allowed-origins
         not-found-interceptor ::http/not-found-interceptor
         ext-mime-types ::http/mime-types
         enable-session ::http/enable-session
         enable-csrf ::http/enable-csrf
         secure-headers ::http/secure-headers
         :or {file-path nil
              router :prefix-tree
              resource-path nil
              not-found-interceptor http/not-found
              method-param-name :_method
              ext-mime-types {}
              enable-session nil
              enable-csrf nil
              secure-headers {}}} service-map
        processed-routes (cond
                           (satisfies? route/ExpandableRoutes routes) (route/expand-routes routes)
                           (fn? routes) routes
                           (nil? routes) nil
                           (and (seq? routes) (every? map? routes)) routes
                           :else (throw (ex-info "Routes specified in the service map don't fulfill the contract.
                                                 They must be a seq of full-route maps or satisfy the ExpandableRoutes protocol"
                                                 {:routes routes})))]
    (if-not interceptors
      (assoc service-map ::http/interceptors
                         (cond-> []
                                 true (conj http/log-request)
                                 (not (nil? allowed-origins)) (conj (cors/allow-origin allowed-origins))
                                 true (conj not-found-interceptor)
                                 (or enable-session enable-csrf) (conj (middlewares/session (or enable-session {})))
                                 enable-csrf (conj (csrf/anti-forgery enable-csrf))
                                 true (conj (middlewares/content-type {:mime-types ext-mime-types}))
                                 true (conj route/query-params)
                                 true (conj (route/method-param method-param-name))
                                 ;; TODO: If all platforms support async/NIO responses, we can bring this back
                                 ;(not (nil? resource-path)) (conj (middlewares/fast-resource resource-path))
                                 true (conj (middlewares/not-modified))
                                 (not (nil? resource-paths)) (concat (map #(resource %) resource-paths))
                                 (not (nil? file-path)) (conj (middlewares/file file-path))
                                 (not (nil? secure-headers)) (conj (sec-headers/secure-headers secure-headers))
                                 true (conj (route/router processed-routes router))))
      service-map)))

