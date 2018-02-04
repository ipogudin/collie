(ns ipogudin.collie.server.dev.db
  (:require [clojure.java.jdbc :as j]
            [ipogudin.collie.server.dao.common :as common-dao]))

(def ddl
  {
   :manufacturers
   [[:id "serial"]
    [:name "varchar(256)"]]
   :cars
   [[:id "serial"]
    [:name "varchar(256)"]
    [:manufacturer "integer NOT NULL REFERENCES manufacturers (id)"]]
   :showrooms
   [[:id "serial"]
    [:name "varchar(256)"]]
   :showrooms_to_cars
   [[:car "integer NOT NULL REFERENCES cars (id)"]
    [:showroom "integer NOT NULL REFERENCES showrooms (id)"]]
   })

(def entities
  {
   :manufacturers
   [
    {:name "Factory1"}
    {:name "Factory2"}
    {:name "Factory3"}
    ]
   :cars
   [
    {:name "Car1" :manufacturer 1}
    {:name "Car2" :manufacturer 1}
    {:name "Car3" :manufacturer 2}
    ]
   })

(defn setup-db
  []
  (let [db common-dao/db]
    (j/with-db-transaction
      [t db]
      (doseq [[type structure] ddl]
        (j/db-do-commands t
          [(j/create-table-ddl type structure)])))
    (j/with-db-transaction
      [t db]
      (doseq [[type rows] entities]
        (doseq [row rows]
          (j/insert! t type row))))))