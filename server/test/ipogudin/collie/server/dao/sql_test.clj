(ns ipogudin.collie.server.dao.sql-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as j]
            [mount.core :as mount]
            [ipogudin.collie.server.dao.h2-sql-common :as h2-sql-common]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.server.dao.sql :as sql]))

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
     :engine_types
     [[:id "serial"]
      [:code "varchar(256)"]
      [:name "varchar(256)"]])
   (j/create-table-ddl
     :cars
     [[:id "serial"]
      [:name "varchar(256)"]
      [:manufacturer "integer NOT NULL REFERENCES manufacturers (id)"]
      [:engine_type "varchar(256)"]])
   (j/create-table-ddl
     :showrooms
     [[:id "serial"]
      [:name "varchar(256)"]])
   (j/create-table-ddl
     :showrooms_to_cars
     [[:car "integer NOT NULL REFERENCES cars (id)"]
      [:showroom "integer NOT NULL REFERENCES showrooms (id)"]])
   (j/create-table-ddl
     :prices
     [[:id "serial"]
      [:car "integer REFERENCES cars (id)"]
      [:price "decimal(10,2)"]
      [:dealer "varchar(256)"]])])

(j/with-db-transaction
  [t db]
  (j/insert! t :manufacturers {:name "Factory1"})
  (j/insert! t :manufacturers {:name "Factory2"})
  (j/insert! t :manufacturers {:name "Factory3"})
  (j/insert! t :engine_types {:code "gasoline" :name "Gasoline"})
  (j/insert! t :engine_types {:code "diesel" :name "Diesel"})
  (j/insert! t :cars {:name "Car1" :manufacturer 1 :engine_type "gasoline"})
  (j/insert! t :cars {:name "Car2" :manufacturer 1 :engine_type "diesel"})
  (j/insert! t :cars {:name "Car3" :manufacturer 2 :engine_type "gasoline"})
  (j/insert! t :showrooms {:name "Showroom1"})
  (j/insert! t :showrooms {:name "Showroom2"})
  (j/insert! t :showrooms {:name "Showroom3"})
  (j/insert! t :showrooms_to_cars {:car 1 :showroom 1})
  (j/insert! t :showrooms_to_cars {:car 1 :showroom 2})
  (j/insert! t :showrooms_to_cars {:car 2 :showroom 2})
  (j/insert! t :showrooms_to_cars {:car 3 :showroom 2})
  (j/insert! t :prices {:car 1 :price 12500.00 :dealer "Car trader"})
  (j/insert! t :prices {:car 1 :price 12459.00 :dealer "FFF"})
  (j/insert! t :prices {:car 2 :price 17200.00 :dealer "Car trader"})
  (j/insert! t :prices {:car 3 :price 16900.00 :dealer "FFF"}))

(def entities
  [
   {::schema/name :manufacturers
    ::schema/fields
                 [
                  {::schema/name :id
                   ::schema/field-type ::schema/serial
                   ::schema/primary-key true}
                  {::schema/name :name
                   ::schema/field-type ::schema/string}
                  {::schema/name :cars
                   ::schema/field-type ::schema/one-to-many
                   ::schema/related-entity :cars
                   ::schema/related-entity-field :manufacturer}]}
   {::schema/name :engine_types
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :code
                    ::schema/field-type ::schema/string}
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
                   ::schema/field-type ::schema/one-to-one
                   ::schema/related-entity :manufacturers}
                  {::schema/name :engine_type
                   ::schema/field-type ::schema/one-to-one
                   ::schema/related-entity :engine_types
                   ::schema/related-entity-field :code}
                  {::schema/name :showrooms
                   ::schema/field-type ::schema/many-to-many
                   ::schema/related-entity :showrooms
                   ::schema/related-entity-field :id
                   ::schema/relation :showrooms_to_cars
                   ::schema/left :car
                   ::schema/right :showroom}
                  {::schema/name :prices
                   ::schema/field-type ::schema/one-to-many
                   ::schema/related-entity :prices
                   ::schema/related-entity-field :car}]}
   {::schema/name :showrooms
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}]}
   {::schema/name :prices
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :car
                    ::schema/field-type ::schema/int}
                   {::schema/name :price
                    ::schema/field-type ::schema/decimal
                    ::schema/precision 10
                    ::schema/scale 2}
                   {::schema/name :dealer
                    ::schema/field-type ::schema/string}]}
   ])

(def schema-map (schema/schema-seq-to-map entities))

(defn fixture
  [f]
  (->
    (mount/only #{#'ipogudin.collie.server.dao.sql-common/insert!
                  #'ipogudin.collie.server.dao.sql-common/update!
                  #'ipogudin.collie.server.dao.sql-common/delete!})
    h2-sql-common/setup-h2
    mount/start)
  (try
    (f)
    (finally (mount/stop))))

(use-fixtures :once fixture)

(deftest getting-by-pk
  (testing "Getting by primary key"
    (let [car (sql/get-by-pk db schema-map :cars 1)]
      (is (not-empty car))
      (is (= :cars (::entity/type car)))
      (is (= "Car1" (:name car)))
      (is (nil? (sql/get-by-pk db schema-map :cars (Integer/MAX_VALUE)))))))

(deftest upserting
  (testing "upserting"
    (testing "Insertion of new entities"
      (let [car4 {:name "Car4" :manufacturer 2 ::entity/type :cars :engine_type "gasoline"}
            pk-value (sql/upsert db schema-map car4)]
        (is (= (merge {:id pk-value} car4) (sql/get-by-pk db schema-map :cars pk-value)))))
    (testing "Update of existing entities"
      (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars :engine_type "diesel"}
            id (sql/upsert db schema-map car5)
            car5-updated {:id id :name "Car5" :manufacturer 2 ::entity/type :cars :engine_type "gasoline"}]
        (sql/upsert db schema-map car5-updated)
        (is (= car5-updated (sql/get-by-pk db schema-map :cars id)))))
    (testing "Insertion of new entities with dependencies"
      (let [manufacturer-pk 2
            showroom-pk 1
            prices [{::entity/type :prices :car nil :price 13499.00M :dealer "dealer1"}
                    {::entity/type :prices :car nil :price 13799.00M :dealer "dealer2"}]
            showrooms [(sql/get-by-pk db schema-map :showrooms showroom-pk)]
            manufacturer [(sql/get-by-pk db schema-map :manufacturers manufacturer-pk)]
            car4 {:name "Car4" :manufacturer manufacturer-pk ::entity/type :cars :engine_type "gasoline"
                  :deps {:prices prices
                         :showrooms showrooms
                         :manufacturer manufacturer}}
            pk-value (sql/upsert db schema-map car4)
            result-prices (sql/get-entities
                             db
                             schema-map
                             :prices
                             :filter [[:car pk-value]])
            result-showroom-relations (j/query db ["select car, showroom from showrooms_to_cars where car = ?" pk-value])]
        (is (= (merge {:id pk-value} (dissoc car4 :deps)) (sql/get-by-pk db schema-map :cars pk-value)))
        (is (= #{13499.00M 13799.00M} (into #{} (map :price result-prices))))
        (is (= showroom-pk (->>
                             result-showroom-relations
                             (map :showroom)
                             first)))))
    (testing "Update of existing entities with dependencies"
      (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars :engine_type "diesel"
                  :deps {:prices [{::entity/type :prices :car nil :price 13899.00M :dealer "dealer0"}]}}
            showroom-pk 2
            showrooms [(sql/get-by-pk db schema-map :showrooms showroom-pk)]
            pk-value (sql/upsert db schema-map car5)
            prices [{::entity/type :prices :car pk-value :price 13499.00M :dealer "dealer1"}
                    {::entity/type :prices :car pk-value :price 13799.00M :dealer "dealer2"}]
            car5-updated {:id pk-value :name "Car5" :manufacturer 2 ::entity/type :cars :engine_type "gasoline"
                          :deps {:prices prices
                                 :showrooms showrooms}}
            _ (sql/upsert db schema-map car5-updated)
            result-prices (sql/get-entities
                            db
                            schema-map
                            :prices
                            :filter [[:car pk-value]])
            result-showroom-relations (j/query db ["select car, showroom from showrooms_to_cars where car = ?" pk-value])]
        (is (= (dissoc car5-updated :deps) (sql/get-by-pk db schema-map :cars pk-value)))
        (is (= #{13499.00M 13799.00M} (into #{} (map :price result-prices))))
        (is (= showroom-pk (->>
                             result-showroom-relations
                             (map :showroom)
                             first)))))))

(deftest deletion
  (testing "Deletion of entities"
    (let [car5 {:name "Car5" :manufacturer 2 ::entity/type :cars}
          pk-value (sql/upsert db schema-map car5)
          persisted-car5 (sql/get-by-pk db schema-map :cars pk-value)
          result1 (sql/delete db schema-map :cars pk-value)
          result2 (sql/delete db schema-map :cars Integer/MAX_VALUE)]
      (is (= 1 result1))
      (is (nil? (sql/get-by-pk db schema-map :cars pk-value)))
      (is (= 0 result2)))))

(deftest getting-entities
  (testing "getting entities"
    (testing "Getting entities with default options"
      (let [cars-vec1 (sql/get-entities db schema-map :cars)]
        (is (not (empty? cars-vec1)))))
    (testing "Getting entities with order-by, limit and offset options"
      (let [[c1 c2 c3 & others :as cars-vec2]
            (sql/get-entities
              db
              schema-map
              :cars
              :order-by [[:id :asc]]
              :limit 3
              :offset 0)]
        (is (empty? others))
        (is (every? (partial = :cars) (mapv ::entity/type cars-vec2)))
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
    (testing "Getting entities with filter, limit and offset options"
      (let [[c1 & others]
            (sql/get-entities
              db
              schema-map
              :cars
              :filter [[:name "Car1"]]
              :limit 1
              :offset 0)]
        (is (empty? others))
        (is (= "Car1" (:name c1)))))
    (testing "Getting entities with dependencies."
      (let [[{{:keys [showrooms manufacturer] :as deps} :deps :as c1} & _]
            (sql/get-entities
              db
              schema-map
              :cars
              :filter [[:name "Car1"]]
              :limit 1
              :offset 0
              :resolved-dependencies true)]
        (is (= "Car1" (:name c1)))
        (is (= 4 (-> deps keys count)))
        (is (= "Factory1" (:name manufacturer)))
        (is (= 2 (count showrooms))))
      (let [[{{:keys [cars] :as deps} :deps :as m1} & _]
            (sql/get-entities
              db
              schema-map
              :manufacturers
              :filter [[:name "Factory1"]]
              :limit 1
              :offset 0
              :resolved-dependencies true)]
        (is (= "Factory1" (:name m1)))
        (is (= 1 (-> deps keys count)))
        (is (= 2 (count cars)))
        (is (= ["Car1" "Car2"] (mapv :name cars)))))))