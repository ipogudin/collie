(ns ipogudin.collie.edn
  (:require #?(:clj [clojure.java.io :as io])
            #?(:clj [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:cljs [cljs.reader :as reader]))
  #?(:clj (:import [org.joda.time DateTime LocalDate])))

(def iso8601-formatter (f/formatters :date-time))
(def date-formatter (f/formatters :date))

(defn datetime->reader-str [d]
  (str "#joda/DateTime \"" (f/unparse iso8601-formatter d) \"))

(defn date->reader-str [d]
  (str "#joda/Date \"" (f/unparse-local-date date-formatter d) \"))

(defn reader-str->datetime [s]
  (f/parse iso8601-formatter s))

(defn reader-str->date [s]
  (f/parse-local-date date-formatter s))

(def
  edn-options
  {:readers
   {'joda/DateTime reader-str->datetime
    'joda/Date reader-str->date}})

;;
;; cljs registering readers and writers
;;

#?(:clj (defmethod print-dup DateTime [^DateTime d out]
          (.write out (datetime->reader-str d))))

#?(:clj (defmethod print-method DateTime [^DateTime d out]
          (.write out (datetime->reader-str d))))

#?(:clj (defmethod print-dup LocalDate [^LocalDate d out]
          (.write out (date->reader-str d))))

#?(:clj (defmethod print-method LocalDate [^LocalDate d out]
          (.write out (date->reader-str d))))

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

#?(:clj
   (defn write-edn [file-path o]
     (with-open [w (io/writer file-path)]
       (.write w (pr-str o)))))

;;
;; cljs registering readers and writers
;;

#?(:cljs (reader/register-tag-parser! 'joda/DateTime reader-str->datetime))

#?(:cljs (reader/register-tag-parser! 'joda/Date reader-str->date))

#?(:cljs (extend-protocol IPrintWithWriter
           goog.date.DateTime
           (-pr-writer [d out opts]
             (-write out (datetime->reader-str d)))
           goog.date.Date
           (-pr-writer [d out opts]
             (-write out (date->reader-str d)))))
