(ns time-tracker-web-nxt.views.manage-users
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-frame-datatable.core :as rdt]
            [re-frame-datatable.views :as rdt-views]
            [time-tracker-web-nxt.routes :as routes]
            [time-tracker-web-nxt.views.common :as common]))

(defn invite-user-form []
  (let [email (reagent/atom "")]
    (fn []
      [:div.invite-user-form
       [:label.cclabel "Email: "]
       [common/input {:type "text"
                      :name "email"
                      :value @email
                      :class "ccinput"
                      :on-change (fn [v] (reset! email v))}]
       [:button.btn.btn-primary
        {:type "input"
         :on-click (fn []
                     (rf/dispatch [:invite-user {:email @email}])
                     (reset! email ""))}
        "Invite"]])))

(defn list-registered-users []
  [:div
   [rdt/datatable
    :registered-users-datatable
    [:registered-users]
    [{::rdt/column-key [:name] ::rdt/column-label "Name" ::rdt/sorting {::rdt/enabled? true}}
     {::rdt/column-key [:role] ::rdt/column-label "Role"}
     {::rdt/column-key [:email] ::rdt/column-label "Email"}]
    {::rdt/pagination {::rdt/enabled? true
                       ::rdt/per-page 10}}]
   [rdt-views/default-pagination-controls
    :registered-users-datatable
    [:registered-users]]])

(defn list-invited-users []
  (let [registered-users (rf/subscribe [:registered-users])]
    (fn []
      [:div
       [rdt/datatable
        :invited-users-datatable
        [:invited-users]
        [{::rdt/column-key [:email] ::rdt/column-label "Email"}
         {::rdt/column-key [:invited-by] ::rdt/column-label "Invited by"
          ::rdt/render-fn  (fn [invited-by]
                             [:span (->> @registered-users
                                         (filter #(= (:id %) invited-by))
                                         first
                                         :name)])}]
        {::rdt/pagination {::rdt/enabled? true
                           ::rdt/per-page 10}}]
       [rdt-views/default-pagination-controls
        :invited-users-datatable
        [:invited-users]]])))

(defn panel []
  [:div.page
   [common/header]
   [:div.panel

    [:h2 "Registered Users"]
    [:hr]
    [list-registered-users]
    [:hr]
    [:br]
    [:h3 "Invite User"]
    [invite-user-form]
    [:h2 "Invited Users"]
    [:hr]
    [list-invited-users]]])
