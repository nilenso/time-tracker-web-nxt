(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.string :as gs]
            [goog.string.format]))

(defn add-timer []
  (let [timer-note (reagent/atom nil)]
    [:div 
     [:textarea {:placeholder "Add notes"
                 :on-change #(reset! timer-note (-> % .-target .-value))}]
     [:button
      {:type "input" :on-click #(re-frame/dispatch [:add-timer @timer-note])}
      "Add a Timer"]]))

(defn time-display [elapsed-seconds]
  (let [minutes (-> (quot elapsed-seconds 60)
                    (mod 60))
        hours (quot minutes 60)
        seconds (mod elapsed-seconds 60)]
    (gs/format "%02d:%02d:%02d" hours minutes seconds)))

(defn timer [{:keys [id elapsed state note]}]
  [:div 
   [:p "Timer " id " has been running for " (time-display elapsed) " seconds as " state
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
        ts (re-frame/subscribe [:timers])] 
    (fn []
      [:div
       [:div "Hello from " @name]
       [add-timer]
       [timers @ts]])))
