(ns time-tracker-web-nxt.views.timer
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]
   [time-tracker-web-nxt.utils :as utils]))

(defn- duration-handler
  [data key]
  (fn [val]
    (let [parsed (if (empty? val)
                   0
                   (js/parseInt val 10))]
      (swap! data assoc key parsed))))

(defn create-timer-widget []
  (let [data (reagent/atom {:notes      ""
                            :elapsed-hh 0
                            :elapsed-mm 0
                            :elapsed-ss 0})]
    (fn []
      (let [show?                    (rf/subscribe [:show-create-timer-widget?])
            clients                  @(rf/subscribe [:clients])
            selected-client          @(rf/subscribe [:selected-client])
            all-projects             @(rf/subscribe [:projects])
            projects                 (filter #(= (:client_id %) selected-client) all-projects)
            selected-project         @(rf/subscribe [:selected-project])
            all-tasks                @(rf/subscribe [:tasks])
            tasks                    (filter #(= (:project_id %) selected-project) all-tasks)
            selected-task            @(rf/subscribe [:selected-task])
            notes-handler            #(swap! data assoc :notes (common/element-value %))
            reset-elements!          (fn []
                                       (rf/dispatch [:select-client (:id (first clients))])
                                       (reset! data {:notes      ""
                                                     :elapsed-hh 0
                                                     :elapsed-mm 0
                                                     :elapsed-ss 0}))
            cancel-handler           (fn []
                                       (rf/dispatch [:hide-widget])
                                       (reset-elements!))
            create-handler           (fn []
                                       (rf/dispatch
                                        [:trigger-create-timer
                                         {:id selected-task}
                                         @data])
                                       (reset-elements!))]
        [:div.new-timer-popup {:style (if @show? {} {:display "none"})}
         [common/dropdown-widget clients selected-client :select-client]
         [common/dropdown-widget projects selected-project :select-project]
         [common/dropdown-widget tasks selected-task :select-task]
         [:textarea.project-notes {:placeholder "Add notes"
                                   :value       (:notes @data)
                                   :on-change   notes-handler}]
         [:div
          [:label
           "Hours"
           [common/input {:value     (:elapsed-hh @data)
                          :type      "number"
                          :on-change (duration-handler data :elapsed-hh)}]
           [:br]]
          [:label
           "Minutes"
           [common/input {:value     (:elapsed-mm @data)
                          :type      "number"
                          :on-change (duration-handler data :elapsed-mm)}]
           [:br]]
          [:label
           "Seconds"
           [common/input {:value     (:elapsed-ss @data)
                          :type      "number"
                          :on-change (duration-handler data :elapsed-ss)}]
           [:br]]]

         [:div.button-group
          [:button.btn.btn-secondary
           {:type "input" :on-click cancel-handler}
           "Cancel"]
          [:button.btn.btn-primary
           {:type "input" :on-click create-handler}
           "Create"]]]))))

(defn timer-display
  [{:keys [id duration client project task state notes edit-timer?] :as timer}]
  [:tr
   [:td.timer-column
    [:span.timer-project (:name client)]
    [:span " > "]
    [:span.timer-project (:name project)]
    [:span " > "]
    [:span.timer-project (:name task)]
    [:p.timer-notes notes]]
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

      nil)]])

(defn timer-edit
  [{:keys [duration notes]}]
  (let [changes                 (reagent/atom {:notes      notes
                                               :elapsed-hh (:hh duration)
                                               :elapsed-mm (:mm duration)
                                               :elapsed-ss (:ss duration)})]
    (fn [{:keys [id edit-timer?]}]
      [:div
       [common/input {:value     (:elapsed-hh @changes)
                      :type      "number"
                      :on-change (duration-handler changes :elapsed-hh)}]
       [common/input {:value     (:elapsed-mm @changes)
                      :type      "number"
                      :on-change (duration-handler changes :elapsed-mm)}]
       [common/input {:value     (:elapsed-ss @changes)
                      :type      "number"
                      :on-change (duration-handler changes :elapsed-ss)}]
       [:textarea {:value     (:notes @changes)
                   :on-change #(swap! changes assoc :notes (common/element-value %))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button
        {:on-click (fn []
                     (reset! edit-timer? false)
                     (rf/dispatch [:trigger-update-timer (assoc @changes :id id)]))}
        "Update"]])))

(defn timer-row [{:keys [id duration task-id notes]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id duration state task notes]}]
      (let [elapsed       (utils/->hh-mm-ss duration)
            clients       @(rf/subscribe [:clients])
            projects      @(rf/subscribe [:projects])
            tasks         @(rf/subscribe [:tasks])
            get-by-id     (fn [p id] (some #(when (= id (:id %)) %) p))
            task          (get-by-id tasks task-id)
            project       (get-by-id projects (:project_id task))
            client        (get-by-id clients (:client_id project))
            timer-options {:id          id
                           :duration    elapsed
                           :client      client
                           :project     project
                           :task        task
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
        [:col {:style {:width "20%"}}]]
       [:tbody
        (for [t sorted-timers]
          ^{:key (:id t)} [timer-row t])]])))

(defn main []
  [:div.main
   [:div.new-timer
    [:label "Current Date: "]
    [common/datepicker]
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [:show-widget])}
     "+"]
    [create-timer-widget]]

   [:div.timers
    [:h3 [:i "Today's Timers"]]
    [timer-list]]])

(defn timers-panel []
  (let [user (rf/subscribe [:user])]
    [:div.page
     [common/header]
     [main]]))
