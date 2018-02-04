(ns ipogudin.collie.client.events
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.client.db :as db]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.schema :refer [schema]]
            [ipogudin.collie.protocol :as p]))

(re-frame/reg-event-db
  :initialize-db
  (fn  [_ _]
    (deep-merge
      db/default-db
      {:schema @schema})))

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
                  :uri             "/api/"
                  :params          body
                  :timeout         1000
                  :format          (ajax/edn-request-format)
                  :response-format (ajax/edn-response-format)
                  :on-success      [:api-result]
                  :on-failure      [:api-failure]}}))

(re-frame/reg-event-fx
  :select-entity-type
  (fn  [{:keys [db]} [_ type]]
    {:db (assoc db :opened-entities nil)
     :http-xhrio  {:method          :post
                   :uri             "/api/"
                   :params          (p/request [(p/get-entities-command type)])
                   :timeout         1000
                   :format          (ajax/edn-request-format)
                   :response-format (ajax/edn-response-format)
                   :on-success      [:show-entities]
                   :on-failure      [:api-failure]}}))

(re-frame/reg-event-db
  :show-entities
  (fn  [db [_ r]]
    (println db)
    (assoc db :opened-entities r)))