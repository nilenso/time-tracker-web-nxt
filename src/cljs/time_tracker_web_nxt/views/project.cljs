(ns time-tracker-web-nxt.views.project
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]))


(defn project-form []
  (let [project-name   (reagent/atom "")
        clients         (rf/subscribe [:clients])
        selected-client (rf/subscribe [:selected-client])
        submit {:name "Create"
                :handler (fn [m]
                           (rf/dispatch [:create-project m]))}
        ;; TODO: Cancel should just hide the form
        cancel {:name "Cancel"
                :handler #(rf/dispatch [:goto [:client :id @selected-client]])}]
    (fn []
      [:div.create-project-form
       [:h2 "Create a project"]
       [:div
        [:label.cclabel "Name: "]
        [common/input {:type      "text"
                       :name      "name"
                       :value     @project-name
                       :class     "ccinput"
                       :on-change #(reset! project-name %)}]]
       [:div.button-group.actions
        [:button.btn.btn-primary
         {:type "input" :on-click #((:handler submit) {:name      @project-name
                                                       :client-id @selected-client})}
         (:name submit)]
        [:button.btn.btn-secondary
         {:type "input" :on-click (:handler cancel)}
         (:name cancel)]]])))
