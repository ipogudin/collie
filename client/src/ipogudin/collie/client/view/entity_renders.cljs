(ns ipogudin.collie.client.view.entity-renders
  (:require [ipogudin.collie.schema :as schema]))

(defn render-name
  [{name ::schema/name ui ::schema/ui}]
  (if ui
    (::schema/title ui)
    name))

(defn render-header
  [type]
  (let [s (get @schema/schema type)]
    (into
      [:tr]
      (map
        (comp (fn [n] [:th n]) render-name)
        (::schema/fields s)))))

(defn render-row
  [type entity]
  (let [s (get @schema/schema type)]
    (into
      [:tr {:key (schema/find-primary-key-value s entity)}]
      (map
        (fn [field] [:td (get entity (::schema/name field))])
        (::schema/fields s)))))