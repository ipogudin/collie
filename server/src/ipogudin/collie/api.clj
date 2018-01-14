(ns ipogudin.collie.api
  (:require [mount.core :refer [defstate]]
            [ipogudin.collie.entity :as entity]
            [ipogudin.collie.protocol :as protocol]
            [ipogudin.collie.dao.common :as common-dao]))

{::protocol/get-by-pk {:f common-dao/get-by-pk}
 ::protocol/upsert {:f common-dao/upsert}
 ::protocol/delete {:f common-dao/delete}}

(defstate schema :start #())

(defmulti execute-one ::protocol/code)
(defmethod execute-one
  ::protocol/get-by-pk
  [{id ::protocol/id
    type ::entity/type
    pk-value ::protocol/pk-value}]
  (protocol/result id (common-dao/get-by-pk common-dao/db schema type pk-value)))

(defn execute
  [commands]
  (let [a 1]
    (mapv
      execute-one
      commands)))

(defn handle
  [request]
  (let [{id ::protocol/id} request]
    (protocol/response
      id
      ::protocol/ok
      (execute (:commands request)))))