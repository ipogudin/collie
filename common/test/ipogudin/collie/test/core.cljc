(ns ipogudin.collie.test.core
  (:require [#?(:clj clojure.test
                :cljs cljs.test) :refer [deftest testing is]]))

(defn successful?
  "Checks result of auto-generated tests fro clojure specs."
  [spec-result]
  (every? (comp nil? :failure) spec-result))

