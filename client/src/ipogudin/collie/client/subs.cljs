(ns ipogudin.collie.client.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :schema
  (fn [db]
    (:schema db)))

(re-frame/reg-sub
  :selected
  (fn [db]
    (:selected db)))