(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame]
            [time-tracker-web-nxt.auth :as auth]))

(defn main-panel []
  (let [app-name (re-frame/subscribe [:app-name])
        user     (re-frame/subscribe [:user])]
    (fn []
      [:div "Hi " (:name  @user) "!" " " "Welcome to " @app-name])))

(defn app []
  [:div
   (let [user @auth/user] 
     (if-not (:signed-in? user)
       [:a
        {:href "#" :on-click (fn [_] (do
                                       (.signIn (auth/auth-instance))
                                       (re-frame/dispatch [:log-in user])))}
        "Sign in with Google"]
       [:div
        [:p "Hello "
         [:strong (:name user)]
         [:br]
         [:img {:src (:image-url user)}]]
        [:div 
         [:a
          {:href "#" :on-click (fn [_] (do  
                                         (.signOut (auth/auth-instance))
                                         (re-frame/dispatch [:log-out user])))}
          "Sign Out"]
         [:br]
         [:br]
         [main-panel]]]))])
