(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.string :as gs]
            [goog.string.format]))

(defn add-timer [projects]
  (let [timer-note (atom nil)
        [_ default-task] (first projects)
        timer-project (atom (:task default-task))] 
    [:div
     (into  [:select {:placeholder "Add Project"
                      :default-value (:task default-task)
                      :on-change #(reset! timer-project
                                          (-> % .-target .-value))}]
            (for [[_ task] projects]
              [:option (:task task)]))
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
  [:div "Timer " id " for project " project
   " has been running for " (display-time (:hh elapsed) (:mm elapsed) (:ss elapsed))
   " seconds as " state
   " with notes " note
   (condp = state
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

(defn timer [{:keys [id elapsed state project note]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id elapsed state project note]}]
      (let [elapsed-map (split-time elapsed)
            timer-options {:id id :elapsed elapsed-map :project project :state state
                           :note note :edit-timer? edit-timer?}] 
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
  (let [name (re-frame/subscribe [:name])
        ts (re-frame/subscribe [:timers])
        projects (re-frame/subscribe [:projects])] 
    (fn []
      [:div
       [:div "Hello from " @name]
       [add-timer @projects]
       [timers @ts]])))
