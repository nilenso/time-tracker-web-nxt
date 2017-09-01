(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame]
            [goog.string :as gs]
            [goog.string.format]))

(defn add-timer []
  [:button
   {:type "input" :on-click #(re-frame/dispatch [:add-timer (rand-int 30)])}
   "Add a Timer"])

(defn time-display [elapsed-seconds]
  (let [minutes (-> (quot elapsed-seconds 60)
                    (mod 60))
        hours (quot minutes 60)
        seconds (mod elapsed-seconds 60)]
    (gs/format "%02d:%02d:%02d" hours minutes seconds)))

(defn timer [[k {:keys [id elapsed state]}] ]
  [:div 
   [:p "Timer " id " has been running for " (time-display elapsed) " seconds as " state]
   (condp = state
     :paused [:button {:on-click #(re-frame/dispatch [:start-timer id])} "Start Timer"]
     :running [:button {:on-click #(re-frame/dispatch [:stop-timer id])} "Stop Timer"]
     nil)
   ])

(defn timers [ts]
  [:ul
   (for [t ts]
     ^{:key t}
     [:li [timer t]])])

(defn main-panel []
  (let [name (re-frame/subscribe [:name])
        ts (re-frame/subscribe [:timers])]
    (fn []
      [:div
       [:div "Hello from " @name]
       [add-timer]
       [timers @ts]])))
