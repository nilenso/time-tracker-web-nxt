(ns time-tracker-web-nxt.views.task
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [time-tracker-web-nxt.views.common :as common]))

(defn task-form []
  (let [show-task-form?  (rf/subscribe [:show-task-form?])
        task-name        (reagent/atom "")
        selected-project (rf/subscribe [:selected-project])
        submit           {:name    "Create"
                          :handler (fn [m]
                                     (rf/dispatch [:create-task m])
                                     (rf/dispatch [:hide-task-form])
                                     (reset! task-name ""))}
        cancel           {:name    "Cancel"
                          :handler (fn []
                                     (rf/dispatch [:hide-task-form])
                                     (reset! task-name ""))}]
    (fn []
      [:div.create-task-form {:style (if @show-task-form? {} {:display "none"})}
       [:h2 "Create a task"]
       [:div
        [:label.cclabel "Name: "]
        [common/input {:type      "text"
                       :name      "name"
                       :value     @task-name
                       :class     "ccinput"
                       :on-change #(reset! task-name %)}]]
       [:div.button-group.actions
        [:button.btn.btn-primary
         {:type "input" :on-click #((:handler submit) {:name       @task-name
                                                       :project-id (:id @selected-project)})}
         (:name submit)]
        [:button.btn.btn-secondary
         {:type "input" :on-click (:handler cancel)}
         (:name cancel)]]])))
