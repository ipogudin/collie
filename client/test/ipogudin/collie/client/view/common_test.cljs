(ns ipogudin.collie.client.view.common-test
  (:require [cljs.test :refer [deftest testing is]]
            [com.rpl.specter :refer [setval NONE]]
            [ipogudin.collie.common :refer [ALL-OBJECTS]]
            [ipogudin.collie.client.view.common :as common-view]))

(def dialog
  [:div.modal.fade {:id "id"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:div.modal-header
      [:h5.modal-title "test"]
      [:button.close {:type "button"
                      :data-dismiss "modal"
                      :aria-label "Close"}
       [:span
        {:dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body [:p "text as a parameter"]]
       [:div.modal-footer
        [:button.btn.btn-primary {:type "button"
                                  :data-dismiss "modal"}
         "OK"]]]]])


(deftest rendering
  (testing "Common rendering tools"
    (testing "modal-dialog"
      (let [rendered-dialog (common-view/modal-dialog-renderer
                              "id"
                              [:p "text as a parameter"]
                              :title "test"
                              :buttons {:ok {:title "OK"}})]
        (is (=
              dialog
              (setval
                [ALL-OBJECTS (partial = :on-click)]
                NONE
                rendered-dialog)))))))