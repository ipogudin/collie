(ns ipogudin.collie.client.view.common
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [ipogudin.collie.client.common :refer [format]]))

(defn raw-html
  [html]
  {:dangerouslySetInnerHTML {:__html html}})

(defn modal-dialog-renderer
  [id body & opts]
  (let [{:keys [title buttons handler]} (apply hash-map opts)
        {{ok-title :title ok-handler :handler :as ok} :ok
         {cancel-title :title cancel-handler :handler :as cancel} :cancel} buttons]
        [:div.modal.fade {:id id}
         [:div.modal-dialog {:role "document"}
          [:div.modal-content
           [:div.modal-header
            [:h5.modal-title title]
            [:button.close {:type "button"
                            :data-dismiss "modal"
                            :aria-label "Close"}
             [:span
              (raw-html "&times;")]]]
           [:div.modal-body
            body]
           (into
             [:div.modal-footer]
             (filterv
               some?
               [(if ok
                  [:button.btn.btn-primary {:type "button"
                                            :data-dismiss "modal"
                                            :on-click (fn []
                                                        (if ok-handler
                                                          (ok-handler)))}
                   ok-title])
                (if cancel
                  [:button.btn.btn-secondary {:type "button"
                                              :data-dismiss "modal"
                                              :on-click (fn []
                                                          (if ok-handler
                                                            (cancel-handler)))}
                   cancel-title])]))]]]))

(defn modal-dialog
  [body & opts]
  (let [id (str (random-uuid))
        {:keys [handler]} (apply hash-map opts)]
    (reagent/create-class
      {:component-did-mount
       (fn []
         (-> (format "#%s" id) js/jQuery (.modal "show"))
         (-> (format "#%s" id) js/jQuery (.on "hidden.bs.modal"
                                              (fn []
                                                (if handler (handler))))))
       ;:component-did-update
       ;(fn []
       ;  (-> (format "#%s" id) js/jQuery (.modal "show")))
       :reagent-render
       (fn [body & opts] (apply modal-dialog-renderer (->> opts (cons body) (cons id))))})))
