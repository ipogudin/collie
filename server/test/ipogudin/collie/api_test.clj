(ns ipogudin.collie.api-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [com.rpl.specter :refer [select-one setval ALL END NONE] :as specter]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.dao.common]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.api :as api]))

(def entities
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
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}
                   {::schema/name :manufacturer
                    ::schema/field-type ::schema/one-to-one}]}
   ])

(def schema-map (schema/schema-seq-to-map entities))

(def database
  (atom {
         :manufacturers {
                         :seq (atom 2)
                         :table [{:id   1
                                 :name "first manufacturer"
                                 ::entity/type :manufacturers}
                                {:id   2
                                 :name "second manufacturer"
                                 ::entity/type :manufacturers}]}
         :cars {
                :seq (atom 2)
                :table [{:id 1
                        :name "first car"
                        :manufacturer 1
                        ::entity/type :cars}
                       {:id 2
                        :name "second car"
                        :manufacturer 1
                        ::entity/type :cars}
                       {:id 3
                        :name "third car"
                        :manufacturer 2
                        ::entity/type :cars}]}
         }))
;get-by-id [db schema-map entity-type-kw id]
;upsert [db schema-map entity]
;[db schema-map entity-type-kw i
(defn get-by-pk
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (select-one [entity-type-kw :table ALL #(= (get % pk-name-kw) pk-value)] @database)))

(defn upsert
  [db schema-map {entity-type-kw ::entity/type :as entity}]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)
        pk-value (get entity pk-name-kw)]
    (if (nil? pk-value)
      (let [new-pk-value (swap! (get-in database [entity-type-kw :seq]) inc)]
        (swap!
          database
          (setval [entity-type-kw :table END] [entity] @database))
        new-pk-value)
      (swap!
        database
        (setval [entity-type-kw :table ALL #(= (get % pk-name-kw) pk-value)] entity @database)))))

(defn delete
  [db schema-map entity-type-kw pk-value]
  (let [pk-name-kw (-> schema-map entity-type-kw schema/find-primary-key ::schema/name)]
    (swap!
      database
      #(setval
         [:cars :table ALL (fn[entity] (= (get entity pk-name-kw) pk-value))]
         NONE
         @database))))

(defn fixture
  [f]
  (->
    (mount/only #{#'ipogudin.collie.api/schema
                  #'ipogudin.collie.dao.common/get-by-pk
                  #'ipogudin.collie.dao.common/upsert
                  #'ipogudin.collie.dao.common/delete})
    (mount/swap {#'ipogudin.collie.api/schema schema-map
                 #'ipogudin.collie.dao.common/get-by-pk get-by-pk
                 #'ipogudin.collie.dao.common/upsert upsert
                 #'ipogudin.collie.dao.common/delete delete})
    mount/start)
  (try
    (f)
    (finally (mount/stop))))

(use-fixtures :once fixture)

(deftest api
  (testing "execution of get by pk command"
    (let [{result ::protocol/result} (api/execute-one (protocol/get-by-pk-command :cars 1))]
      (is (= (:name result) "first car")))))