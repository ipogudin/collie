(ns ipogudin.collie.events
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.db :as db]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]))

(re-frame/reg-event-db
  :initialize-db
  (fn  [_ _]
    db/default-db))

(re-frame/reg-event-db
  :api-result
  (fn  [db [_ result]]
    (assoc db :value (:value result))))

(re-frame/reg-event-db
  :clear-value
  (fn  [db _]
    (assoc db :value "...")))

(re-frame/reg-event-fx
  :api-request
  (fn [db [_ body]]
    {:http-xhrio {:method          :post
                  :uri             "/api/some-method"
                  :params          body
                  :timeout         1000
                  :format          (ajax/edn-request-format)
                  :response-format (ajax/edn-response-format)
                  :on-success      [:api-result]
                  :on-failure      [:api-failure]}}))