(ns ipogudin.collie.client.events
  (:require [re-frame.core :as re-frame]
            [ipogudin.collie.client.db :as db]
            [day8.re-frame.http-fx]
            [ajax.edn :as ajax]
            [ipogudin.collie.common :refer [deep-merge dissoc-in]]
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

(defn prepare-pagination [db type]
  (let [current-type (get-in db [:selecting :type])
        current-pagination (get-in db [:selecting :pagination])
        default-pagination (:pagination @configuration/configuration)]
      (deep-merge
        {:page 1 :limit 10}
        default-pagination
        (if (= type current-type) current-pagination {}))))

(defn prepare-ordering [db type]
  (let [entity-schema (get-in db [:schema type])
        current-type (get-in db [:selecting :type])]
    (if (not= type current-type)
      (->>
        (map
          (fn [{{order ::schema/default-order} ::schema/ui name ::schema/name}]
            (if (some? order)
              [name order]))
          (::schema/fields entity-schema))
        (filterv some?)
        (into {}))
      (get-in db [:selecting :ordering]))))

(defn prepare-filtering [db type]
  (let [entity-schema (get-in db [:schema type])
        current-type (get-in db [:selecting :type])]
    (if (not= type current-type)
      {}
      (get-in db [:selecting :filtering]))))

(defn switch-order [order]
  (case order
    :asc :desc
    :desc nil
    nil :asc))

(defn process-pagination
  [pagination]
  [::p/offset (* (- (:page pagination) 1) (:limit pagination))
   ::p/limit (:limit pagination)])

(defn process-ordering
  [ordering]
  (if (empty? ordering)
    []
    [::order-by (filterv second ordering)]))

(defn process-filtering
  [filtering]
  (if (empty? filtering)
    []
    [::filter (vec filtering)]))

(re-frame/reg-event-fx
  :select-entities
  (fn  [{:keys [db]} [_ type]]
    (let [pagination (prepare-pagination db type)
          ordering (prepare-ordering db type)
          filtering (prepare-filtering db type)
          api-root (:api-root @configuration/configuration)]
      {:db (merge db {:selecting {
                                      :status :unsync
                                      :entities nil
                                      :type type
                                      :pagination pagination
                                      :ordering ordering
                                      :filtering filtering
                                      }})
       :http-xhrio  {:method          :post
                     :uri             api-root
                     :params          (p/request [(apply p/get-entities-command
                                                     (concat
                                                       [type
                                                        ::p/resolved-dependencies true]
                                                       (process-pagination pagination)
                                                       (process-ordering ordering)
                                                       (process-filtering filtering))
                                                    )])
                     :timeout         30000
                     :format          (ajax/edn-request-format)
                     :response-format (ajax/edn-response-format)
                     :on-success      [:set-selected-entities]
                     :on-failure      [:api-failure]}})))

(re-frame/reg-event-fx
  :set-pagination
  (fn  [{:keys [db]} [_ pagination]]
    (let [type (get-in db [:selecting :type])]
      {:db (deep-merge db {:selecting {:pagination pagination}})
       :dispatch [:select-entities type]})))

(re-frame/reg-event-fx
  :set-filtering
  (fn  [{:keys [db]} [_ filtering]]
    (let [type (get-in db [:selecting :type])]
      {:db (deep-merge db {:selecting {:filtering filtering}})
       :dispatch [:select-entities type]})))

(re-frame/reg-event-fx
  :disable-filtering
  (fn  [{:keys [db]} [_ field-name]]
    (let [type (get-in db [:selecting :type])]
      {:db (dissoc-in db [:selecting :filtering field-name])
       :dispatch [:select-entities type]})))

(re-frame/reg-event-fx
  :next-page
  (fn  [{:keys [db]} [_]]
    (let [page (get-in db [:selecting :pagination :page])]
      {:dispatch [:set-pagination {:page (inc page)}]})))

(re-frame/reg-event-fx
  :previous-page
  (fn  [{:keys [db]} [_]]
    (let [page (get-in db [:selecting :pagination :page])
          previous-page (dec page)]
      {:dispatch [:set-pagination {:page (if (< previous-page 1) 1 previous-page)}]})))

(re-frame/reg-event-fx
  :switch-ordering
  (fn  [{:keys [db]} [_ field-name]]
    (let [type (get-in db [:selecting :type])
          order (get-in db [:selecting :ordering field-name])]
      {:db (deep-merge db {:selecting {:ordering {field-name (switch-order order)}}})
       :dispatch [:select-entities type]})))

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
            (re-frame/dispatch [:select-entities selected-type])) ;TODO inefficient implementation, update should happen on per entity basis
          (dissoc db :editing))
        (assoc db :error {:message "Something goes wrong"})))))