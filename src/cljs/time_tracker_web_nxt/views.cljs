(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame] 
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

(defn time-display [elapsed-seconds]
  (let [minutes (-> (quot elapsed-seconds 60)
                    (mod 60))
        hours (quot minutes 60)
        seconds (mod elapsed-seconds 60)]
    (gs/format "%02d:%02d:%02d" hours minutes seconds)))

(defn timer [{:keys [id elapsed state project note]}]
  [:div 
   [:p "Timer " id " for project " project
    " has been running for " (time-display elapsed) " seconds as " state
    " with notes " note
    (condp = state
      :paused [:button {:on-click #(re-frame/dispatch [:start-timer id])} "Start Timer"]
      :running [:button {:on-click #(re-frame/dispatch [:stop-timer id])} "Stop Timer"]
      nil)] 
   ])

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
