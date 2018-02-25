(ns ipogudin.collie.client.common-test
  (:require [cljs.test :refer [deftest testing is]]
            [ipogudin.collie.client.common :refer [format]]))

(deftest common-functions
  (testing "Common functions"
    (testing "Format"
      (is (= "string" (format "%s" "string"))))))
