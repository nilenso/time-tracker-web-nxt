(ns time-tracker-web-nxt.views
  (:require
   [goog.string :as gs]
   [goog.string.format]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.auth :as auth]))

(defn add-timer [projects]
  (let [timer-note (atom nil)
        default-project (first projects)
        timer-project (atom default-project)]
    [:div
     [:select {:placeholder "Add Project"
               :default-value (:name default-project)
               :on-change #(reset! timer-project
                                   {:id (-> % .-target .-value)
                                    :name (-> % .-target .-label)})}
      (for [{:keys [id name]} projects]
        ^{:key id}
        [:option {:value id} name])]
     [:textarea {:placeholder "Add notes"
                 :on-change #(reset! timer-note (-> % .-target .-value))}]
     [:button
      {:type "input" :on-click #(re-frame/dispatch [:add-timer @timer-project @timer-note])}
      "Add a Timer"]]))

(defn split-time [elapsed-seconds]
  (let [hours (quot elapsed-seconds (* 60 60))
        minutes (- (quot elapsed-seconds 60) (* hours 60))
        seconds (- elapsed-seconds (* hours 60 60) (* minutes 60))]
    {:hh hours :mm minutes :ss seconds}))

(defn display-time [elapsed-hh elapsed-mm elapsed-ss]
  (gs/format "%02d:%02d:%02d" elapsed-hh elapsed-mm elapsed-ss))

(defn timer-display
  [{:keys [id elapsed project state note edit-timer?]}]
  [:div "Timer " id " for project " (:name project)
   " has been running for " (display-time (:hh elapsed) (:mm elapsed) (:ss elapsed))
   " seconds as " state
   " with notes " note
   (case state
     :paused
     [:div
      [:button {:on-click #(re-frame/dispatch [:start-timer id])} "Start Timer"]
      [:button {:on-click #(reset! edit-timer? true)} "Edit Timer"]]
     :running
     [:div
      [:button {:on-click #(re-frame/dispatch [:stop-timer id])} "Stop Timer"]]
     nil)])

(defn timer-display-editable
  [{:keys [elapsed note]}]
  (let [changes (reagent/atom {:note note
                               :elapsed-hh (:hh elapsed)
                               :elapsed-mm (:mm elapsed)
                               :elapsed-ss (:ss elapsed)})
        dur-change-handler (fn [elap-key e]
                             (let [elap-val (-> e .-target .-value)]
                               (swap! changes assoc elap-key (if (empty? elap-val)
                                                               0
                                                               (js/parseInt elap-val)))))
        dur-change-handler-w-key #(partial dur-change-handler %)]
    (fn [{:keys [id project edit-timer?]}]
      [:div "Timer " id " for project " project
       " has been running for "
       [:input {:value (:elapsed-hh @changes)
                :on-change (dur-change-handler-w-key :elapsed-hh)}]
       [:input {:value (:elapsed-mm @changes)
                :on-change (dur-change-handler-w-key :elapsed-mm)}]
       [:input {:value (:elapsed-ss @changes)
                :on-change (dur-change-handler-w-key :elapsed-ss)}]
       [:textarea {:value (:note @changes)
                   :on-change #(swap! changes assoc :note (-> % .-target .-value))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button {:on-click #(do
                              (reset! edit-timer? false)
                              (re-frame/dispatch [:update-timer id @changes]))} "Update"]])))

(defn timer [{:keys [id duration project-id notes]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id duration state project notes]}]
      (let [elapsed-map (split-time duration)
            all-projects @(re-frame/subscribe [:projects])
            get-project-by-id (fn [project-id projects]
                                (some #(when (= project-id (:id %)) %) projects))
            timer-options {:id id :elapsed elapsed-map
                           :project {:id project-id
                                     :name (:name (get-project-by-id project-id all-projects))}
                           :state state
                           :note notes :edit-timer? edit-timer?}]
        (if @edit-timer?
          [timer-display-editable timer-options]
          [timer-display timer-options])))))

(defn timers [ts]
  (let [sorted-ts (->> ts
                       vals
                       (sort-by :id)
                       reverse)]
    [:ul
     (for [t sorted-ts]
       ^{:key (:id t)}
       [:li [timer t]])]))

(defn main-panel []
  (let [app-name (re-frame/subscribe [:app-name])
        user     (re-frame/subscribe [:user])
        ts (re-frame/subscribe [:timers])
        projects (re-frame/subscribe [:projects])]
    (fn []
      [:div
       [:div "Hi " (:name  @user) "!" " " "Welcome to " @app-name]
       [add-timer @projects]
       [timers @ts]])))

(defn login []
  [:a
   {:href "#"
    :on-click (fn [_] (-> (.signIn (auth/auth-instance))
                         (.then
                          #(re-frame/dispatch [:log-in %]))))
    } "Sign in with Google"])

(defn logout []
  [:a {:href "#"
       :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                            (.then
                             #(re-frame/dispatch [:log-out]))))}
   "Sign Out"])

(defn profile [user]
  [:p "Hello "
    [:strong (:name user)]
    [:br]
    [:img {:src (:image-url user)}]])

(defn dashboard [user]
  [:div
   [profile user]
   [:div
    [logout]
    [:br]
    [:br]
    [main-panel]]])

(defn app []
  (let [user (re-frame/subscribe [:user])]
    (if-not (:signed-in? @user)
      [login]
      [dashboard @user])))
