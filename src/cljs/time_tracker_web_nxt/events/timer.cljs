(ns time-tracker-web-nxt.events.timer
  (:require
   [cljs-time.coerce :as t-coerce]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.events.ws :as ws-events]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store tt-reg-event-db tt-reg-event-fx]]
   [time-tracker-web-nxt.utils :as utils]))

(defn start-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  {:db        (assoc-in db [:timers id :state] :running)
   :set-clock id})

(defn resume-timer [{:keys [db] :as cofx} [_ timer-id]]
  (let [[_ socket] (:conn db)]
    {:db        (assoc-in db [:timers timer-id :state] :running)
     :set-clock timer-id
     :send   [{:command  "start-timer"
               :timer-id timer-id} socket]}))

(defn tick-running-timer [{:keys [db] :as cofx} _]
  (let [running?         (fn [timer] (= :running (:state timer)))
        running-timer-id (first
                          (for [[k v] (:timers db)
                                :when (running? v)]
                            k))
        fx-map           {:db (assoc db :boot-from-local-storage? false)}]
    (if running-timer-id
      (assoc fx-map :set-clock running-timer-id)
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
  (let [timer-id    id
        interval-id (get (:intervals db) timer-id)
        [_ socket]  (:conn db)]
    {:db          (-> db
                     (assoc-in [:timers timer-id :state] :paused)
                     (assoc-in [:timers timer-id :duration] duration)
                     (assoc-in [:timers timer-id :elapsed] duration)
                     (update-in [:intervals] dissoc timer-id))
     :clear-clock interval-id
     :send     [{:command  "stop-timer"
                 :timer-id timer-id} socket]}))

(defn set-clock [timer-id]
  (let [dispatch-fn #(rf/dispatch [:increment-timer-duration timer-id])
        interval-id (js/setInterval dispatch-fn 1000)]
    (rf/dispatch [:add-interval timer-id interval-id])))

(defn clear-clock [interval-id]
  (js/clearInterval interval-id))

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
   :resume-timer
   [->local-store]
   resume-timer)

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

  (rf/reg-fx :set-clock set-clock)
  (rf/reg-fx :clear-clock clear-clock))
