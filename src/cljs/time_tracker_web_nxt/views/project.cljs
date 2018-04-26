(ns time-tracker-web-nxt.views.project
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]))


(defn project-form
  [{:keys [project-name submit cancel]}]
  (let [clients         (rf/subscribe [:clients])
        selected-client (rf/subscribe [:selected-client])]
    (fn [{:keys [project-name submit cancel] :as foo}]
      [:div.create-project-form
       [:h2 "Create a project"]
       [:div
        [:label.cclabel "Name: "]
        [:input.ccinput {:type      "text"
                         :name      "name"
                         :value     @project-name
                         :on-change #(reset! project-name (common/element-value %))}]]
       [common/dropdown-widget @clients @selected-client :select-client]
       [:div.button-group.actions
        [:button.btn.btn-primary
         {:type "input" :on-click #((:handler submit) {:name      @project-name
                                                       :client-id @selected-client})}
         (:name submit)]
        [:button.btn.btn-secondary
         {:type "input" :on-click (:handler cancel)}
         (:name cancel)]]])))

(defn create-project-panel []
  (let [project-name   (reagent/atom "")
        submit-handler (fn [m]
                         (rf/dispatch [:create-project m]))
        ;; TODO: change to goto projects page after adding a projects
        ;; page
        cancel-handler #(rf/dispatch [:goto :timers])]
    (fn []
      [:div.page
       [common/header]
       [project-form {:project-name project-name
                      :submit       {:name    "Create"
                                     :handler submit-handler}
                      :cancel       {:name    "Cancel"
                                     :handler cancel-handler}}]])))
