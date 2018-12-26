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
(s/def ::left entity/db-name-spec)
(s/def ::right entity/db-name-spec)
(s/def ::field-type #{::serial ::boolean ::int ::decimal ::string ::date ::timestamp
                      ::one-to-one ::one-to-many ::many-to-many})
(s/def ::primary-key boolean?)
(s/def ::precision int?)
(s/def ::scale int?)
(s/def ::show-fn (s/with-gen fn? #(gen/fmap (fn [s] (fn [_] s)) (gen/string-alphanumeric))))

(s/def ::ts-format string?)
(s/def ::tz-disabled boolean?)

(s/def ::max-length int?)

(s/def ::title string?)
(s/def ::hidden boolean?)
(s/def ::editable boolean?)
(s/def ::nullable boolean?)
(s/def ::default any?)
(s/def ::preview-text-length int?)
(s/def ::selector-size int?)
(s/def ::default-order #{:asc :desc})
(s/def ::ui (s/keys :req [::title]
                    :opt [::primary-key
                          ::show-fn
                          ::hidden
                          ::editable
                          ::preview-text-length
                          ::selector-size]))

(s/def ::common-field (s/keys :req [::name ::field-type] :opt [::primary-key ::ui ::nullable ::default ::default-order]))

(defmulti field-type-mm ::field-type)
(defmethod field-type-mm ::serial [_]
  ::common-field)
(defmethod field-type-mm ::boolean [_]
  ::common-field)
(defmethod field-type-mm ::int [_]
  ::common-field)
(defmethod field-type-mm ::decimal [_]
  (s/merge ::common-field (s/keys :req [::precision ::scale])))
(defmethod field-type-mm ::string [_]
  (s/merge ::common-field (s/keys :opt [::max-length])))
(defmethod field-type-mm ::date [_]
  (s/merge ::common-field (s/keys :req [::ts-format])))
(defmethod field-type-mm ::timestamp [_]
  (s/merge ::common-field (s/keys :req [::ts-format ::tz-disabled])))
(defmethod field-type-mm ::one-to-one [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity ::related-entity-field])))
(defmethod field-type-mm ::one-to-many [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity ::related-entity-field])))
(defmethod field-type-mm ::many-to-many [_]
  (s/merge ::common-field
           (s/keys :req [::related-entity ::relation ::left ::right ::related-entity-field])))

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

(s/def ::entity (s/keys :req [::name ::fields] :opt [::ui]))

(s/def ::entities (s/coll-of ::entity :kind vector? :min-count 1 :distinct true))


(defn schema-seq-to-map
  [entities]
  (->> entities (mapv (fn [e ][(::name e) e])) (into {})))

(s/fdef schema-seq-to-map
        :args (s/cat :entities ::entities)
        :ret map?)

(defn find-primary-key
  [entity-schema]
  (->> entity-schema ::fields (filterv ::primary-key) first))

(s/fdef find-primary-key
        :args (s/cat :entity ::entity)
        :ret (s/or :found ::field :not-found nil?))

(defn find-field-schema
  [entity-schema field-name]
  (->>
    entity-schema
    ::fields
    (filter #(= field-name (::name %)))
    first))

(defn get-value-by-field-schema
  "Returns a value from an entity according to a field schema."
  [field-schema entity-value]
  (->> field-schema ::name (get entity-value)))

(defn relation?
  "Checks whether a field is a relation of one of supported types."
  [{type ::field-type :as field}]
  (some (partial = type) [::one-to-one ::one-to-many ::many-to-many]))

(defn multi-relation?
  "Checks whether a field is a multi relation."
  [{type ::field-type :as field}]
  (some (partial = type) [::one-to-many ::many-to-many]))

; mount states
(defstate schema :start #())