(defn flatten-1
  [s]
  (reduce
    (fn [s v] (concat s v))
    (list)
    s))

(task-options!
  pom {:project     'collie
       :version     "0.0.1-SNAPSHOT"
       :description "UI to manager entities and relationships"
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(def common-env
  {
    :source-paths #{"common/src"}
    :resource-paths #{}
    :dependencies '[[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]]})

(def client-env
  {
    :source-paths #{"client/src"}
    :resource-paths #{"client/resources"}
    :dependencies '[[org.clojure/clojurescript "1.9.671" :scope "provided"]
                    [ajchemist/boot-figwheel "0.5.4-6" :scope "test"]
                    [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                    [com.cemerick/piggieback "0.2.1" :scope "test"]
                    [figwheel-sidecar "0.5.11" :scope "test"]
                    [me.raynes/fs "1.4.5" :scope "test"]]})

(def server-env
  {
    :source-paths #{"server/src"}
    :resource-paths #{}
    :dependencies '[]})

(load-file "figwheel_utils.clj")

(def builds
  "List of dev cljs-to-js builds."
  [{:id "collie"
    :source-paths ["client/src"]
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

(deftask build-client
  "Builds a client part."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> client-env seq flatten-1))
  (require 'boot-figwheel)
  (require 'cemerick.piggieback)
  (let [figwheel (resolve 'boot-figwheel/figwheel)
        wrap-cljs-repl (resolve 'cemerick.piggieback/wrap-cljs-repl)]
    (swap! boot.repl/*default-middleware*
      conj 'cemerick.piggieback/wrap-cljs-repl)
    (comp
      (figwheel-utils/copying-built-js-to-class-path
        (figwheel :target-path target-path
                  :all-builds builds
                  :figwheel-options {:server-port 7000
                                     :validate-config true
                                     :css-dirs [(format "%s/css/" target-path)]
                                     :open-file-command "emacsclient"}))
      (repl :server true)
      (watch)
      (target :no-clean true))))

(deftask build-server
  "Builds a server part."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> server-env seq flatten-1))
  (comp
    (aot :all true)
    (uber)
    (jar :file "server.jar")
    (target)))

(deftask gen-lein
  "Generates a lein project definition."
  []
  (apply merge-env! (-> common-env seq flatten-1))
  (apply merge-env! (-> client-env seq flatten-1))
  (apply merge-env! (-> server-env seq flatten-1))
  (merge-env! :dependencies '[[onetom/boot-lein-generate "0.1.3" :scope "test"]])
  (require 'boot.lein)
  (let [generate (resolve 'boot.lein/generate)]
    (generate))
  identity)
