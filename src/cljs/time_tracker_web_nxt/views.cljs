(ns time-tracker-web-nxt.views
  (:require
   [cljs-pikaday.reagent :as pikaday]
   [goog.string :as gs]
   [goog.string.format]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]
   [time-tracker-web-nxt.auth :as auth]))

(defn project-dropdown [projects selected]
  [:div
   [:select {:placeholder "Add Project"
             :style {:margin-bottom "1em" :padding-left "0.5em" :width "100%"}
             :default-value (:name (first projects))
             :on-change #(reset! selected
                                 {:id (-> % .-target .-value)
                                  :name (-> % .-target .-label)})}
    (for [{:keys [id name]} projects]
      ^{:key id}
      [:option {:value id} name])]])

(defn add-timer-widget [projects]
  (let [timer-note (atom nil)
        default-project (first projects)
        timer-project (atom default-project)
        show? (re-frame/subscribe [:show-add-timer-widget?])]
    [:div {:style (if @show?
                    {:margin-bottom "2em"}
                    {:display "none"})}
     [project-dropdown projects timer-project]
     [:div [:textarea {:placeholder "Add notes"
                       :style {:margin-bottom "1em" :padding-left "0.5em" :width "99%"}
                       :on-change #(reset! timer-note (-> % .-target .-value))}]]
     [:div [:button.pure-button
            {:type "input"
             :style {:text-transform "uppercase" :margin-right "1em"}
             :on-click #(re-frame/dispatch [:show-add-timer-widget false])}
            "Cancel"]
      [:button.pure-button.pure-button-primary.ttbutton
       {:type "input"
        :on-click #(do
                     (re-frame/dispatch [:show-add-timer-widget false])
                     (re-frame/dispatch [:add-timer @timer-project @timer-note]))}
       "Start Timer"]]]))

(defn split-time [elapsed-seconds]
  (let [hours (quot elapsed-seconds (* 60 60))
        minutes (- (quot elapsed-seconds 60) (* hours 60))
        seconds (- elapsed-seconds (* hours 60 60) (* minutes 60))]
    {:hh hours :mm minutes :ss seconds}))

(defn format-time [elapsed-hh elapsed-mm elapsed-ss]
  (gs/format "%02d:%02d:%02d" elapsed-hh elapsed-mm elapsed-ss))

(defn format-project-name [p]
  (.join (.split p "|") " | "))

(defn timer-display
  [{:keys [id elapsed project state notes edit-timer?] :as timer}]
  [:tr
   [:td {:style {:border "none"}}
    [:span {:style {:font-size "1.1em"}} (format-project-name (:name project))]]
   [:td.time-col {:style {:border "none"}}
    [:span.time-display (format-time (:hh elapsed) (:mm elapsed) (:ss elapsed))]]
   [:td {:style {:border "none"}}
    (case state
      :paused
      [:span
       [:button.button-small.pure-button.ttbutton
        {:style {:margin-right 10} :on-click #(re-frame/dispatch [:resume-timer id])}
        "Start"]
       [:button.button-small.pure-button.ttbutton
        {:on-click #(reset! edit-timer? true)}
        "Edit"]]

      :running
      [:span
       [:button.button-small.pure-button.ttbutton
        {:on-click #(re-frame/dispatch [:stop-timer timer])}
        "Stop"]]

      nil)]
   ])

(defn timer-display-editable
  [{:keys [elapsed notes]}]
  (let [changes (reagent/atom {:notes notes
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
      [:div "Timer " id " for project " (:name project)
       " has been paused after "
       [:input {:value (:elapsed-hh @changes)
                :on-change (dur-change-handler-w-key :elapsed-hh)}]
       [:input {:value (:elapsed-mm @changes)
                :on-change (dur-change-handler-w-key :elapsed-mm)}]
       [:input {:value (:elapsed-ss @changes)
                :on-change (dur-change-handler-w-key :elapsed-ss)}]
       [:textarea {:value (:notes @changes)
                   :on-change #(swap! changes assoc :notes (-> % .-target .-value))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button {:on-click #(do
                              (reset! edit-timer? false)
                              (re-frame/dispatch [:update-timer id @changes]))} "Update"]])))

(defn timer [{:keys [id elapsed project-id notes]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id elapsed state project notes]}]
      (let [elapsed-map (split-time elapsed)
            all-projects @(re-frame/subscribe [:projects])
            get-project-by-id (fn [project-id projects]
                                (some #(when (= project-id (:id %)) %) projects))
            timer-options {:id id :elapsed elapsed-map
                           :project {:id project-id
                                     :name (:name (get-project-by-id project-id all-projects))}
                           :state state
                           :notes notes :edit-timer? edit-timer?}]
        (if @edit-timer?
          [timer-display-editable timer-options]
          [timer-display timer-options])))))

(defn timers [ts]
  (if (empty? ts)
    [:i "No timers for today"]
    (let [sorted-ts (->> ts vals (sort-by :id) reverse)]
      [:table.pure-table.pure-table-horizontal {:style {:border "none"}}
       [:colgroup
        [:col {:style {:width "60%"}}]
        [:col {:style {:width "20%"}}]
        [:col {:style {:width "20%"}}]
        ]
       [:tbody
        (for [t sorted-ts]
          ^{:key (:id t)}
          [timer t])]])))

(defn datepicker []
  ;; Note: This seems more like a hacked-together solution. Should look
  ;; for a better implementation.
  (let [date-atom (reagent/atom (js/Date.))]
    [pikaday/date-selector {:date-atom date-atom
                            :pikaday-attrs {:on-select
                                            #(do (reset! date-atom %)
                                                 (re-frame/dispatch [:timer-date-changed :timer-date @date-atom]))}
                            :input-attrs {:style {:padding "0.5em" :width "15.2em"}}}]))

(defn main-panel []
  (let [app-name (re-frame/subscribe [:app-name])
        user     (re-frame/subscribe [:user])
        ts (re-frame/subscribe [:timers])
        projects (re-frame/subscribe [:projects])]
    (fn []
      [:div.main
       [:br]
       [:div {:style {:text-align "center"}}
        [:p
         {:style {:display "inline-block" :margin-right "1em" :vertical-align "middle"
                  :font-size "1.2em" :text-transform "uppercase"}}
         "Current Date: "]
        [datepicker]
        [:button.pure-button.ttbutton
         {:style {:margin-left "1.1em" :vertical-align "baseline"}
          :on-click #(re-frame/dispatch [:show-add-timer-widget true])}
         "+"]]
       [:br]
       [:div {:id "new-timer-modal"}]
       [:br]
       [add-timer-widget @projects]
       [:div
        [:h2 "Timers"]
        [timers @ts]]])))

(defn login []
  [:div.splash-screen
   [:h1 {:style {:font-size "5em"}} "Time Tracker"]
   [:a.google-sign-in
    {:href "#"
     :on-click (fn [_] (-> (.signIn (auth/auth-instance))
                          (.then
                           #(re-frame/dispatch [:log-in %]))))
     } [:img {:src "../images/btn_google_signin_light_normal_web@2x.png"
              :style {:width "12em"}}]]
   ])

(defn logout []
  [:a.pure-menu-link {:href "#"
                      :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                                           (.then
                                            #(re-frame/dispatch [:log-out]))))}
   "Sign Out"])

(defn profile [user]
  [:div [:p {:style {:display "inline-block"
                     :vertical-align "super"
                     :margin-right "0.5em"}}
         [:strong (:name user)]]
   [:img {:src (:image-url user) :width "25px"}]])

(defn header [user]
  [:div.pure-menu.pure-menu-horizontal
   {:style {:border-bottom "1px solid #e4e6e8"}}
   [:a.pure-menu-heading
    {:href "#"
     :style {:color "#EB5424"}} "Time Tracker"]
   [:ul.pure-menu-list
    [:li.pure-menu-item [profile user]]
    [:li.pure-menu-item [logout]]]])

(defn dashboard [user]
  [:div {:style {:height "100%"}}
   [header user]
   [main-panel]])

(defn app []
  (let [user (re-frame/subscribe [:user])]
    (if-not (:signed-in? @user)
      [login]
      [dashboard @user])))
