(ns time-tracker-web-nxt.views.project
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [time-tracker-web-nxt.views.common :as common]
            [time-tracker-web-nxt.views.task :as task-views]
            [time-tracker-web-nxt.routes :as routes]
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
        selected-client-id     (rf/subscribe [:selected-client])
        all-projects            (rf/subscribe [:projects-for-client])
        all-clients             (rf/subscribe [:clients])
        show-task-form?         (rf/subscribe [:show-task-form?])
        selected-client         (first (filter #(= (:id %) @selected-client-id) @all-clients))
        selected-project        (first (filter #(= (:id %) @selected-project-id) @all-projects))]
    (fn []
      [:div.page
       [common/header]
       [:div.panel
        [:h2 [common/hierarchy-widget [{:href (routes/url-for :clients)
                                        :title "All Clients"}
                                       {:href (routes/url-for :client
                                                              :client-id (:id selected-client))
                                        :title (:name selected-client)}
                                       {:title (:name selected-project)}]]]
        [:hr]
        [:br]
        [task-views/task-form]
        [:button.btn.btn-primary
         {:type     "input"
          :on-click #(rf/dispatch [:show-task-form])
          :style    (if-not @show-task-form? {} {:display "none"})}
         "+ Add Task"]
        [rdt/datatable
         :task-datatable
         [:tasks-for-project]
         [{::rdt/column-key [:name] ::rdt/column-label "Task Name" ::rdt/sorting {::rdt/enabled? true}}]
         {::rdt/pagination {::rdt/enabled? true
                            ::rdt/per-page 10}}]
        [rdt-views/default-pagination-controls
         :task-datatable
         [:tasks-for-project]]]])))
