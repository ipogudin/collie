(ns ipogudin.collie.client.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :name
  (fn [db]
    (:name db)))

(re-frame/reg-sub
  :schema
  (fn [db]
    (:schema db)))

(re-frame/reg-sub
  :value-to-display
  (fn [db]
    (:value db)))

(re-frame/reg-sub
  :opened-entities
  (fn [db]
    (:opened-entities db)))