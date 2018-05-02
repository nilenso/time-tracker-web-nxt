(ns time-tracker-web-nxt.views.project
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [time-tracker-web-nxt.views.common :as common]
            [time-tracker-web-nxt.views.task :as task-views]
            [re-frame-datatable.core :as rdt]
            [re-frame-datatable.views :as rdt-views]))

(defn project-form [show?]
  (let [project-name   (reagent/atom "")
        selected-client (rf/subscribe [:selected-client])
        submit {:name "Create"
                :handler (fn [m]
                           (rf/dispatch [:create-project m])
                           (reset! show? false)
                           (reset! project-name ""))}
        cancel {:name "Cancel"
                :handler (fn []
                           (reset! show? false)
                           (reset! project-name ""))}]
    (fn [show?]
      [:div.create-project-form {:style (if @show? {} {:display "none"})}
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


(defn project-panel []
  (let [selected-project-id     (rf/subscribe [:selected-project])
        all-projects            (rf/subscribe [:projects-for-client])
        show-task-form? (rf/subscribe [:show-task-form?])
        selected-project        (reagent/atom (first (filter #(= (:id %) @selected-project-id) @all-projects)))]
    (fn []
      [:div.page
       [common/header]
       [:div.panel
        [:h2 (:name @selected-project)]
        [:hr]
        [:br]
        [task-views/task-form]
        [:button.btn.btn-primary
         {:type     "input"
          :on-click #(rf/dispatch [:show-task-form])
          :style    (if-not @show-task-form? {} {:display "none"})}
         "+ Add Task"]
        [rdt/datatable
         :project-datatable
         [:tasks]
         [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
          {::rdt/column-key [:name] ::rdt/column-label "Task Name" ::rdt/sorting {::rdt/enabled? true}}]
         {::rdt/pagination {::rdt/enabled? true
                            ::rdt/per-page 10}}]
        [rdt-views/default-pagination-controls
         :task-datatable
         [:tasks-for-project]]]])))
