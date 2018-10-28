(ns ipogudin.collie.server.api
  (:require [mount.core :refer [defstate]]
            [com.rpl.specter :refer [transform select ALL]]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [ipogudin.collie.common :as common]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.server.dao.common :as common-dao]))

(defmulti execute-one (fn [db command] (::protocol/code command)))

(defmethod execute-one
  ::protocol/get-by-pk
  [db
   {id ::protocol/id
    type ::entity/type
    pk-value ::protocol/pk-value}]
  (protocol/result id (common-dao/get-by-pk db schema/schema type pk-value)))

(defmethod execute-one
  ::protocol/upsert
  [db
   {id ::protocol/id
    entity ::entity/entity}]
  (protocol/result id (common-dao/upsert db schema/schema entity)))

(defmethod execute-one
  ::protocol/delete
  [db
   {id ::protocol/id
    pk-value ::protocol/pk-value
    type ::entity/type}]
  (protocol/result id (common-dao/delete db schema/schema type pk-value)))

(defmethod execute-one
  ::protocol/get-entities
  [db
   {id ::protocol/id
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
          [db schema/schema type]
          options-vec)))))

(defn execute
  [commands]
  (j/with-db-transaction [t common-dao/db]
    (mapv
      (partial execute-one t)
      commands)))

(defn handle
  [request]
  (let [{id ::protocol/id} request]
    (log/trace "API received a request: " request)
    (protocol/response
      id
      ::protocol/ok
      (execute (::protocol/commands request)))))