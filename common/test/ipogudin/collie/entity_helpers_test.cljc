(ns ipogudin.collie.entity-helpers-test
  (:require [#?(:clj clojure.test
                :cljs cljs.test) :refer [deftest testing is]]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.entity :as e]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity-helpers :refer [set-field-value]]))

(def schema-map
  (schema/schema-seq-to-map
    [
     {::schema/name :a
      ::schema/fields
                    [
                     {::schema/name :id
                      ::schema/field-type ::schema/serial
                      ::schema/primary-key true}
                     {::schema/name :name
                      ::schema/field-type ::schema/string}
                     {::schema/name :int
                      ::schema/field-type ::schema/int}
                     {::schema/name :b
                      ::schema/field-type ::schema/one-to-one
                      ::schema/related-entity :b
                      ::schema/related-entity-field :id}
                     {::schema/name :c_entities
                      ::schema/field-type ::schema/one-to-many
                      ::schema/related-entity :c
                      ::schema/related-entity-field :a}
                     {::schema/name :d_entities
                      ::schema/field-type ::schema/many-to-many
                      ::schema/related-entity :d
                      ::schema/related-entity-field :id
                      ::schema/relation :a_to_d
                      ::schema/left :a
                      ::schema/right :d}]}
     {::schema/name :b
      ::schema/fields
                    [
                     {::schema/name :id
                      ::schema/field-type ::schema/serial
                      ::schema/primary-key true}
                     {::schema/name :name
                      ::schema/field-type ::schema/string}]}
     {::schema/name :c
      ::schema/fields
                    [
                     {::schema/name :id
                      ::schema/field-type ::schema/serial
                      ::schema/primary-key true}
                     {::schema/name :name
                      ::schema/field-type ::schema/string}
                     {::schema/name :a
                      ::schema/field-type ::schema/int}]}
     {::schema/name :d
      ::schema/fields
                    [
                     {::schema/name :id
                      ::schema/field-type ::schema/serial
                      ::schema/primary-key true}
                     {::schema/name :name
                      ::schema/field-type ::schema/string}]}
     ]))

(deftest setting-field-value
  (testing "Setting field"
    (testing "Updating field with direct value"
      (let [a {:id 1 :name "A" :int 5 ::e/type :a}
            field-for-update (-> schema-map :a (schema/find-field-schema :name))
            new-value "AAA"]
        (is (= (assoc a :name new-value)
               (set-field-value schema-map field-for-update a new-value)))))
    (testing "Updating one-to-one relation field"
      (let [a {:id 1 :name "A" :int 5 :b 1 ::e/type :a :deps {:b {:id 1 :name "B" ::e/type :b}}}
            field-for-update (-> schema-map :a (schema/find-field-schema :b))
            new-value {:id 2 :name "B2" ::e/type :b}]
        (is (= (deep-merge a {:b 2 :deps {:b new-value}})
               (set-field-value schema-map field-for-update a new-value)))))
    (testing "Updating one-to-many relation field"
      (let [a {:id 1
               :name "A"
               :int 5
               ::e/type :a
               :deps {:c_entities [{:id 1
                                    :name "C"
                                    :a 1
                                    ::e/type :c}
                                   {:id 2
                                    :name "C2"
                                    :a 1
                                    ::e/type :c}]}}
            field-for-update (-> schema-map :a (schema/find-field-schema :c_entities))
            new-value [{:id 2 :name "C2" ::e/type :c} {:id 3 :name "C3" ::e/type :c}]]
        (is (= (assoc-in a [:deps :c_entities] (mapv #(assoc % :a 1) new-value))
               (set-field-value schema-map field-for-update a new-value)))))
    (testing "Updating many-to-many relation field"
      (let [a {:id 1
               :name "A"
               :int 5
               ::e/type :a
               :deps {:d_entities [{:id 1
                                    :name "D"
                                    ::e/type :d}
                                   {:id 2
                                    :name "D2"
                                    ::e/type :d}]}}
            field-for-update (-> schema-map :a (schema/find-field-schema :d_entities))
            new-value [{:id 2 :name "D2" ::e/type :d} {:id 3 :name "D3" ::e/type :d}]]
        (is (= (assoc-in a [:deps :d_entities] new-value)
               (set-field-value schema-map field-for-update a new-value)))))))