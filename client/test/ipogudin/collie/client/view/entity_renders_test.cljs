(ns ipogudin.collie.client.view.entity-renders-test
  (:require [cljs.test :refer [deftest testing is]]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.client.view.entity-renders :refer [render-name render-text]]))

(defn- field-with-name
  ([name] (field-with-name name nil))
  ([name title]
    (deep-merge
      {::schema/name name}
      (if title {::schema/ui {::schema/title title}}))))

(defn- limited-text
  [text-length]
  {::schema/ui {::schema/preview-text-length text-length}})

(deftest rendering
  (testing "Entity rendering"
    (testing "render-name"
      (is (= nil (render-name nil)))
      (is (= nil (render-name {})))
      (is (= "name" (render-name (field-with-name "name"))))
      (is (= "title" (render-name (field-with-name "name" "title")))))
    (testing "render-text"
      (is (= nil (render-text nil nil)))
      (is (= nil (render-text {} nil)))
      (is (= nil (render-text (limited-text 3) nil)))
      (is (= "ab" (render-text (limited-text 3) "ab")))
      (is (= "abc" (render-text (limited-text 3) "abc")))
      (is (= "abc..." (render-text (limited-text 3) "abcd")))
      (is (= "abc..." (render-text (limited-text 3) "abcdefg hijklmnop qrst"))))))