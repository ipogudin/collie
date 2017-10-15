(ns ipogudin.collie.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :name
  (fn [db]
    (:name db)))

(re-frame/reg-sub
  :value-to-display
  (fn [db]
    (:value db)))