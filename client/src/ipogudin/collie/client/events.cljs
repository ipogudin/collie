(ns ipogudin.collie.client.events
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.client.db :as db]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.schema :refer [schema]]
            [ipogudin.collie.protocol :as p]))

(defn extract-1st-result
  [db db-path {status ::p/status [{result ::p/result}] ::p/results}]
  (if (= ::p/ok status)
    (assoc-in db db-path result)
    (assoc db :error {:message "Something goes wrong"})))

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

(re-frame/reg-event-fx
  :api-request
  (fn [{:keys [db]} [_ body]]
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
    {:db (deep-merge db {:selected {
                                    :filled false
                                    :entities nil
                                    :type type
                                    }})
     :http-xhrio  {:method          :post
                   :uri             "/api/"
                   :params          (p/request [(p/get-entities-command type ::p/resolved-dependencies true)])
                   :timeout         1000
                   :format          (ajax/edn-request-format)
                   :response-format (ajax/edn-response-format)
                   :on-success      [:set-selected-entities]
                   :on-failure      [:api-failure]}}))

(re-frame/reg-event-db
  :set-selected-entities
  (fn  [db [_ response]]
    (assoc-in
      (extract-1st-result db [:selected :entities] response)
      [:selected :filled]
      true)))