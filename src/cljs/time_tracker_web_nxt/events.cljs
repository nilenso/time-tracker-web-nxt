(ns time-tracker-web-nxt.events
  (:require [re-frame.core :as re-frame]
            [time-tracker-web-nxt.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-fx
 :add-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers (keyword (str "timer" timer-id))]
                  {:id      timer-id
                   :elapsed 0
                   :state :paused})
    :dispatch [:start-timer timer-id]}))

(re-frame/reg-event-db
 :inc-timer-dur
 (fn [db [_ timer-id]]
   (if (= :running
          (get-in db [:timers (keyword (str "timer" timer-id)) :state]))
     (update-in db [:timers (keyword (str "timer" timer-id)) :elapsed]
                inc)
     db)))

(re-frame/reg-event-fx
 :start-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers (keyword (str "timer" timer-id)) :state]
                  :running)
    :set-clock timer-id}))

(re-frame/reg-event-db
 :add-interval
 (fn [db [_ timer-id interval-id]]
   (assoc-in db [:intervals (keyword (str "timer" timer-id))] interval-id)))

(re-frame/reg-fx
 :set-clock
 (fn [timer-id]
   (let [interval-id (js/setInterval #(re-frame/dispatch [:inc-timer-dur timer-id]) 1000)]
     (re-frame/dispatch [:add-interval timer-id interval-id]))))

(re-frame/reg-fx
 :clear-clock
 (fn [interval-id]
   (js/clearInterval interval-id)))

(re-frame/reg-event-fx
 :stop-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   (let [interval-id ((keyword (str "timer" timer-id)) (:intervals db))]
     {:db (->
           db
           (assoc-in [:timers (keyword (str "timer" timer-id)) :state]
                     :paused)
           (update-in [:intervals] dissoc (keyword (str "timer" timer-id))))
      :clear-clock interval-id})))
