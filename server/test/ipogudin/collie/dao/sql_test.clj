(ns ipogudin.collie.dao.sql-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.dao.sql :as sql]))

(def db
    {
     :classname   "org.h2.Driver"
     :subprotocol "h2:mem"
     :subname     "test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
     :user        "sa"
     :password    ""
     })

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
  (j/insert! t :cars {:name "Car3" :manufacturer 2}))

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

(def schema-map (schema/create-schema entities))

(deftest reading
  (testing "Getting by id"
    (let [e (sql/get-by-id db schema-map :cars 1)]
      (is (not-empty e))
      (is (= :cars (::entity/type e)))
      (is (nil? (sql/get-by-id db schema-map :cars (Integer/MAX_VALUE)))))))

(deftest writing
  (testing "Insertion of new entities"
    (let [car4 {:name "Car4" :manufacturer 2 ::entity/type :cars}
          id (sql/upsert db schema-map car4)]
      (is (= (merge {:id id} car4) (sql/get-by-id db schema-map :cars id)))))
  (testing "Update of existing entities"
    (let [car2 {:id 2 :name "Car2 (Updated)" :manufacturer 1 ::entity/type :cars}
          id (sql/upsert db schema-map car2)]
      (is (= car2 (sql/get-by-id db schema-map :cars id))))))

(deftest deletion
  (testing "Deletion of entities"
    (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars}
          id (sql/upsert db schema-map car5)
          persisted-car5 (sql/get-by-id db schema-map :cars id)]
      (sql/delete db schema-map :cars id)
      (is (nil? (sql/get-by-id db schema-map :cars id))))))