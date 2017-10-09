(ns time-tracker-web-nxt.views
  (:require
   [cljs-pikaday.reagent :as pikaday]
   [goog.string :as gs]
   [goog.string.format]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]
   [time-tracker-web-nxt.auth :as auth]
   [taoensso.timbre :as timbre
    :refer-macros [log  trace  debug  info  warn  error  fatal  report
                   logf tracef debugf infof warnf errorf fatalf reportf
                   spy get-env]]))

(defn project-dropdown [projects selected]
  [:select.project-dropdown {:on-change #(reset! selected {:id (-> % .-target .-value)})}
   (for [{:keys [id name]} projects]
     ^{:key id}
     [:option {:value id :label name} name])])

(defn add-timer-widget [projects]
  (let [default-note ""
        timer-note (atom default-note)
        default-project (:id (first projects))
        timer-project (atom default-project)
        show? (re-frame/subscribe [:show-add-timer-widget?])]
    (fn [projects]
      [:div.new-timer-popup {:style (if @show? {} {:display "none"})}
       [project-dropdown projects timer-project]
       [:textarea.project-notes {:placeholder "Add notes"
                                 :on-change #(reset! timer-note (-> % .-target .-value))}]
       [:div.button-group
        [:button.btn.btn-secondary
         {:type "input"
          :on-click #(re-frame/dispatch [:show-add-timer-widget false])}
         "Cancel"]
        [:button.btn.btn-primary
         {:type "input"
          :on-click
          #(do
             (info "Added timer with project" @timer-project " and note " @timer-note)
             (re-frame/dispatch [:show-add-timer-widget false])
             (re-frame/dispatch [:add-timer @timer-project @timer-note]))}
         "Start"]]])))

(defn split-time [elapsed-seconds]
  (let [hours (quot elapsed-seconds (* 60 60))
        minutes (- (quot elapsed-seconds 60) (* hours 60))
        seconds (- elapsed-seconds (* hours 60 60) (* minutes 60))]
    {:hh hours :mm minutes :ss seconds}))

(defn format-time [elapsed-hh elapsed-mm elapsed-ss]
  (gs/format "%02d:%02d:%02d" elapsed-hh elapsed-mm elapsed-ss))

(defn format-project-name [p]
  (when-not (empty? p)
    (.join (.split p "|") " : ")))

(defn timer-display
  [{:keys [id elapsed project state notes edit-timer?] :as timer}]
  [:tr
   [:td.timer-column
    [:span.timer-project (format-project-name (:name project))]
    [:p.timer-notes notes]
    ]
   [:td.time-column {:style {:border "none"}}
    [:span.time-display (format-time (:hh elapsed) (:mm elapsed) (:ss elapsed))]]
   [:td.time-column {:style {:border "none"}}
    (case state
      :paused
      [:span
       [:button.btn.btn-primary
        {:style {:margin-right 10} :on-click #(re-frame/dispatch [:resume-timer id])}
        "Start"]
       [:button.btn.btn-secondary
        {:on-click #(reset! edit-timer? true)}
        "Edit"]]

      :running
      [:span
       [:button.btn.btn-primary
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
    [:p.empty-list-placeholder "No timers for today"]
    (let [sorted-ts (->> ts vals (sort-by :id) reverse)]
      [:table.pure-table.pure-table-horizontal
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
                            }]))

(defn main-panel []
  (let [app-name (re-frame/subscribe [:app-name])
        user     (re-frame/subscribe [:user])
        ts (re-frame/subscribe [:timers])
        projects (re-frame/subscribe [:projects])]
    (fn []
      [:div.main
       [:div.new-timer
        [:label
         "Current Date: "]
        [datepicker]
        [:button.btn.btn-primary
         {:on-click #(re-frame/dispatch [:show-add-timer-widget true])}
         "+"]
        [add-timer-widget @projects]]

       [:div.timers
        [:h3 [:i "Today's Timers"]]
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
  [:a.link.link-secondary {:href "#"
                           :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                                                 (.then
                                                  #(re-frame/dispatch [:log-out]))))}
   "Sign Out"])

(defn profile [user]
  [:div.user-profile
   [:p.user-name (:name user)]
   [:img.user-image {:src (:image-url user)}]])

(defn header [user]
  [:div.header.pure-menu.pure-menu-horizontal
   [:p#logo
    {:href "#"} "Time Tracker"]
   [:nav.menu
    [:ul.header-links
     [:li.header-link.active "Timers"]
     [:li.header-link "About"]
     ]]
   [:div.user-profile-and-signout
    [profile user]
    [logout]]])

(defn dashboard [user]
  [:div {:style {:height "100%"}}
   [header user]
   [main-panel]])

(defn app []
  (let [user (re-frame/subscribe [:user])]
    (if-not (:signed-in? @user)
      [login]
      [dashboard @user])))
