(ns ipogudin.collie.schema-test
  (:require [#?(:clj clojure.test
               :cljs cljs.test) :refer [deftest testing is]]
            [#?(:clj clojure.spec.test.alpha
                :cljs cljs.spec.test.alpha) :as stest]
            #?(:cljs [clojure.test.check :as stc])
            [ipogudin.collie.test.core :refer [successful?]]
            [ipogudin.collie.schema :as schema]))

#?(:clj (alias 'stc 'clojure.spec.test.check))

(def schema-vec
  [
   {::schema/name :manufacturers
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}]}
   {::schema/name :cars
    ::schema/ui {
                 ::schema/title "Cars"
                 }
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string
                    ::schema/ui {
                                 ::schema/title "Name"
                                 }}
                   {::schema/name :manufacturer
                    ::schema/field-type ::schema/one-to-one
                    ::schema/ui {
                                 ::schema/title "Manufacturer"
                                 }}]}
   ])

(deftest schema-converters
  (testing "converting a schema from seq to map"
    (let [schema-map (schema/schema-seq-to-map schema-vec)
          manufacturers (:manufacturers schema-map)
          cars (:cars schema-map)]
      (is (= (count schema-map) 2))
      (is (some? manufacturers))
      (is (= (->> manufacturers ::schema/fields count) 2))
      (is (some? cars))
      (is (= (->> cars ::schema/fields count) 3)))))

(deftest finding-primary-key
  (testing "Finding primary key"
    (testing "a primary key should be found in an entity"
      (let [entity (first schema-vec)
            primary-key (schema/find-primary-key entity)]
        (is (some? primary-key))
        (is (= (::schema/name primary-key) :id))))
    (testing "finding a primary key (automatic tests)"
      (is (successful? (stest/check `schema/find-primary-key {::stc/opts {:num-tests 10}}))))))

(deftest field-spec
  (testing "common metadata"))