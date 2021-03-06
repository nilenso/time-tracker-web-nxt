(ns time-tracker-web-nxt.views
  (:require
   [re-frame.core :as rf]
   [time-tracker-web-nxt.views.common :as common]
   [time-tracker-web-nxt.views.timer :as timer-views]
   [time-tracker-web-nxt.views.client :as client-views]
   [time-tracker-web-nxt.views.project :as project-views]
   [time-tracker-web-nxt.views.task :as task-views]
   [time-tracker-web-nxt.views.manage-users :as manage-users-views]
   [time-tracker-web-nxt.auth :as auth]))

(defn sign-in-panel []
  (when-not (:signed-in? @(rf/subscribe [:user]))
    [:div.splash-screen
     [:h1.splash-screen-title "Time Tracker"]
     [:a.sign-in {:href     "#"
                  :on-click (fn [_] (-> (.signIn (auth/auth-instance)) ;; FIX: handle error cases
                                        (.then #(rf/dispatch [:log-in %]))))}
      [:img.google-sign-in
       {:src "/images/btn_google_signin_light_normal_web@2x.png"
        :alt "Sign in with Google"}]]]))


(defn about-panel []
  [:div.page
   [common/header]
   [:div.about
    [:p "Built with ♥ by the folks at nilenso"]]])


(def panels
  {:timers       timer-views/timers-panel
   :about        about-panel
   :sign-in      sign-in-panel
   :clients      client-views/clients-panel
   :client       client-views/client-panel
   :manage-users manage-users-views/panel
   :project      project-views/project-panel})

(defn app []
  (let [active-panel (rf/subscribe [:active-panel])
        user         (rf/subscribe [:user])]
    [(panels @active-panel)]))
