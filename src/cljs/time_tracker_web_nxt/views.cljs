(ns time-tracker-web-nxt.views
  (:require
   [cljs-pikaday.reagent :as pikaday]
   [hodgepodge.core :refer [get-item local-storage]]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.utils :as utils]
   [taoensso.timbre :as timbre
    :refer-macros [log  trace  debug  info  warn  error  fatal  report
                   logf tracef debugf infof warnf errorf fatalf reportf
                   spy get-env]]))

(defn get-element-value [event]
  (-> event .-target .-value))

(defn project-dropdown [projects selected]
  (let [selected-id (:id @selected)]
    [:select.project-dropdown
     {:on-change #(reset! selected {:id (-> % .-target .-value)})}
     (for [{:keys [id name]} projects]
       ^{:key id} [:option (if (= selected-id id)
                             {:value id :label name :selected "selected"}
                             {:value id :label name}) name])]))

(defn create-timer-widget []
  (let [projects         (rf/subscribe [:projects])
        default          {:id (:id (first @projects))}
        selected-project (reagent/atom default)
        notes            (reagent/atom "")
        show?            (rf/subscribe [:show-create-timer-widget?])]
    (fn []
      (let [default-selected     {:id (:id (first @projects))}
            notes-change-handler #(reset! notes (get-element-value %))
            reset-elements!      (fn []
                                   (reset! selected-project default-selected)
                                   (reset! notes ""))
            cancel-handler       (fn []
                                   (rf/dispatch [:hide-widget])
                                   (reset-elements!))
            start-handler        (fn []
                                   (rf/dispatch
                                    [:create-and-start-timer
                                     (if (:id @selected-project)
                                       @selected-project
                                       default-selected)
                                     @notes])
                                   (reset-elements!))]
        [:div.new-timer-popup {:style (if @show? {} {:display "none"})}
         [project-dropdown @projects selected-project]
         [:textarea.project-notes {:placeholder "Add notes"
                                   :value       @notes
                                   :on-change   notes-change-handler}]
         [:div.button-group
          [:button.btn.btn-secondary
           {:type "input" :on-click cancel-handler}
           "Cancel"]
          [:button.btn.btn-primary
           {:type "input" :on-click start-handler}
           "Start"]]]))))

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
    [:span.time-display (utils/format-time (:hh elapsed) (:mm elapsed) (:ss elapsed))]]
   [:td.time-column {:style {:border "none"}}
    (case state
      :paused
      [:span
       [:button.btn.btn-primary
        {:style {:margin-right 10} :on-click #(rf/dispatch [:resume-timer id])}
        "Start"]
       [:button.btn.btn-secondary
        {:on-click #(reset! edit-timer? true)}
        "Edit"]]

      :running
      [:span
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:stop-timer timer])}
        "Stop"]]

      nil)]
   ])

(defn timer-edit
  [{:keys [elapsed notes]}]
  (let [changes                 (reagent/atom {:notes      notes
                                               :elapsed-hh (:hh elapsed)
                                               :elapsed-mm (:mm elapsed)
                                               :elapsed-ss (:ss elapsed)})
        duration-change-handler (fn [key event]
                                  (let [val    (get-element-value event)
                                        parsed (if (empty? val) 0 (js/parseInt val 10))]
                                    (swap! changes assoc key parsed)))
        handler                 #(partial duration-change-handler %)]
    (fn [{:keys [id project edit-timer?]}]
      [:div
       [:input {:value (:elapsed-hh @changes) :on-change (handler :elapsed-hh)}]
       [:input {:value (:elapsed-mm @changes) :on-change (handler :elapsed-mm)}]
       [:input {:value (:elapsed-ss @changes) :on-change (handler :elapsed-ss)}]
       [:textarea {:value     (:notes @changes)
                   :on-change #(swap! changes assoc :notes (get-element-value %))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button
        {:on-click (fn []
                     (reset! edit-timer? false)
                     (rf/dispatch [:update-timer id @changes]))}
        "Update"]])))

(defn timer-row [{:keys [id elapsed project-id notes]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id elapsed state project notes]}]
      (let [elapsed       (utils/->hh-mm-ss elapsed)
            projects      @(rf/subscribe [:projects])
            get-by-id     (fn [p id] (some #(when (= id (:id %)) %) p))
            timer-options {:id          id
                           :elapsed     elapsed
                           :project     {:id   project-id
                                         :name (:name (get-by-id projects project-id))}
                           :state       state
                           :notes       notes
                           :edit-timer? edit-timer?}]
        (if @edit-timer?
          [timer-edit timer-options]
          [timer-display timer-options])))))

(defn timer-list []
  (let [sorted-timers @(rf/subscribe [:sorted-timers])]
    (if (empty? sorted-timers)
     [:p.empty-list-placeholder "No timers for today"]
     [:table.pure-table.pure-table-horizontal
      [:colgroup
       [:col {:style {:width "60%"}}]
       [:col {:style {:width "20%"}}]
       [:col {:style {:width "20%"}}]
       ]
      [:tbody
       (for [t sorted-timers]
         ^{:key (:id t)} [timer-row t])]])))

(defn datepicker []
  [pikaday/date-selector
   {:date-atom (rf/subscribe [:timer-date])
    :pikaday-attrs
    {:on-select #(rf/dispatch [:timer-date-changed :timer-date %])}}])

(defn main []
  [:div.main
   [:div.new-timer
    [:label "Current Date: "]
    [datepicker]
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [:show-widget])}
     "+"]
    [create-timer-widget]]

   [:div.timers
    [:h3 [:i "Today's Timers"]]
    [timer-list]]])

(defn sign-in []
  [:div.splash-screen
   [:h1.splash-screen-title "Time Tracker"]
   [:a.sign-in {:href     "#"
                :on-click (fn [_] (-> (.signIn (auth/auth-instance))
                                    (.then
                                     #(rf/dispatch [:log-in %]))))}
    [:img.google-sign-in]]])

(defn sign-out []
  [:a.link.link-secondary {:href     "#"
                           :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                                               (.then
                                                #(rf/dispatch [:log-out]))))}
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
    [sign-out]]])

(defn timers-panel [user]
  (let [boot-ls? (rf/subscribe [:boot-from-local-storage?])]
    ;; If loading data from localstorage, start ticking a running timer if any.
    ;; FIXME: Figure out a better way to do this
    (if @boot-ls?
      (rf/dispatch [:tick-running-timer])
      (do (rf/dispatch [:create-ws-connection (:token user)])
          [:div.page
           [header user]
           [main]]))))

(defn app []
  (let [user (rf/subscribe [:user])]
    (if-not (:signed-in? @user)
      [sign-in]
      [timers-panel @user])))
