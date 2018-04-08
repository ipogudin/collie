(ns ipogudin.collie.client.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :schema
  (fn [db]
    (:schema db)))

(re-frame/reg-sub
  :selecting
  (fn [db]
    (:selecting db)))

(re-frame/reg-sub
  :editing
  (fn [db]
    (:editing db)))