(ns time-tracker-web-nxt.events.timer
  (:require
   [cljs-time.coerce :as t-coerce]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.events.ws :as ws-events]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store tt-reg-event-db tt-reg-event-fx]]
   [time-tracker-web-nxt.utils :as utils]))

(defn start-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  (when-not (= (get-in db [:timers id :state]) :running)
    {:db   (assoc-in db [:timers id :state] :running)
     :tick {:action :start
            :id     id
            :event  [:increment-timer-duration id]}}))

(defn trigger-start-timer [{:keys [db] :as cofx} [_ id]]
  (let [[_ socket] (:conn db)]
    {:send      [{:command  "start-timer"
                  :timer-id id} socket]}))

(defn tick-running-timer [{:keys [db] :as cofx} _]
  (let [running?         (fn [timer] (= :running (:state timer)))
        running-timer-id (first
                          (for [[k v] (:timers db)
                                :when (running? v)]
                            k))
        fx-map           {:db (assoc db :boot-from-local-storage? false)}]
    (if running-timer-id
      (assoc fx-map :tick {:action :start
                           :id     running-timer-id
                           :event  [:increment-timer-duration running-timer-id]})
      fx-map)))

(defn update-timer
  [{:keys [db] :as cofx} [_ timer-id {:keys [elapsed-hh elapsed-mm elapsed-ss notes] :as new}]]
  (let [elapsed    (utils/->seconds elapsed-hh elapsed-mm elapsed-ss)
        new        (assoc new :elapsed elapsed)
        ori        (-> db
                      :timers
                      (get timer-id)
                      (select-keys [:notes :elapsed]))
        new-map    (merge ori new)
        [_ socket] (:conn db)]
    {:db      (-> db
                 (assoc-in [:timers timer-id :elapsed]
                           (:elapsed new-map))
                 (assoc-in [:timers timer-id :notes]
                           (:notes new-map)))
     :send [{:command  "update-timer"
             :timer-id timer-id
             :duration elapsed
             :notes    notes} socket]}))

(defn stop-timer [{:keys [db] :as cofx} [_ {:keys [id duration]}]]
  (let [[_ socket]  (:conn db)]
    {:db   (-> db
              (assoc-in [:timers id :state] :paused)
              (assoc-in [:timers id :duration] duration)
              (assoc-in [:timers id :elapsed] duration))
     :tick {:action :stop :id id}
     :send [{:command  "stop-timer"
             :timer-id id} socket]}))

(defn timer-date-changed
  [{:keys [db] :as cofx} [_ key date]]
  (let [auth-token (get-in db [:user :token])]
    {:db       (assoc db key date)
     :dispatch [:get-timers auth-token (t-coerce/from-date date)]}))

(defn init []
  (rf/reg-event-db
   :increment-timer-duration
   [->local-store]
   (fn [db [_ timer-id]]
     (if (= :running (get-in db [:timers timer-id :state]))
       (update-in db [:timers timer-id :elapsed] inc)
       db)))

  (rf/reg-event-db
   :add-interval
   [db-spec-inspector ->local-store]
   (fn [db [_ timer-id interval-id]]
     (assoc-in db [:intervals timer-id] interval-id)))

  (rf/reg-event-db
   :add-timer-to-db
   [db-spec-inspector ->local-store]
   (fn [db [_ timer]]
     (-> db (assoc-in [:timers (:id timer)]
                      (assoc timer :state (utils/timer-state timer))))))

  (tt-reg-event-fx
   :create-and-start-timer
   [(rf/inject-cofx :current-timestamp) ->local-store]
   ws-events/ws-create-and-start-timer)

  (tt-reg-event-fx
   :start-timer
   [db-spec-inspector ->local-store]
   start-timer)

  (rf/reg-event-fx
   :trigger-start-timer
   [->local-store]
   trigger-start-timer)

  (rf/reg-event-fx
   :tick-running-timer
   tick-running-timer)

  (tt-reg-event-fx
   :update-timer
   [db-spec-inspector ->local-store]
   update-timer)

  (tt-reg-event-fx
   :stop-timer
   [db-spec-inspector ->local-store]
   stop-timer)

  (rf/reg-event-fx
   :timer-date-changed
   [db-spec-inspector ->local-store]
   timer-date-changed)

  (rf/reg-fx
   :tick
   (let [live-intervals (atom {})]
     (fn [{:keys [action id event]}]
       (if (= action :start)
         (do
           (swap! live-intervals assoc id (js/setInterval #(rf/dispatch event) 1000)))
         (do
           (js/clearInterval (get @live-intervals id))
           (swap! live-intervals dissoc id)))))))
