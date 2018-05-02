(ns figwheel-utils
  (:require [boot.core :refer :all]
            [clojure.java.io :as io]))

(defn- copy-dir-to-classpath [from to]
  (require 'me.raynes.fs)
  (let [tmp (tmp-dir!)
        copy-dir (resolve 'me.raynes.fs/copy-dir)]
    (fn middleware [next-handler]
      (fn handler [fileset]
        (empty-dir! tmp)
        (copy-dir (io/file from) (io/file tmp to))
        (-> fileset (add-resource tmp) commit! next-handler)))))

(defn copying-built-js-to-class-path
  "As figwheel compiles to a folder on disk (:target-path), this task copies the
   initially compiled js files into `public/js/` on the classpath, so that the
   compojure resources route for `public` can serve figwheel compiled js files,
   along with the rest of the static files."
  [figwheel-middleware from to]
  (comp
   figwheel-middleware
   (copy-dir-to-classpath from to)))
