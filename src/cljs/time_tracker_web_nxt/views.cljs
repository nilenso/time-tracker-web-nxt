(ns time-tracker-web-nxt.views
  (:require
   [cljs-pikaday.reagent :as pikaday]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as rdt]
   [re-frame-datatable.views :as rdt-views]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.routes :as routes]
   [time-tracker-web-nxt.utils :as utils]
   [taoensso.timbre :as timbre
    :refer-macros [log  trace  debug  info  warn  error  fatal  report
                   logf tracef debugf infof warnf errorf fatalf reportf
                   spy get-env]]))

(defn element-value [event]
  (-> event .-target .-value))

(defn project-dropdown [projects selected]
  (let [selected-id (:id @selected)]
    [:select.project-dropdown
     {:value (or selected-id "")
      :on-change #(reset! selected {:id (-> % .-target .-value)})}
     (for [{:keys [id name]} projects]
       ^{:key id} [:option (if (= selected-id id)
                             {:value id :label name}
                             {:value id :label name}) name])]))

(defn create-timer-widget []
  (let [projects         (rf/subscribe [:projects])
        default          {:id (:id (first @projects))}
        selected-project (reagent/atom default)
        data            (reagent/atom {:notes ""
                                       :elapsed-hh 0
                                       :elapsed-mm 0
                                       :elapsed-ss 0})
        show?            (rf/subscribe [:show-create-timer-widget?])]
    (fn []
      (let [default-selected     {:id (:id (first @projects))}
            notes-handler #(swap! data assoc :notes (element-value %))
            duration-handler (fn [key event]
                                      (let [val    (element-value event)
                                            parsed (if (empty? val) 0 (js/parseInt val 10))]
                                        (swap! data assoc key parsed)))
            partial-duration-handler #(partial duration-handler %)
            reset-elements!      (fn []
                                   (reset! selected-project default-selected)
                                   (reset! data {:notes ""
                                                 :elapsed-hh 0
                                                 :elapsed-mm 0
                                                 :elapsed-ss 0}))
            cancel-handler       (fn []
                                   (rf/dispatch [:hide-widget])
                                   (reset-elements!))
            create-handler        (fn []
                                   (rf/dispatch
                                    [:trigger-create-timer
                                     (if (:id @selected-project)
                                       @selected-project
                                       default-selected)
                                     @data])
                                   (reset-elements!))]
        [:div.new-timer-popup {:style (if @show? {} {:display "none"})}
         [project-dropdown @projects selected-project]
         [:textarea.project-notes {:placeholder "Add notes"
                                   :value       (:notes @data)
                                   :on-change   notes-handler}]
         [:div
          [:input {:value (:elapsed-hh @data) :on-change (partial-duration-handler :elapsed-hh)}]
          [:input {:value (:elapsed-mm @data) :on-change (partial-duration-handler :elapsed-mm)}]
          [:input {:value (:elapsed-ss @data) :on-change (partial-duration-handler :elapsed-ss)}]]

         [:div.button-group
          [:button.btn.btn-secondary
           {:type "input" :on-click cancel-handler}
           "Cancel"]
          [:button.btn.btn-primary
           {:type "input" :on-click create-handler}
           "Create"]]]))))

(defn format-project-name [p]
  (when-not (empty? p)
    (.join (.split p "|") " : ")))

(defn timer-display
  [{:keys [id duration project state notes edit-timer?] :as timer}]
  [:tr
   [:td.timer-column
    [:span.timer-project (format-project-name (:name project))]
    [:p.timer-notes notes]
    ]
   [:td.time-column {:style {:border "none"}}
    [:span.time-display (utils/format-time (:hh duration) (:mm duration) (:ss duration))]]
   [:td.time-column {:style {:border "none"}}
    (case state
      :paused
      [:span
       [:button.btn.btn-primary
        {:style {:margin-right 10} :on-click #(rf/dispatch [:trigger-start-timer {:id id}])}
        "Start"]
       [:button.btn.btn-secondary
        {:style {:margin-right 10} :on-click #(reset! edit-timer? true)}
        "Edit"]
       [:button.btn.btn-secondary
        {:on-click #(rf/dispatch [:trigger-delete-timer {:id id}])}
        "Delete"]]

      :running
      [:span
       [:button.btn.btn-primary
        {:on-click #(rf/dispatch [:trigger-stop-timer timer])}
        "Stop"]]

      nil)]
   ])

(defn timer-edit
  [{:keys [duration notes]}]
  (let [changes                 (reagent/atom {:notes      notes
                                               :elapsed-hh (:hh duration)
                                               :elapsed-mm (:mm duration)
                                               :elapsed-ss (:ss duration)})
        duration-change-handler (fn [key event]
                                  (let [val    (element-value event)
                                        parsed (if (empty? val) 0 (js/parseInt val 10))]
                                    (swap! changes assoc key parsed)))
        handler                 #(partial duration-change-handler %)]
    (fn [{:keys [id project edit-timer?]}]
      [:div
       [:input {:value (:elapsed-hh @changes) :on-change (handler :elapsed-hh)}]
       [:input {:value (:elapsed-mm @changes) :on-change (handler :elapsed-mm)}]
       [:input {:value (:elapsed-ss @changes) :on-change (handler :elapsed-ss)}]
       [:textarea {:value     (:notes @changes)
                   :on-change #(swap! changes assoc :notes (element-value %))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button
        {:on-click (fn []
                     (reset! edit-timer? false)
                     (rf/dispatch [:trigger-update-timer (assoc @changes :id id)]))}
        "Update"]])))

(defn timer-row [{:keys [id duration project-id notes]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id duration state project notes]}]
      (let [elapsed       (utils/->hh-mm-ss duration)
            projects      @(rf/subscribe [:projects])
            get-by-id     (fn [p id] (some #(when (= id (:id %)) %) p))
            timer-options {:id          id
                           :duration    elapsed
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

(defn sign-in-panel []
  (if-not (:signed-in? @(rf/subscribe [:user]))
    [:div.splash-screen
     [:h1.splash-screen-title "Time Tracker"]
     [:a.sign-in {:href     "#"
                  :on-click (fn [_] (-> (.signIn (auth/auth-instance)) ;; FIX: handle error cases
                                        (.then #(rf/dispatch [:log-in %]))))}
      [:img.google-sign-in
       {:src "images/btn_google_signin_light_normal_web@2x.png"
        :alt "Sign in with Google"}]]]))

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
     [:a {:href (routes/url-for :clients)
          :style {:display (if (= "admin" (:role @user))
                             "block"
                             "none")}
          :on-click #(rf/dispatch [:get-all-clients])}
      [:li.user-menu-link "Manage Clients"]]
     [:a {:href "javascript:void(0)"
          :on-click (fn [_] (-> (.signOut (auth/auth-instance))
                              (.then
                               #(rf/dispatch [:log-out]))))}
      [:li.user-menu-link "Sign out"]]]))

(defn header []
  (let [active-panel  (rf/subscribe [:active-panel])
        timers-panel? (= :timers @active-panel)
        about-panel?  (= :about @active-panel)
        clients-panel? (= :clients @active-panel)
        user (rf/subscribe [:user])]
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

(defn timers-panel []
  (let [user     (rf/subscribe [:user])]
    (rf/dispatch [:create-ws-connection (:token @user)])
    [:div.page
     [header]
     [main]]))

(defn about-panel []
  [:div.page
   [header]
   [:div.about
    [:p "Built with ♥ by the folks at Nilenso"]]])

(defn points-of-contact [client]
  (let [update-poc (fn [path val]
                     (rf/dispatch [:update-point-of-contact path val]))]
    (conj
     [:div.poc-parent]
     (for [[id data] (:points-of-contact client)]
       ^{:key id}
       [:div.poc-child
        [:label.cclabel "Name: "]
        [:input.poc-input
         {:type "text"
          :name (str "poc-name-" id)
          :value (:name data)
          :on-change #(update-poc [id :name] (element-value %))}]
        [:label.cclabel "Phone: "]
        [:input.poc-input
         {:type "text"
          :name (str "poc-phone-" id)
          :value (:phone data)
          :on-change #(update-poc [id :phone] (element-value %))}]
        [:label.cclabel "Email: "]
        [:input.poc-input
         {:type "text"
          :name (str "poc-email-" id)
          :value (:email data)
          :on-change #(update-poc [id :email] (element-value %))}]])
     [:a.link.link-primary
      {:href "javascript:void(0)"
       :on-click #(rf/dispatch [:add-point-of-contact {:name ""
                                                       :phone ""
                                                       :email ""
                                                       :action "insert"
                                                       :client_id (:id client)}])}
      [:i.fa.fa-plus-square {:aria-hidden "true"}]
      [:span "Point of Contact"]])))

(defn client-form [{:keys [source source-update-fn submit cancel]}]
  [:div.create-client-form
   [:div
    [:label.cclabel "Name: "]
    [:input.ccinput {:type      "text"
                     :name      "name"
                     :value     (:name @source)
                     :on-change #(source-update-fn :name (element-value %))}]]
   [:div
    [:label.cclabel "Address: "]
    [:textarea.cctextarea {:name      "address"
                           :value     (:address @source)
                           :on-change #(source-update-fn :address (element-value %))}]]
   [:div
    [:label.cclabel "GSTIN: "]
    [:input.ccinput {:type      "text"
                     :name      "gstin"
                     :value     (:gstin @source)
                     :on-change #(source-update-fn :gstin (element-value %))}]]
   [:div
    [:label.cclabel "PAN: "]
    [:input.ccinput {:type      "text"
                     :name      "pan"
                     :value     (:pan @source)
                     :on-change #(source-update-fn :pan (element-value %))}]]
   [points-of-contact @source]
   [:div.button-group.actions
    [:button.btn.btn-primary
     {:type "input" :on-click (:handler submit)}
     (:name submit)]
    [:button.btn.btn-secondary
     {:type "input" :on-click (:handler cancel)}
     (:name cancel)]]])

(defn create-client-panel []
  (let [client         (rf/subscribe [:client])
        update-fn      (fn [k v]
                         (rf/dispatch [:update-client-details k v]))
        submit-handler #(rf/dispatch [:create-client @client])
        cancel-handler #(rf/dispatch [:cancel-form-and-return
                                      {:panel         :clients
                                       :remove-db-key :client}])]
    (fn []
      [:div.page
       [header]
       (client-form {:source           client
                     :source-update-fn update-fn
                     :submit           {:name    "Create"
                                        :handler submit-handler}
                     :cancel           {:name    "Cancel"
                                        :handler cancel-handler}})])))

(defn edit-client-panel []
  (let [client         (rf/subscribe [:client])
        update-fn      (fn [k v]
                         (rf/dispatch [:update-client-details k v]))
        submit-handler #(rf/dispatch [:update-client @client])
        cancel-handler #(rf/dispatch [:cancel-form-and-return
                                      {:panel         :clients
                                       :remove-db-key :client}])]
    (fn []
      [:div.page
       [header]
       (client-form {:source           client
                     :source-update-fn update-fn
                     :submit           {:name    "Update"
                                        :handler submit-handler}
                     :cancel           {:name    "Cancel"
                                        :handler cancel-handler}})])))

(defn clients-panel []
  [:div.page
   [header]
   [:div.panel
    [:button.btn.btn-primary
     {:type     "input"
      :on-click #(rf/dispatch [:goto :create-client])}
     "+ Add Client"]
    [rdt/datatable
     :client-datatable
     [:clients]
     [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
      {::rdt/column-key [:name] ::rdt/column-label "Name" ::rdt/sorting {::rdt/enabled? true}}
      {::rdt/column-key [:address] ::rdt/column-label "Address"}
      {::rdt/column-key [:gstin] ::rdt/column-label "GSTIN"}
      {::rdt/column-key [:pan] ::rdt/column-label "PAN"}
      {::rdt/column-key []
       ::rdt/column-label ""
       ::rdt/render-fn
       (fn [client]
         [:a {:href "javascript:void(0)"
              :on-click #(rf/dispatch [:show-edit-client-form client])}
          [:i.fa.fa-pencil-square-o {:aria-hidden "true"}]])}]
     {::rdt/pagination {::rdt/enabled? true
                        ::rdt/per-page 10}}]
    [rdt-views/default-pagination-controls
     :client-datatable
     [:clients]]]])

(def panels
  {:timers        timers-panel
   :about         about-panel
   :sign-in       sign-in-panel
   :clients       clients-panel
   :create-client create-client-panel
   :edit-client   edit-client-panel})

(defn app []
  (let [active-panel (rf/subscribe [:active-panel])
        user (rf/subscribe [:user])]
    (when (:signed-in? @user)
      (rf/dispatch [:fetch-data]))
    [(panels @active-panel)]))
