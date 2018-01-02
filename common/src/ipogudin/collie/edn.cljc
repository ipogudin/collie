(ns ipogudin.collie.edn
  (:require [ipogudin.collie.protocol :as p]
            #?(:clj [clojure.java.io :as io])))

(def
  edn-options
  {:readers
   {'ipogudin.collie.protocol.Request p/map->Request}})

#?(:clj
   (defn read-edn [p]
     "Reads edn file and returns clojure map.
     If path starts from / or ./ it is a local file
     otherwise it is a resource on classpath."
     (if p
       (let [descriptor
             (if (or
                   (.startsWith p "/")
                   (.startsWith p "./"))
               io/file
               io/resource)]
         (->> p
              descriptor
              slurp
              read-string)))))