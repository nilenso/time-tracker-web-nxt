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
 :start-timer
 (fn [db [_ timer-id]]
   (assoc-in db [:timers (keyword (str "timer" timer-id)) :state]
              :running)))
