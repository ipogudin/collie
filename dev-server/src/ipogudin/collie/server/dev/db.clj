(ns ipogudin.collie.server.dev.db
  (:require [clojure.java.jdbc :as j]
            [ipogudin.collie.server.dao.common :as common-dao]))

(def ddl
  {:currency
   [[:id "serial"]
    [:name "varchar(256)"]]
   :manufacturers
   [[:id "serial"]
    [:name "varchar(256)"]]
   :engine_types
   [[:id "serial"]
    [:code "varchar(256)"]
    [:name "varchar(256)"]]
   :cars
   [[:id "serial"]
    [:visible "boolean"]
    [:name "varchar(256)"]
    [:model "varchar(256)"]
    [:description "varchar(2048)"]
    [:manufacturer "integer REFERENCES manufacturers (id)"]
    [:engine_type "varchar(256)"]
    [:price "numeric(10, 2)"]
    [:drive_wheels "integer NOT NULL"]
    [:width "integer NOT NULL"]
    [:length "integer NOT NULL"]
    [:height "integer NOT NULL"]
    [:transmission_speed "integer NOT NULL"]
    [:cylinders "integer NOT NULL"]
    [:min_kerb_weight "integer NOT NULL"]
    [:max_kerb_weight "integer NOT NULL"]
    [:gross_weight_limit "integer NOT NULL"]]
   :showrooms
   [[:id "serial"]
    [:name "varchar(256)"]]
   :showrooms_to_cars
   [[:car "integer NOT NULL REFERENCES cars (id) ON DELETE CASCADE"]
    [:showroom "integer NOT NULL REFERENCES showrooms (id) ON DELETE CASCADE"]]
   })

(def entities
  {
   :currency
   [
    {:name "Dollar"}
    {:name "Euro"}
    ]
   :manufacturers
   [
    {:name "Factory1"}
    {:name "Factory2"}
    {:name "Factory3"}
    ]
   :engine_types
   [
    {:code "gasoline" :name "Gasoline"}
    {:code "diesel" :name "Diesel"}
    ]
   :cars
   [
    {:visible true :name "Car1" :model "Model A" :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
     :manufacturer 1 :engine_type "gasoline" :price 19200.99M :drive_wheels 2
     :width 1750 :length 4350 :height 1560 :transmission_speed 6 :cylinders 4
     :min_kerb_weight 1250 :max_kerb_weight 1450 :gross_weight_limit 1590}
    {:visible true :name "Car2" :model "Model A" :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
     :manufacturer 1 :engine_type "diesel" :price 15200.30M :drive_wheels 2
     :width 1850 :length 4450 :height 1530 :transmission_speed 6 :cylinders 4
     :min_kerb_weight 1290 :max_kerb_weight 1550 :gross_weight_limit 1690}
    {:visible true :name "Car3" :model "Model A" :description "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
     :manufacturer 2 :engine_type "gasoline" :price 29200M :drive_wheels 4
     :width 1980 :length 4730 :height 1690 :transmission_speed 7 :cylinders 6
     :min_kerb_weight 1450 :max_kerb_weight 1580 :gross_weight_limit 1930}
    ]
   :showrooms
   [
    {:name "Showroom1"}
    {:name "Showroom2"}
    {:name "Showroom3"}
    ]
   :showrooms_to_cars
   [
    {:car 1 :showroom 1}
    {:car 1 :showroom 2}
    {:car 2 :showroom 2}
    {:car 3 :showroom 2}
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