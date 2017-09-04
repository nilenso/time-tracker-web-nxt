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
  (let [minutes (-> (quot elapsed-seconds 60)
                    (mod 60))
        hours (quot minutes 60)
        seconds (mod elapsed-seconds 60)]
    {:hh hours :mm minutes :ss seconds}))

(defn time-display [elapsed-seconds]
  (let [minutes (-> (quot elapsed-seconds 60)
                    (mod 60))
        hours (quot minutes 60)
        seconds (mod elapsed-seconds 60)]
    (gs/format "%02d:%02d:%02d" hours minutes seconds)
    #_celapsed-seconds))

(defn timer-display [id elapsed project state note edit-timer?]
  [:div "Timer " id " for project " project
   " has been running for " (time-display elapsed) " seconds as " state
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

(defn timer-display-editable [id elapsed-hh elapsed-mm elapsed-ss
                              project state note edit-timer?]
  (let [changes (reagent/atom {:note note
                               :elapsed-hh elapsed-hh
                               :elapsed-mm elapsed-mm
                               :elapsed-ss elapsed-ss})]
    (fn [id elapsed-hh elapsed-mm elapsed-ss
         project state note edit-timer]
      [:div "Timer " id " for project " project
       " has been running for "
       [:input {:value (:elapsed-hh @changes)
                :on-change (fn [e]
                             (let [elap-hh (-> e .-target .-value)]
                               (swap! changes assoc :elapsed-hh (if (empty? elap-hh)
                                                                  0
                                                                  (js/parseInt elap-hh)))))}]
       [:input {:value (:elapsed-mm @changes)
                :on-change (fn [e]
                             (let [elap-mm (-> e .-target .-value)]
                               (swap! changes assoc :elapsed-mm (if (empty? elap-mm)
                                                                  0
                                                                  (js/parseInt elap-mm)))))}]
       [:input {:value (:elapsed-ss @changes)
                :on-change (fn [e]
                             (let [elap-ss (-> e .-target .-value)]
                               (swap! changes assoc :elapsed-ss (if (empty? elap-ss)
                                                                  0
                                                                  (js/parseInt elap-ss)))))}]
       [:textarea {:value (:note @changes)
                   :on-change #(swap! changes assoc :note (-> % .-target .-value))}]
       [:button {:on-click #(reset! edit-timer? false)} "Cancel"]
       [:button {:on-click #(do (reset! edit-timer? false)
                                (re-frame/dispatch [:update-timer id @changes]))} "Update"]])))

(defn timer [{:keys [id elapsed state project note]}]
  (let [edit-timer? (reagent/atom false)]
    (fn [{:keys [id elapsed state project note]}]
      (let [elapsed-map (split-time elapsed)]
        (if @edit-timer?
          [timer-display-editable id (:hh elapsed-map) (:mm elapsed-map) (:ss elapsed-map)
           project state note edit-timer?]
          [timer-display id elapsed project state note edit-timer?])))))

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
