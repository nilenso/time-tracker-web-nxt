(ns time-tracker-web-nxt.events
  (:require [re-frame.core :as re-frame]
            [time-tracker-web-nxt.db :as db]))
(defn- timer-key
  [timer-id]
  (keyword (str "timer" timer-id)))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-fx
 :add-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers (timer-key timer-id)]
                  {:id      timer-id
                   :elapsed 0
                   :state :paused})
    :dispatch [:start-timer timer-id]}))

(re-frame/reg-event-db
 :inc-timer-dur
 (fn [db [_ timer-id]]
   (if (= :running
          (get-in db [:timers (timer-key timer-id) :state]))
     (update-in db [:timers (timer-key timer-id) :elapsed]
                inc)
     db)))

(re-frame/reg-event-fx
 :start-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers (timer-key timer-id) :state]
                  :running)
    :set-clock timer-id}))

(re-frame/reg-event-db
 :add-interval
 (fn [db [_ timer-id interval-id]]
   (assoc-in db [:intervals (timer-key timer-id)] interval-id)))

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
   (let [tk (timer-key timer-id)
         interval-id (tk (:intervals db))]
     {:db (->
           db
           (assoc-in [:timers tk :state]
                     :paused)
           (update-in [:intervals] dissoc tk))
      :clear-clock interval-id})))
