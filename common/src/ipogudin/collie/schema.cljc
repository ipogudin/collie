(ns ipogudin.collie.schema
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [#?(:clj clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]
            [mount.core :refer [defstate]]
            [ipogudin.collie.entity :as entity]))

(s/def ::name entity/db-name-spec)
(s/def ::related-entity entity/db-name-spec)
(s/def ::related-entity-field entity/db-name-spec)
(s/def ::relation entity/db-name-spec)
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

(s/def ::field (s/multi-spec field-type-mm ::field-type))

(s/def ::fields (s/with-gen
  (s/and
    (s/coll-of ::field :kind vector? :min-count 1 :distinct true)
    (fn [col]
      (->
        (filter ::primary-key col)
        count
        (= 1))))
  #(gen/fmap
     (fn [fields]
       (into
         (filterv (fn [f] (-> f ::primary-key not)) fields)
         (let [field (gen/generate (s/gen ::field))]
           [(assoc field ::primary-key true)])))
     (gen/vector-distinct (s/gen ::field) {:min-elements 0 :max-elements 5}))))

(s/def ::entity (s/keys :req [::name ::fields]))

(s/def ::entities (s/coll-of ::entity :kind vector? :min-count 1 :distinct true))


(defn schema-seq-to-map
  [entities]
  (->> entities (mapv (fn [e ][(::name e) e])) (into {})))

(s/fdef schema-seq-to-map
        :args (s/cat :entities ::entities)
        :ret map?)

(defn find-primary-key
  [entity]
  (->> entity ::fields (filterv ::primary-key) first))

(s/fdef find-primary-key
        :args (s/cat :entity ::entity)
        :ret (s/or :found ::field :not-found nil?))

; mount states
(defstate schema :start #())