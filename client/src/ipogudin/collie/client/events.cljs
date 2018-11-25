(ns ipogudin.collie.client.events
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.client.db :as db]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [ipogudin.collie.common :refer [deep-merge]]
            [ipogudin.collie.schema :refer [schema]]
            [ipogudin.collie.protocol :as p]
            [ipogudin.collie.entity :as e]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.client.api.entity :as entity-api]
            [ipogudin.collie.entity-helpers :as entity-helpers]
            [ipogudin.collie.schema :as schema]
            [ipogudin.collie.client.configuration :as configuration]))

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

(re-frame/reg-event-db
  :api-failure
  (fn  [db [_ result]]
    (assoc db :error {:message "Something goes wrong"})))

(re-frame/reg-event-fx
  :select-entity
  (fn  [{:keys [db]} [_ type]]
    {:db (deep-merge db {:selecting {
                                    :status :unsync
                                    :entities nil
                                    :type type
                                    }})
     :http-xhrio  {:method          :post
                   :uri             (:api-root @configuration/configuration)
                   :params          (p/request [(p/get-entities-command type ::p/resolved-dependencies true)])
                   :timeout         30000
                   :format          (ajax/edn-request-format)
                   :response-format (ajax/edn-response-format)
                   :on-success      [:set-selected-entities]
                   :on-failure      [:api-failure]}}))

(re-frame/reg-event-db
  :set-selected-entities
  (fn  [db [_ response]]
    (assoc-in
      (extract-1st-result db [:selecting :entities] response)
      [:selecting :status]
      :sync)))

(re-frame/reg-event-fx
  :edit-entity
  (fn  [{:keys [db]} [_ entity]]
    (if entity
      (let [schema (:schema db)
            entity-schema (->> entity ::e/type (get schema))
            field-names-and-commands (entity-api/options-for-dependencies entity-schema)
            command-id-to-dep-field (into
                                    {}
                                    (mapv
                                      (fn [{n :name c :command}]
                                        [(::p/id c) n])
                                      field-names-and-commands))
            commands (mapv :command field-names-and-commands)]
        {:db (assoc
                db
                :editing
                {:entity entity
                 :command-id-to-dep-field command-id-to-dep-field
                 :status :unsync})
         :http-xhrio  {:method          :post
                       :uri             (:api-root @configuration/configuration)
                       :params          (p/request commands)
                       :timeout         30000
                       :format          (ajax/edn-request-format)
                       :response-format (ajax/edn-response-format)
                       :on-success      [:set-dependencies-for-entity]
                       :on-failure      [:api-failure]}})
        {:db (dissoc db :editing)})))

(re-frame/reg-event-db
  :set-dependencies-for-entity
  (fn  [db [_ response]]
    (let [{status ::p/status results ::p/results} response
          editing (:editing db)
          command-id-to-dep-field (:command-id-to-dep-field editing)]
      (if (= ::p/ok status)
        (assoc
          db
          :editing
          (->
            editing
            (dissoc :command-id-to-dep-field)
            (assoc :status :sync)
            (assoc :dep-options
                   (into
                     {}
                     (mapv
                       (fn [{id ::p/id result ::p/result}]
                         [(get command-id-to-dep-field id) result])
                       results)))))
        (assoc db :error {:message "Something goes wrong"})))))

(re-frame/reg-event-db
  :change-entity-field
  (fn  [db [_ field-schema field-value]]
    (let [s (:schema db)
          field-name (::schema/name field-schema)
          entity-value (get-in db [:editing :entity])
          updated-entity (entity-helpers/set-field-value s field-schema entity-value field-value)]
      (assoc-in
        db
        [:editing :entity]
        updated-entity))))

(re-frame/reg-event-fx
  :save-entity
  (fn  [{:keys [db]} [_ entity]]
    {:db db
     :http-xhrio  {:method          :post
                   :uri             (:api-root @configuration/configuration)
                   :params          (p/request [(p/upsert-command entity)])
                   :timeout         30000
                   :format          (ajax/edn-request-format)
                   :response-format (ajax/edn-response-format)
                   :on-success      [:complete-editing]
                   :on-failure      [:api-failure]}}))

(re-frame/reg-event-fx
  :delete-entity
  (fn  [{:keys [db]} [_ entity-value]]
    (let [s (:schema db)
          t (e/get-entity-type entity-value)
          pk (entity-helpers/find-primary-key-value
               (entity-helpers/find-entity-schema s entity-value)
               entity-value)]
      {:db db
       :http-xhrio  {:method          :post
                     :uri             (:api-root @configuration/configuration)
                     :params          (p/request [(p/delete-command t pk)])
                     :timeout         30000
                     :format          (ajax/edn-request-format)
                     :response-format (ajax/edn-response-format)
                     :on-success      [:complete-editing]
                     :on-failure      [:api-failure]}})))

(re-frame/reg-event-db
  :complete-editing
  (fn  [db [_ response]]
    (let [{status ::p/status results ::p/results} response
          selected-type (get-in db [:selecting :type])]
      (if (= ::p/ok status)
        (do
          (if selected-type
            (re-frame/dispatch [:select-entity selected-type])) ;TODO inefficient implementation, update should happen on per entity basis
          (dissoc db :editing))
        (assoc db :error {:message "Something goes wrong"})))))