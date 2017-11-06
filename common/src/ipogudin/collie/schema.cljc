(ns ipogudin.collie.schema
  (:require [clojure.spec.alpha :as s]))

(s/def ::name keyword?)
(s/def ::related-entity keyword?)
(s/def ::related-entity-field keyword?)
(s/def ::relation keyword?)
(s/def ::field-type #{::serial ::int ::decimal ::string ::one-to-one ::one-to-many ::many-to-many})
(s/def ::primary-key boolean?)
(s/def ::precision vector?)

(s/def ::common-field (s/keys :req [::name ::field-type] :opt [::primary-key]))

(defmulti field-type-mm ::field-type)
(defmethod field-type-mm ::serial [_]
  ::common-field)
(defmethod field-type-mm ::int [_]
  ::common-field)
(defmethod field-type-mm ::decimal [_]
  (s/merge ::common-field (s/keys :req [::precision])))
(defmethod field-type-mm ::string [_]
  ::common-field)
(defmethod field-type-mm ::one-to-one [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity] :opt [::related-entity-field])))
(defmethod field-type-mm ::one-to-many [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity] :opt [::related-entity-field])))
(defmethod field-type-mm ::many-to-many [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity] :opt [::relation ::related-entity-field])))

(s/def ::field (s/multi-spec field-type-mm :field-type))

(s/def ::fields (s/coll-of ::field :kind vector?))

(s/def ::entity (s/keys :req [::name ::fields]))

(s/def ::entities (s/coll-of ::entity :kind vector? :min-count 1 :distinct true))


(defn create-schema
  [entities]
  (->> entities (mapv (fn [e ][(::name e) e])) (into {})))

(s/fdef create-schema
        :args (s/cat :entities ::entities))

(defn find-primary-key
  [entity]
  (->> entity ::fields (filterv ::primary-key) first))

(s/fdef find-primary-key
        :args (s/cat :entity ::entity)
        :ret ::field)