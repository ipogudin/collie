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

(def schema-map (schema/schema-seq-to-map entities))

(deftest getting-by-id
  (testing "Getting by id"
    (let [car (sql/get-by-pk db schema-map :cars 1)]
      (is (not-empty car))
      (is (= :cars (::entity/type car)))
      (is (= "Car1" (:name car)))
      (is (nil? (sql/get-by-pk db schema-map :cars (Integer/MAX_VALUE)))))))

(deftest upserting
  (testing "Insertion of new entities"
    (let [car4 {:name "Car4" :manufacturer 2 ::entity/type :cars}
          pk-value (sql/upsert db schema-map car4)]
      (is (= (merge {:id pk-value} car4) (sql/get-by-pk db schema-map :cars pk-value))))))
  (testing "Update of existing entities"
    (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars}
          id (sql/upsert db schema-map car5)
          car5-updated {:id id :name "Car5" :manufacturer 2 ::entity/type :cars}]
      (is (= car5-updated (sql/get-by-pk db schema-map :cars id)))))

(deftest deletion
  (testing "Deletion of entities"
    (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars}
          pk-value (sql/upsert db schema-map car5)
          persisted-car5 (sql/get-by-pk db schema-map :cars pk-value)]
      (sql/delete db schema-map :cars pk-value)
      (is (nil? (sql/get-by-pk db schema-map :cars pk-value))))))

(deftest getting-entities
  (testing "Getting entities with default options"
    (let [cars-vec1 (sql/get-entities db schema-map :cars)]
      (is (not (empty? cars-vec1)))))
  (testing "Getting entities with order-by limit and offset options"
    (let [[c1 c2 c3 & others :as cars-vec2]
          (sql/get-entities
            db
            schema-map
            :cars
            :order-by [[:id :asc]]
            :limit 3
            :offset 0)]
      (is (empty? others))
      (is (= ["Car1" "Car2" "Car3"] (mapv :name cars-vec2))))
    (let [[m1 m2 & others :as manufacturers-vec1]
          (sql/get-entities
            db
            schema-map
            :manufacturers
            :order-by [[:name :desc] [:id :asc]]
            :limit 2)]
      (is (empty? others))
      (is (= ["Factory3" "Factory2"] (mapv :name manufacturers-vec1)))))
  (testing "Getting entities with filter limit and offset options"
    (let [[c1 & others]
          (sql/get-entities
            db
            schema-map
            :cars
            :filter [[:name "Car1"]]
            :limit 1
            :offset 0)]
      (is (empty? others))
      (is (= "Car1" (:name c1))))))