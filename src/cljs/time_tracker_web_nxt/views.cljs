(ns time-tracker-web-nxt.views
  (:require [re-frame.core :as re-frame]))

(defn add-timer []
  [:button
   {:type "input" :on-click #(re-frame/dispatch [:add-timer (rand-int 30)])}
   "Add a Timer"])

(defn timer [[k {:keys [id elapsed state]}] ]
  [:p "Timer " id " has been running for " elapsed " seconds as " state])

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
