(defn flatten-1
  [s]
  (reduce
    (fn [s v] (concat s v))
    (list)
    s))

(def version "0.0.2")
(def prefix "META-INF/resources")
(def distribution-path (str prefix "/webjars/collie/" version "/dist/"))
(def js-distribution-path (str distribution-path "js/"))
(def css-distribution-path (str distribution-path "css/"))
(def js-url-path (clojure.string/replace js-distribution-path prefix ""))

(set-env! :dependencies '[[adzerk/boot-test "1.2.0" :scope "test"]
                          [adzerk/boot-cljs "2.1.4" :scope "provided"]
                          [crisptrutski/boot-cljs-test "0.3.5-SNAPSHOT"
                           :scope "test"
                           :exclusions [org.clojure/clojure
                                        org.clojure/tools.reader
                                        org.clojure/clojurescript
                                        adzerk/boot-cljs]]
                          [deraen/boot-sass "0.3.1" :scope "provided"]])
(require '[adzerk.boot-test :refer :all]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[deraen.boot-sass :refer [sass]]
         '[adzerk.boot-cljs :refer [cljs]])

(task-options!
  pom {:project     'ipogudin/collie
       :version     version
       :description "UI to manager entities in DB"
       :url         "https://github.com/ipogudin/collie"
       :developers  {"Ivan Pogudin" "i.a.pogudin@gmail.com"}
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(def common-env
  {
    :source-paths #{"common/src"}
    :test-paths #{"common/test"}
    :resource-paths #{}
    :dependencies '[[org.clojure/clojure "1.9.0" :scope "provided"]
                    [org.clojure/test.check "0.10.0-alpha2" :scope "test"]
                    [org.clojure/tools.reader "1.1.1" :scope "provided"]
                    [org.clojure/core.async "0.3.465" :exclusions [org.clojure/tools.reader]]
                    [com.rpl/specter "1.1.0" :scope "provided"]
                    [javax.xml.bind/jaxb-api "2.3.0" :scope "provided"]
                    [mount "0.1.11" :scope "provided"]]})

(def client-env
  {
    :source-paths #{"client/src"}
    :test-paths #{"client/test"}
    :resource-paths #{"client/resources"}
    :dependencies '[[org.clojure/clojurescript "1.9.946" :scope "provided"]
                    [re-frame "0.10.5" :scope "provided" :exclusions [org.clojure/tools.reader]]
                    [reagent "0.7.0" :scope "provided" :exclusions [org.clojure/tools.reader]]
                    [day8.re-frame/http-fx "0.1.6" :scope "provided" :exclusions [org.clojure/tools.reader]]
                    [com.andrewmcveigh/cljs-time "0.5.2"]
                    [org.webjars.bower/bootstrap "4.0.0" :scope "provided"]
                    [org.webjars.bower/jquery "3.3.1" :scope "provided"]
                    [org.webjars.bower/popper.js "1.12.9" :scope "provided"]
                    [ajchemist/boot-figwheel "0.5.4-6" :scope "test"]
                    [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                    [com.cemerick/piggieback "0.2.2" :scope "test" :exclusions [org.clojure/tools.reader]]
                    [figwheel-sidecar "0.5.14" :scope "test" :exclusions [org.clojure/tools.reader]]
                    [me.raynes/fs "1.4.6" :scope "test"]
                    [doo "0.1.8" :scope "test"]]})

(def server-env
  {
    :source-paths #{"server/src"}
    :test-paths #{"server/test"}
    :resource-paths #{"server/resources"}
    :dependencies '[[ring/ring-core "1.6.1" :scope "provided"]
                    [org.clojure/java.jdbc "0.7.3" :scope "provided"]
                    [c3p0/c3p0 "0.9.1.2"]
                    [org.clojure/tools.logging "0.4.0"]
                    [clj-time "0.14.3"]
                    [com.h2database/h2 "1.4.196" :scope "test"]]})

(def dev-server-env
  {
   :source-paths #{"dev-server/src"}
   :resource-paths #{"dev-server/resources"}
   :dependencies '[[io.pedestal/pedestal.service "0.5.3" :scope "provided" :exclusions [org.clojure/tools.reader]]
                   [io.pedestal/pedestal.jetty "0.5.3" :scope "provided" :exclusions [org.clojure/tools.reader]]
                   [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api] :scope "provided"]
                   [org.slf4j/jul-to-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/jcl-over-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/log4j-over-slf4j "1.7.22" :scope "provided"]]})

(defn set-env
  "Sets boot environment from custom environment."
  [env]
  (apply merge-env! (-> env seq flatten-1)))

(defn set-envs
  "Sets boot environment from a sequence of custom environments"
  [f & envs]
  (let [[f envs]
        (if (fn? f)
          [f envs]
          [identity (conj envs f)])]
    (doseq [env envs]
      (set-env (f env)))))

(load-file "figwheel_utils.clj")

(def server-agent (agent nil))

(deftask dev-server
         "Dev server"
         []
         (set-envs common-env server-env dev-server-env)
         (with-post-wrap fileset
           (require 'ipogudin.collie.server.dev.core)
           (let [run-dev (resolve 'ipogudin.collie.server.dev.core/run-dev)
                 stop (resolve 'ipogudin.collie.server.dev.core/stop)]
             (if-not @server-agent
               (send
                 server-agent
                 (fn [s]
                   (if s (stop s))
                   (run-dev)))))))

(def figwheel-builds
  "List of dev cljs-to-js builds."
  [{:id "collie"
    :source-paths (-> (apply
                        concat
                        (map :source-paths [client-env common-env dev-server-env]))
                      set
                      vec)
    :compiler {:main 'ipogudin.collie.client.dev.core
               :pretty-print  true
               :output-to (str js-distribution-path "collie.js")
               :output-dir "collie.out"
               :asset-path (str js-url-path "collie.out")
               :source-map true
               :optimizations :none
               :verbose true}}])

(deftask dev-client
  "Builds a client part."
  []
  (set-envs common-env client-env)
  (require 'boot-figwheel)
  (require 'cemerick.piggieback)
  (let [figwheel (resolve 'boot-figwheel/figwheel)
        wrap-cljs-repl (resolve 'cemerick.piggieback/wrap-cljs-repl)]
    (swap! boot.repl/*default-middleware*
      conj wrap-cljs-repl)
    (comp
      (figwheel-utils/copying-built-js-to-class-path
        (figwheel :target-path "target"
                  :all-builds figwheel-builds
                  :figwheel-options {:server-port 9449
                                     :validate-config true
                                     :css-dirs [(str "target/" css-distribution-path)]})
        (str "./target/" js-distribution-path)
        js-distribution-path)
      (watch)
      (sass :output-style :nested)
      (sift :move {#"css/" (str css-distribution-path)})
      (target :no-clean true))))

(deftask dev
   "Runs development server"
   []
   (comp
     (repl :server true)
     (dev-client)
     (dev-server)
     (target :no-clean false)))

(defn prod-client
  [m]
  (set-envs
    common-env
    client-env)
  (comp
    (sass :output-style :nested)
    (sift :move {#"css/" (str css-distribution-path)})
    (cljs
      :compiler-options
      {:main m
       :output-to (str js-distribution-path "collie.js")
       :output-dir "collie.out"
       :externs ["externs/jquery.js" "externs/bootstrap.js"]
       ;:asset-path "public/js/collie.out"
       :optimizations :advanced
       :pretty-print  false
       :verbose true})
    (sift :include [#"^collie.out.*"] :invert true)
    (sift :move {#"externs/" (str js-distribution-path "externs/")})
    (target :no-clean true)))

(deftask build-client
   "Builds client side code to pack it as artifacts in jar file."
   []
   (prod-client 'ipogudin.collie.client.core))

(deftask integrate-client
   "Builds client side code to run it for integration tests.
   It is needed to validate that an optimized build (js code) works correctly."
   []
   (prod-client 'ipogudin.collie.client.dev.app))

(deftask integrate
   []
   (comp
     (repl :server true)
     (watch)
     (integrate-client)
     (dev-server)
     (target :no-clean false)))

(deftask build-jar
  "Builds a jar file which is ready to be pushed into maven repository (including local)."
  []
  (set-envs
    common-env
    server-env
    client-env)
  (comp
    (sift :to-resource [#"(.*)\.clj"])
    (build-client)
    (pom)
    (jar)
    (target :no-clean false)))

(defn prepare-test-env
  [env]
  (merge
    env
    {:source-paths
     (clojure.set/union
       (:source-paths env)
       (:test-paths env))}))

(deftask set-environment-for-server-tests
         "Prepares server environment for tests"
         []
         (set-envs
           prepare-test-env
           common-env
           server-env)
         identity)

(deftask set-environment-for-client-tests
         "Prepares client environment for tests"
         []
         (set-envs
           prepare-test-env
           common-env
           client-env)
         (task-options!
           test-cljs {:cljs-opts {:verbose true}})
         identity)

(defn set-environment-for-all-modules
  []
  (set-envs
    common-env
    client-env
    server-env
    dev-server-env))

(deftask set-full-environment
         "Sets full environment including all submodules."
         []
         (set-environment-for-all-modules)
         identity)

(load-file "boot_lein.clj")

(deftask gen-lein
  "Generates a lein project definition."
  []
  (set-environment-for-all-modules)
  (require 'boot-lein)
  (let [generate (resolve 'boot-lein/generate)]
    (generate))
  identity)
