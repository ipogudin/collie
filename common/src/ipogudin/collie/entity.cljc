(ns ipogudin.collie.entity
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [#?(:clj clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]))

(def db-name-regex #"^([a-zA-Z]{1}[a-zA-Z0-9\-\_]*[a-zA-Z0-9]{1})|([a-zA-Z]+)$")
(def db-name-spec
  (s/with-gen
    (s/and keyword? #(re-matches db-name-regex (name %)))
    #(gen/fmap
       (fn [n]
         (keyword n))
       (gen/string-alphanumeric))))
(s/def ::type db-name-spec)

(s/def ::entity (s/keys :req [::type]))