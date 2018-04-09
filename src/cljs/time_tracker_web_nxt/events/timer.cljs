(ns time-tracker-web-nxt.events.timer
  (:require
   [cljs-time.coerce :as t-coerce]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.events.ws :as ws-events]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector tt-reg-event-db tt-reg-event-fx]]
   [time-tracker-web-nxt.utils :as utils]
   [taoensso.timbre :as timbre]))

(defn start-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  {:db   (assoc-in db [:timers id :state] :running)
   :tick {:action :start
          :id     id
          :event  [:increment-timer-duration id]}})

(defn trigger-create-timer
  [{:keys [db current-timestamp] :as cofx} [_ timer-project {:keys [elapsed-hh elapsed-mm elapsed-ss notes] :as data}]]
  (let [[_ socket] (:conn db)
        timer-date (str (:timer-date db))
        elapsed    (utils/->seconds elapsed-hh elapsed-mm elapsed-ss)]
    {:db (assoc db :show-create-timer-widget? false)
     :send [{:command "create-timer"
             :project-id (js/parseInt (:id timer-project) 10)
             :created-time (utils/datepicker-date->epoch timer-date current-timestamp)
             :notes notes
             :duration elapsed} socket]}))

(defn trigger-start-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  (let [[_ socket] (:conn db)]
    {:send      [{:command  "start-timer"
                  :timer-id id} socket]}))

(defn trigger-update-timer
  [{:keys [db] :as cofx} [_ {:keys [id elapsed-hh elapsed-mm elapsed-ss notes] :as new}]]
  (let [duration   (utils/->seconds elapsed-hh elapsed-mm elapsed-ss)
        [_ socket] (:conn db)]
    {:send [{:command  "update-timer"
             :timer-id id
             :duration duration
             :notes    notes} socket]}))

(defn trigger-stop-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  (let [[_ socket]  (:conn db)]
    {:send [{:command "stop-timer"
             :timer-id id} socket]}))

(defn stop-or-update-timer
  [{:keys [db] :as cofx} [_ {:keys [id duration notes]}]]
  {:db   (-> db
             (assoc-in [:timers id :state] :paused)
             (assoc-in [:timers id :duration] duration)
             (assoc-in [:timers id :notes] notes))
   :tick {:action :stop :id id}})

(defn trigger-delete-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  (let [[_ socket]  (:conn db)]
    {:send [{:command "delete-timer"
             :timer-id id} socket]}))

(defn delete-timer
  [{:keys [db] :as cofx} [_ {:keys [id]}]]
  {:db (update-in db [:timers] dissoc id)})

(defn timer-date-changed
  [{:keys [db] :as cofx} [_ key date]]
  (let [auth-token (get-in db [:user :token])]
    {:db       (assoc db key date)
     :dispatch [:get-timers auth-token (t-coerce/from-date date)]}))

(defn init []
  (rf/reg-event-db
   :increment-timer-duration
   (fn [db [_ timer-id]]
     (if (= :running (get-in db [:timers timer-id :state]))
       (update-in db [:timers timer-id :duration] inc)
       db)))

  (rf/reg-event-db
   :add-interval
   [db-spec-inspector]
   (fn [db [_ timer-id interval-id]]
     (assoc-in db [:intervals timer-id] interval-id)))

  (rf/reg-event-db
   :add-timer-to-db
   [db-spec-inspector]
   (fn [db [_ timer]]
     (-> db (assoc-in [:timers (:id timer)]
                      (assoc timer
                             :state :paused)))))

  (tt-reg-event-fx
   :trigger-create-timer
   [(rf/inject-cofx :current-timestamp)]
   trigger-create-timer)

  (tt-reg-event-fx
   :start-timer
   [db-spec-inspector]
   start-timer)

  (rf/reg-event-fx
   :trigger-start-timer
   trigger-start-timer)

  (tt-reg-event-fx
   :trigger-update-timer
   [db-spec-inspector]
   trigger-update-timer)

  (tt-reg-event-fx
   :trigger-stop-timer
   [db-spec-inspector]
   trigger-stop-timer)

  (tt-reg-event-fx
   :stop-or-update-timer
   [db-spec-inspector]
   stop-or-update-timer)

  (tt-reg-event-fx
   :trigger-delete-timer
   [db-spec-inspector]
   trigger-delete-timer)

  (tt-reg-event-fx
   :delete-timer
   [db-spec-inspector]
   delete-timer)

  (rf/reg-event-fx
   :timer-date-changed
   [db-spec-inspector]
   timer-date-changed)

  (rf/reg-fx
   :tick
   (let [live-intervals (atom {})]
     (fn [{:keys [action id event]}]
       (if (= action :start)
         (do
           (swap! live-intervals
                  (fn [m timer-id]
                    (if-not (get m timer-id)
                      (assoc m timer-id
                             (js/setInterval #(rf/dispatch event) 1000))
                      (do (timbre/debug "Tried to start setInterval twice for " timer-id)
                          @live-intervals)))
                  id))
         (do
           (js/clearInterval (get @live-intervals id))
           (swap! live-intervals dissoc id)))))))
