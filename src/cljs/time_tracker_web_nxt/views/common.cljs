(ns time-tracker-web-nxt.views.common
  (:require [cljs-pikaday.reagent :as pikaday]
            [re-frame.core :as rf]
            [re-frame-datatable.core :as rdt]
            [re-frame-datatable.views :as rdt-views]
            [reagent.core :as reagent]
            [time-tracker-web-nxt.routes :as routes]
            [time-tracker-web-nxt.auth :as auth]
            [reagent.ratom :as ratom]))

(defn element-value [event]
  (-> event .-target .-value))

(defn wrap-event-value
  [on-change-fn]
  (fn [event]
    (on-change-fn (element-value event))))

(defn input
  [{:keys [on-change] :as attrs}]
  [:input (assoc attrs
                 :on-change (wrap-event-value on-change))])

(defn dropdown-widget [items selected-id event]
  [:select.project-dropdown
   {:value     (or selected-id "")
    :on-change (fn [val]
                 (rf/dispatch [event (-> val element-value int)]))}
   (for [{:keys [id name]} items]
     ^{:key id} [:option (if (= selected-id id)
                           {:value id :label name}
                           {:value id :label name}) name])])
(defn datepicker []
  [pikaday/date-selector
   {:date-atom (rf/subscribe [:timer-date])
    :pikaday-attrs
    {:on-select #(rf/dispatch [:timer-date-changed :timer-date %])}}])

(defn user-profile []
  (let [user (rf/subscribe [:user])]
    [:div.user-profile
     [:img.user-image {:src (:image-url @user)}]]))

(defn user-menu []
  (let [show? (rf/subscribe [:show-user-menu?])
        user  (rf/subscribe [:user])]
    [:ul.user-menu-links
     {:style {:display (if @show? "block" "none")}}
     [:li.user-menu-header (str "Signed in as ")
      [:span.user-menu-username [:strong (:name @user)]]]
     [:li.dropdown-divider]
     [:a {:href     (routes/url-for :clients)
          :style    {:display (if (= "admin" (:role @user))
                                "block"
                                "none")}
          :on-click #(rf/dispatch [:get-clients])}
      [:li.user-menu-link "Manage Clients"]]
     [:a {:href     "javascript:void(0)"
          :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                                (.then
                                 #(rf/dispatch [:log-out]))))}
      [:li.user-menu-link "Sign out"]]]))

(defn header []
  (let [active-panel   (rf/subscribe [:active-panel])
        timers-panel?  (= :timers @active-panel)
        about-panel?   (= :about @active-panel)
        clients-panel? (= :clients @active-panel)
        user           (rf/subscribe [:user])]
    [:div.header.pure-menu.pure-menu-horizontal
     [:p#logo
      {:href "#"} "Time Tracker"]
     [:nav.menu
      [:ul.header-links
       [:li.header-link {:class (if timers-panel? "active" "")}
        [:a.nav-link
         {:href     (routes/url-for :timers)
          :on-click #(rf/dispatch [:set-active-panel :timers])}
         "Timers"]]
       [:li.header-link {:class (if about-panel? "active" "")}
        [:a.nav-link
         {:href     (routes/url-for :about)
          :on-click #(rf/dispatch [:set-active-panel :about])}
         "About"]]
       (if (= "admin" (:role @user))
         [:li.header-link {:class (if clients-panel? "active" "")}
          [:a.nav-link
           {:href     (routes/url-for :clients)
            :on-click #(rf/dispatch [:set-active-panel :clients])}
           "Clients"]])]]
     [:a.user-profile-link
      {:href     "javascript:void(0)"
       :on-click #(rf/dispatch [:toggle-user-menu])}
      [:div.user-profile-and-signout
       [user-profile]
       [:span.menu-arrow "▿"]]]
     [user-menu]]))
