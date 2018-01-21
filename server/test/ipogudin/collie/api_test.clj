(ns ipogudin.collie.api-test
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [clojure.java.jdbc :as j]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.dao.common :as common-dao]
            [ipogudin.collie.configuration :as configuration]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.dao.sql :as sql-dao]
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

(def conf
  {
   :db {
        :classname "org.h2.Driver"
        :subprotocol "h2:mem"
        :subname "api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
        :user "sa"
        :password ""
        }
   :db-pool {
             :min-size 1
             :max-size 10
             :acquire-retry-attempts 1
             :acquire-retry-delay 500 ;milliseconds
             :max-idle-time-excess-connections 1800 ;seconds
             :max-idle-time 10800 ;seconds
             }
   })

(defn setup-db
  [db]
  (j/db-do-commands
    db
    [
     (j/create-table-ddl
       :manufacturers
       [[:id "serial"]
        [:name "varchar(256)"]])
     (j/create-table-ddl
       :cars
       [[:id "serial"]
        [:name "varchar(256)"]
        [:manufacturer "integer NOT NULL REFERENCES manufacturers (id)"]])
     (j/create-table-ddl
       :showrooms
       [[:id "serial"]
        [:name "varchar(256)"]])
     (j/create-table-ddl
       :showrooms_to_cars
       [[:car "integer NOT NULL REFERENCES cars (id)"]
        [:showroom "integer NOT NULL REFERENCES showrooms (id)"]])])
  (j/with-db-transaction
    [t db]
    (j/insert! t :manufacturers {:name "Factory1"})
    (j/insert! t :manufacturers {:name "Factory2"})
    (j/insert! t :manufacturers {:name "Factory3"})
    (j/insert! t :cars {:name "Car1" :manufacturer 1})
    (j/insert! t :cars {:name "Car2" :manufacturer 1})
    (j/insert! t :cars {:name "Car3" :manufacturer 2})))

(defn fixture
  [f]
  (->
    (mount/only #{#'ipogudin.collie.configuration/configuration
                  #'ipogudin.collie.api/schema
                  #'ipogudin.collie.dao.common/db
                  #'ipogudin.collie.dao.common/init-db
                  #'ipogudin.collie.dao.common/get-by-pk
                  #'ipogudin.collie.dao.common/upsert
                  #'ipogudin.collie.dao.common/delete
                  #'ipogudin.collie.dao.common/get-entities})
    (mount/swap {#'ipogudin.collie.api/schema schema-map
                 #'ipogudin.collie.configuration/configuration conf})
    sql-dao/setup-dao
    mount/start)
  (try
    (setup-db common-dao/db)
    (f)
    (finally (mount/stop))))

(use-fixtures :once fixture)

(deftest api
  (testing "execution of getting by pk command"
    (let [{result ::protocol/result}
          (api/execute-one
            (protocol/get-by-pk-command :cars 1))]
      (is (= "Car1" (:name result)))))
  (testing "execution of upsert command"
    (let [{result1 ::protocol/result}
          (api/execute-one
            (protocol/upsert-command
              {:name "Car4"
               :manufacturer 3
               ::entity/type :cars}))
          created-car (j/get-by-id common-dao/db :cars result1 :id)
          {result2 ::protocol/result}
          (api/execute-one
            (protocol/upsert-command
              {:id (:id created-car)
               :name "Car4 (updated)"
               :manufacturer 3
               ::entity/type :cars}))
          updated-car (j/get-by-id common-dao/db :cars result2 :id)]
      (is (= result1 result2) "IDs should be identical for created and updated entities")
      (is (= "Car4" (:name created-car)))
      (is (= "Car4 (updated)" (:name updated-car)))))
  (testing "execution of deletion command"
    (let [pk-value (->
                     (j/insert!
                       common-dao/db
                       :cars
                       {:name "Cars for deletion" :manufacturer 3})
                     first
                     vals
                     first)
          {result ::protocol/result}
          (api/execute-one
            (protocol/delete-command
              :cars
              pk-value))
          deleted-car (j/get-by-id common-dao/db :cars pk-value :id)]
      (is (some? pk-value))
      (is (nil? deleted-car))))
  (testing "execution of getting entities command"
    (let [{result ::protocol/result}
          (api/execute-one
            (protocol/get-entities-command
              :cars
              ::protocol/limit 1
              ::protocol/offset 0
              ::protocol/filter [[:manufacturer 1]]
              ::protocol/order-by [[:id ::protocol/asc]]))
          ]
      (is (= 1 (count result)))
      (is (= "Car1" (-> result first :name))))))