(ns time-tracker-web-nxt.events
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [ajax.core :as ajax]
   [cljs-time.coerce :as t-coerce]
   [cljs-time.core :as t-core]
   [cljs-time.format :as t-format]
   [cljs.core.async :as async :refer [chan put! <! close! alts! take! timeout]]
   [cljs.spec.alpha :as s]
   [day8.re-frame.http-fx]
   [hodgepodge.core :refer [local-storage set-item get-item clear!]]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre :refer-macros [log  trace  debug  info  warn  error  fatal  report
                                              logf tracef debugf infof warnf errorf fatalf reportf
                                              spy get-env]]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.config :as config]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.handlers :as handlers]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store tt-reg-event-db tt-reg-event-fx]]
   [time-tracker-web-nxt.utils :as utils]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]))


(defn init []

  ;;; ===================== cofx's ========================

  ;; This coeffect is injected into the `initialize-db` event
  ;; to add the db state stored in local-storage to the coeffects
  ;; of the event handler.

  (rf/reg-cofx
   :local-store-app-db
   (fn [cofx _]
     (let [db (-> local-storage (get-item "db") cljs.reader/read-string)]
       (assoc cofx
              :local-store-app-db
              (if db
                (assoc db :boot-from-local-storage? true)
                nil)))))

  (rf/reg-cofx
   :current-timestamp
   (fn [cofx _]
     (assoc cofx :current-timestamp (t-core/now))))

;;; ================== event-db's ==================

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

  (tt-reg-event-db
   :show-widget
   [db-spec-inspector ->local-store]
   (fn [db _]
     (assoc db :show-create-timer-widget? true)))

  (rf/reg-event-db
   :hide-widget
   (fn [db _]
     (assoc db :show-create-timer-widget? false)))

  (rf/reg-event-db
   :projects-retrieved
   [db-spec-inspector ->local-store]
   handlers/projects-retrieved)

  (rf/reg-event-db
   :timers-retrieved
   [db-spec-inspector ->local-store]
   handlers/timers-retrieved)

;;; ================== COEFFECTS ====================

  (rf/reg-event-fx
   :initialize-db
   [(rf/inject-cofx :local-store-app-db)]
   (fn  [{:keys [db local-store-app-db]} cofx]
     {:db (or local-store-app-db db/default-db)}))

  (tt-reg-event-fx
   :start-timer
   [db-spec-inspector ->local-store]
   handlers/start-timer)

  (rf/reg-event-fx
   :resume-timer
   [->local-store]
   handlers/resume-timer)

  (rf/reg-event-fx
   :tick-running-timer
   handlers/tick-running-timer)

  (rf/reg-event-fx
   :update-timer
   [db-spec-inspector ->local-store]
   handlers/update-timer)

  (tt-reg-event-fx
   :stop-timer
   [db-spec-inspector ->local-store]
   handlers/stop-timer)

  (rf/reg-event-fx
   :log-in
   [db-spec-inspector ->local-store]
   handlers/login)

  (rf/reg-event-fx :log-out handlers/logout)

  (rf/reg-event-fx
   :timer-date-changed
   [db-spec-inspector ->local-store]
   handlers/timer-date-changed)

  (rf/reg-event-fx
   :create-ws-connection
   (fn [cofx [_ google-auth-token]]
     {:ws/create google-auth-token}))

  (rf/reg-event-fx
   :save-connection
   (fn [{:keys [db] :as cofx} [_ sock]]
     {:db (assoc db :conn sock)
      :ws/ping sock}))

  (tt-reg-event-fx
   :create-and-start-timer
   [(rf/inject-cofx :current-timestamp) ->local-store]
   handlers/ws-create-and-start-timer)

  (rf/reg-event-fx :api/request-failed handlers/http-failure)
  (rf/reg-event-fx :api/get-projects handlers/get-projects)
  (rf/reg-event-fx :api/get-timers handlers/get-timers)

;;; =================== EFFECTS ========================

  (rf/reg-fx :set-clock handlers/set-clock)
  (rf/reg-fx :clear-clock handlers/clear-clock)

  (rf/reg-fx
   :clear-local-storage
   (fn []
     (clear! local-storage)))

  (rf/reg-fx
   :error
   (fn [e]
     (timbre/error e)))

  (rf/reg-fx :ws/send handlers/ws-send)
  (rf/reg-fx :ws/create handlers/ws-create)
  (rf/reg-fx :ws/close handlers/ws-close)
  (rf/reg-fx :ws/ping handlers/ws-ping))

(init)
