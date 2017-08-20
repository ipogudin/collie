(defn flatten-1
  [s]
  (reduce
    (fn [s v] (concat s v))
    (list)
    s))

(task-options!
  pom {:project     'ipogudin/collie
       :version     "0.0.1-SNAPSHOT"
       :description "UI to manager entities and relationships"
       :url         "https://github.com/ipogudin/collie"
       :developers  {"Ivan Pogudin" "i.a.pogudin@gmail.com"}
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(def common-env
  {
    :source-paths #{"common/src"}
    :resource-paths #{}
    :dependencies '[[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]
                    [org.clojure/core.async "0.3.443"]]})

(def client-env
  {
    :source-paths #{"client/src"}
    :resource-paths #{"client/resources"}
    :dependencies '[[org.clojure/clojurescript "1.9.671" :scope "provided"]
                    [ajchemist/boot-figwheel "0.5.4-6" :scope "test"]
                    [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                    [com.cemerick/piggieback "0.2.2" :scope "test"]
                    [figwheel-sidecar "0.5.11" :scope "test"]
                    [me.raynes/fs "1.4.5" :scope "test"]]})

(def server-env
  {
    :source-paths #{"server/src"}
    :resource-paths #{}
    :dependencies '[]})

(def dev-server-env
  {
   :source-paths #{"dev-server/src"}
   :resource-paths #{"dev-server/resources"}
   :dependencies '[[io.pedestal/pedestal.service "0.5.2" :scope "provided"]
                   [io.pedestal/pedestal.jetty "0.5.2" :scope "provided"]
                   [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api] :scope "provided"]
                   [org.slf4j/jul-to-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/jcl-over-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/log4j-over-slf4j "1.7.22" :scope "provided"]]})

(def examples-env
  {
   :source-paths #{"examples/src"}
   :resource-paths #{"examples/resources"}
   :dependencies '[[io.pedestal/pedestal.service "0.5.2" :scope "provided"]
                   [io.pedestal/pedestal.jetty "0.5.2" :scope "provided"]
                   [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api] :scope "provided"]
                   [org.slf4j/jul-to-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/jcl-over-slf4j "1.7.22" :scope "provided"]
                   [org.slf4j/log4j-over-slf4j "1.7.22" :scope "provided"]]})

(load-file "figwheel_utils.clj")

(def figwheel-builds
  "List of dev cljs-to-js builds."
  [{:id "collie"
    :source-paths (-> (apply
                        concat
                        (map :source-paths [client-env common-env]))
                      set
                      vec)
    :compiler {:main 'collie.env.dev
               :pretty-print  true
               :output-to "js/collie.js"
               :output-dir "collie.out"
               :asset-path "public/js/collie.out"
               :source-map true
               :optimizations :none}}])

(def target-path
 "Folder to build cljs and less to."
 "target/public")

(def server-agent (agent nil))

(deftask dev-server
         "Dev server"
         []
         (apply merge-env! (-> common-env seq flatten-1))
         (apply merge-env! (-> dev-server-env seq flatten-1))
         (with-post-wrap fileset
           (require 'collie.dev.server)
           (let [run-dev (resolve 'collie.dev.server/run-dev)
                 stop (resolve 'collie.dev.server/stop)]
             (if-not @server-agent
               (send
                 server-agent
                 (fn [s]
                   (if s (stop s))
                   (run-dev)))))))

(deftask dev-client
  "Builds a client part."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> client-env seq flatten-1))
  (require 'boot-figwheel)
  (require 'cemerick.piggieback)
  (let [figwheel (resolve 'boot-figwheel/figwheel)
        wrap-cljs-repl (resolve 'cemerick.piggieback/wrap-cljs-repl)]
    (swap! boot.repl/*default-middleware*
      conj wrap-cljs-repl)
    (comp
      (figwheel-utils/copying-built-js-to-class-path
        (figwheel :target-path target-path
                  :all-builds figwheel-builds
                  :figwheel-options {:server-port 3449
                                     :validate-config true
                                     :css-dirs [(format "%s/css/" target-path)]}))
      (repl :server true)
      (watch)
      (target :no-clean true))))

(deftask dev
   "Runs development server"
   []
   (comp
     (dev-client)
     (dev-server)))

(deftask build-jar
  "Builds a server part artifacts."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> server-env seq flatten-1))
  (apply merge-env! (-> client-env seq flatten-1))
  (comp
    (sift :to-resource [#"(.*)\.clj"])
    (pom)
    (jar)
    (target :no-clean true)
    (install)))

(deftask gen-lein
  "Generates a lein project definition."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> client-env seq flatten-1))
  (apply merge-env! (-> server-env seq flatten-1))
  (apply merge-env! (-> dev-server-env seq flatten-1))
  (apply merge-env! (-> examples-env seq flatten-1))
  (merge-env! :dependencies '[[onetom/boot-lein-generate "0.1.3" :scope "test"]])
  (require 'boot.lein)
  (let [generate (resolve 'boot.lein/generate)]
    (generate))
  identity)
