(ns ipogudin.collie.common-test
  (:require [#?(:clj clojure.test
                :cljs cljs.test) :refer [deftest testing is]]
            [ipogudin.collie.common :as common]))

(deftest common-functions
  (testing "Common functions"
    (testing "Format"
      (is (= "string 10 1.12" (common/format "%s %d %1.2f" "string" 10 1.123M))))))