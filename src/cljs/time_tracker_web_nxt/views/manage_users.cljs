(ns time-tracker-web-nxt.views.manage-users
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
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
         :on-click #(rf/dispatch [:invite-user {:email @email}])}
        "Invite"]])))

(defn panel []
  [:div.page
   [common/header]
   [invite-user-form]])
