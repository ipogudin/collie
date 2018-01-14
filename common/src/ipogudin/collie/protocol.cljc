(ns ipogudin.collie.protocol
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [ipogudin.collie.common :as common]
            [ipogudin.collie.entity :as entity]))

(declare gen-id)

;the beginning of public API
(defn- command
  [code]
  {::id (gen-id)
   ::code code})

(defn get-by-pk-command
  [type pk]
  (common/deep-merge
    (command ::get-by-pk)
    {::entity/type type
     ::pk-value pk}))

(defn upsert-command
  [entity]
  (common/deep-merge (command ::upsert) {::entity/entity entity}))

(defn delete-command
  [type  pk]
  (common/deep-merge
    (command ::delete)
    {::entity/type type
     ::pk-value pk}))

(defn get-entities-command
  [type & opts]
  (common/deep-merge
    (command ::get-entities)
    {::entity/type type}
    (if (empty? opts) {} (apply hash-map opts))))

(defn request
  ([] (request []))
  ([commands]
    {::id (gen-id)
     ::commands (vec commands)}))

(defn add-commands
  [request & commands]
    (common/deep-merge
      request
      {::commands (vec commands)}))

(defn add-command
  [request command]
  (add-commands request [command]))

(defn response
  ([id] (response id ::ok []))
  ([id status results]
   {::id id
    ::status status
    ::results (vec results)}))

(defn result
  [id result]
  {::id id
   ::result result})
;the end of public API

(def id-regex #"^[a-z0-9]{8}\-[a-z0-9]{4}\-[a-z0-9]{4}\-[a-z0-9]{4}\-[a-z0-9]{12}$")
(s/def ::id (s/and string? #(re-matches id-regex %)))
(s/def ::code #{::get-by-pk ::upsert ::delete ::get-entities})

(s/def ::command-common-fields (s/keys :req [::id ::code]))

(s/def ::pk-value (s/or :number int? :string string?))
(s/def ::value (s/or :number number? :string string?))

(s/def ::order #{::asc ::desc})
(s/def ::order-element (s/cat :column entity/db-name-spec :order ::order))
(s/def ::order-by (s/coll-of ::order-element :kind vector? :min-count 1 :distinct true))
(s/def ::filter-element (s/cat :column entity/db-name-spec :value ::value))
(s/def ::filter (s/coll-of ::filter-element :kind vector? :min-count 1 :distinct true))

(s/def ::limit (s/and int? #(>= % 0)))
(s/def ::offset (s/and int? #(>= % 0)))

(defmulti command-mm ::code)
(defmethod command-mm ::get-by-pk [_]
  (s/merge ::command-common-fields (s/keys :req [::entity/type ::pk-value])))
(defmethod command-mm ::upsert [_]
  (s/merge ::command-common-fields (s/keys :req [::entity/entity])))
(defmethod command-mm ::delete [_]
  (s/merge ::command-common-fields (s/keys :req [::entity/type ::pk-value])))
(defmethod command-mm ::get-entities [_]
  (s/merge ::command-common-fields
           (s/keys :req [::entity/type]
                   :opt [::order-by
                         ::filter
                         ::limit
                         ::offset])))

(s/def ::command (s/multi-spec command-mm ::code))

(s/def ::commands (s/coll-of ::command :kind vector? :min-count 1 :distinct true))

(s/def ::request (s/keys :req [::id ::commands]))

(s/def ::result (s/or :map map? :coll (s/coll-of map? :kind vector? :min-count 1 :distinct true)))
(s/def ::results (s/coll-of ::result :kind vector? :min-count 1 :distinct true))
(s/def ::status #{::ok ::error})

(s/def ::response (s/keys :req [::id ::status ::results]))

(defn gen-id []
  (str
    #?(:clj (java.util.UUID/randomUUID)
       :cljs (random-uuid))))

(s/fdef gen-id
        :args empty?
        :ret ::id)