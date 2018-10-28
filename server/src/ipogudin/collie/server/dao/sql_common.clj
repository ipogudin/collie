(ns ipogudin.collie.server.dao.sql-common
  (:require [mount.core :refer [defstate]]))

(defstate insert! :start #())
(defstate update! :start #())
(defstate delete! :start #())
