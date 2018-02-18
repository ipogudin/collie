(ns ipogudin.collie.protocol-test
  (:require [#?(:clj clojure.test
                :cljs cljs.test) :refer [deftest testing is]]
            [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [#?(:clj clojure.spec.test.alpha
                :cljs cljs.spec.test.alpha) :as stest]
            #?(:cljs [clojure.test.check :as stc])
            [ipogudin.collie.test.core :refer [successful?]]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.entity :as entity]))

#?(:clj (alias 'stc 'clojure.spec.test.check))

(deftest id-generation
  (testing "id generation (automatic tests)"
    (is (successful? (stest/check `protocol/gen-id {::stc/opts {:num-tests 10}})))))

(deftest protocol-schema
  (testing "Protocol schema"
    (let [valid-get-by-pk-command (protocol/get-by-pk-command :stubs 1)
          valid-upsert-command (protocol/upsert-command {:some :entity ::entity/type :stubs})
          valid-delete-command (protocol/delete-command :stubs "1")
          valid-get-entities-command-without-options (protocol/get-entities-command :stubs)
          valid-get-entities-command
          (protocol/get-entities-command
            :stubs
            ::protocol/order-by [[:column1 ::protocol/asc]
                                 [:column2 ::protocol/desc]]
            ::protocol/filter [[:column3 10]
                               [:column4 "abc"]]
            ::protocol/limit 10
            ::protocol/offset 0)
          valid-request (-> (protocol/request)
                            (protocol/add-commands
                              valid-get-by-pk-command
                              valid-upsert-command
                              valid-delete-command))]
    (testing "get-by-pk-command"
      (is (nil? (s/explain-data ::protocol/command valid-get-by-pk-command)))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-by-pk-command ::protocol/id))))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-by-pk-command ::protocol/code))))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-by-pk-command ::protocol/pk-value)))))
    (testing "upsert-command"
      (is (nil? (s/explain-data ::protocol/command valid-upsert-command)))
      (is (not (s/valid? ::protocol/command (dissoc valid-upsert-command ::protocol/id))))
      (is (not (s/valid? ::protocol/command (dissoc valid-upsert-command ::protocol/code))))
      (is (not (s/valid? ::protocol/command (dissoc valid-upsert-command ::entity/entity)))))
    (testing "delete-command"
      (is (nil? (s/explain-data ::protocol/command valid-delete-command)))
      (is (not (s/valid? ::protocol/command (dissoc valid-delete-command ::protocol/id))))
      (is (not (s/valid? ::protocol/command (dissoc valid-delete-command ::protocol/code))))
      (is (not (s/valid? ::protocol/command (dissoc valid-delete-command ::protocol/pk-value)))))
    (testing "get-entities-command-without-options"
      (is (nil? (s/explain-data ::protocol/command valid-get-entities-command-without-options)))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-entities-command-without-options ::protocol/id))))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-entities-command-without-options ::protocol/code)))))
    (testing "get-entities-command"
      (is (nil? (s/explain-data ::protocol/command valid-get-entities-command)))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-entities-command ::protocol/id))))
      (is (not (s/valid? ::protocol/command (dissoc valid-get-entities-command ::protocol/code)))))
    (testing "request"
      (is (nil? (s/explain-data ::protocol/request valid-request)))))))