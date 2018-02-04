(ns ipogudin.collie.server.api
  (:require [mount.core :refer [defstate]]
            [com.rpl.specter :refer [transform select ALL]]
            [clojure.tools.logging :as log]
            [ipogudin.collie.common :as common]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.server.dao.common :as common-dao]))

(defmulti execute-one ::protocol/code)

(defmethod execute-one
  ::protocol/get-by-pk
  [{id ::protocol/id
    type ::entity/type
    pk-value ::protocol/pk-value}]
  (protocol/result id (common-dao/get-by-pk common-dao/db schema/schema type pk-value)))

(defmethod execute-one
  ::protocol/upsert
  [{id ::protocol/id
    entity ::entity/entity}]
  (protocol/result id (common-dao/upsert common-dao/db schema/schema entity)))

(defmethod execute-one
  ::protocol/delete
  [{id ::protocol/id
    pk-value ::protocol/pk-value
    type ::entity/type}]
  (protocol/result id (common-dao/delete common-dao/db schema/schema type pk-value)))

(defmethod execute-one
  ::protocol/get-entities
  [{id ::protocol/id
    type ::entity/type
    options ::protocol/options}]
  (let [options-vec
        (->>
          options
          vec
          (select
            [ALL ALL])
          (transform
            common/ALL-OBJECTS
            common/remove-ns-from-keyword))]
    (protocol/result
      id
      (apply
        common-dao/get-entities
        (into
          [common-dao/db schema/schema type]
          options-vec)))))

(defn execute
  [commands]
  (let [a 1]
    (mapv
      execute-one
      commands)))

(defn handle
  [request]
  (let [{id ::protocol/id} request]
    (log/trace "API received a request: " request)
    (protocol/response
      id
      ::protocol/ok
      (execute (::protocol/commands request)))))