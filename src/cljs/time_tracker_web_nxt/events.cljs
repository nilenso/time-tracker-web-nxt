(ns time-tracker-web-nxt.events
  (:require [re-frame.core :as re-frame]
            [time-tracker-web-nxt.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :add-timer
 (fn [db [_ timer-id]]
   (assoc-in db [:timers (keyword (str "timer" timer-id))]
             {:id      timer-id
              :elapsed 0})))
