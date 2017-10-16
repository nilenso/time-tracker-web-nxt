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
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store]]
   [time-tracker-web-nxt.utils :as utils]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]))

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

(rf/reg-event-fx
 :initialize-db
 [(rf/inject-cofx :local-store-app-db)]
 (fn  [{:keys [db local-store-app-db]} cofx]
   {:db (or local-store-app-db db/default-db)}))

(rf/reg-event-db
 :show-add-timer-widget
 [db-spec-inspector ->local-store]
 (fn [db [_ state]]
   (assoc db :show-add-timer-widget? state)))

(rf/reg-event-fx
 :add-timer
 [->local-store]
 (fn [{:keys [db] :as cofx} [_ timer-project timer-note]]
   (let [[_ socket] (:conn db)
         timer-date (str (:timer-date db))
         now (t-core/now)]
     (info "Create timer for " (utils/timer-created-time timer-date now))
     {:ws-send [{:command "create-and-start-timer"
                 :project-id (js/parseInt (:id timer-project))
                 :created-time (utils/timer-created-time timer-date now)
                 :notes timer-note} socket]})))

(rf/reg-event-db
 :inc-timer-dur
 [->local-store]
 (fn [db [_ timer-id]]
   (if (= :running (get-in db [:timers timer-id :state]))
     (update-in db [:timers timer-id :elapsed] inc)
     db)))

(rf/reg-event-fx
 :start-timer
 [db-spec-inspector ->local-store]
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers timer-id :state] :running)
    :set-clock timer-id}))

(rf/reg-event-fx
 :resume-timer
 [->local-store]
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   (let [[_ socket] (:conn db)]
     {:db (assoc-in db [:timers timer-id :state] :running)
      :set-clock timer-id
      :ws-send [{:command "start-timer"
                 :timer-id timer-id} socket]})))

(rf/reg-event-fx
 :tick-running-timer
 (fn [{:keys [db] :as cofx} _]
   (let [running? (fn [timer] (= :running (:state timer)))
         running-timer-id (first
                           (for [[k v] (:timers db) :when (running? v)]
                             k))
         fx-map {:db (assoc db :boot-from-local-storage? false)}]
     (if running-timer-id
       (assoc fx-map :set-clock running-timer-id)
       fx-map))))

(rf/reg-event-db
 :add-interval
 [db-spec-inspector ->local-store]
 (fn [db [_ timer-id interval-id]]
   (assoc-in db [:intervals timer-id] interval-id)))

(rf/reg-fx
 :set-clock
 (fn [timer-id]
   (let [dispatch-fn #(rf/dispatch [:inc-timer-dur timer-id])
         interval-id (js/setInterval dispatch-fn 1000)]
     (rf/dispatch [:add-interval timer-id interval-id]))))

(rf/reg-fx
 :clear-clock
 (fn [interval-id]
   (js/clearInterval interval-id)))

(rf/reg-event-fx
 :stop-timer
 [db-spec-inspector ->local-store]
 (fn [{:keys [db] :as cofx} [_ {:keys [id duration]}]]
   (let [timer-id id
         interval-id (get (:intervals db) timer-id)
         [_ socket] (:conn db)]
     {:db (->
           db
           (assoc-in [:timers timer-id :state] :paused)
           (assoc-in [:timers timer-id :duration] duration)
           (assoc-in [:timers timer-id :elapsed] duration)
           (update-in [:intervals] dissoc timer-id))
      :clear-clock interval-id
      :ws-send [{:command "stop-timer"
                 :timer-id timer-id} socket]})))

(rf/reg-event-fx
 :update-timer
 [db-spec-inspector ->local-store]
 (fn [{:keys [db] :as cofx} [_ timer-id {:keys [elapsed-hh elapsed-mm elapsed-ss notes] :as new}]]
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
      :ws-send [{:command  "update-timer"
                 :timer-id timer-id
                 :duration elapsed
                 :notes    notes} socket]})))

(rf/reg-event-fx
 :log-in
 [db-spec-inspector ->local-store]
 (fn [{:keys [db] :as cofx} [_ user]]
   (let [user-profile (auth/user-profile user)]
     {:db          (assoc db :user user-profile)
      :dispatch-n  [[:list-all-projects (:token user-profile)]
                    [:list-all-timers (:token user-profile) (t-core/today-at-midnight)]]})))

(rf/reg-fx
 :clear-local-storage
 #(clear! local-storage))

(rf/reg-event-fx
 :log-out
 (fn [{:keys [db] :as cofx} [_ user]]
   (let [[_ socket] (:conn db)]
     {:db db/default-db
      :ws-close-connection socket
      :clear-local-storage nil})))

(defn message-handler [{:keys [id started-time duration type] :as data}]
  (if (= "create" type)
    (do
      (info "Create: " data)
      (rf/dispatch [:add-timer-to-db (dissoc data :type)])
      (debug "Starting timer: " id)
      (rf/dispatch [:start-timer id]))
    (if (= "update" type)
      (do
        (info "Update: " data)
        (if (and (nil? started-time) (> duration 0))
          (do
            (debug "Stopping timer: " id)
            (rf/dispatch [:stop-timer data]))))
      (debug "Default: " data))))

(defn timer-state
  [{:keys [duration] :as timer}]
  (if (= 0 duration) :running :paused))

(rf/reg-event-fx
 :timer-date-changed
 [db-spec-inspector ->local-store]
 (fn [{:keys [db] :as cofx} [_ key date]]
   (let [auth-token (get-in db [:user :token])]
     {:db (assoc db key date)
      :dispatch [:list-all-timers auth-token (t-coerce/from-date date)]})))

(rf/reg-event-db
 :add-timer-to-db
 [db-spec-inspector ->local-store]
 (fn [db [_ timer]]
   (-> db (assoc-in [:timers (:id timer)]
                    (assoc timer :state (timer-state timer))))))

(rf/reg-event-fx
 :create-ws-connection
 (fn [cofx [_ google-auth-token]]
   {:create-conn google-auth-token}))

(defn ws-create [goog-auth-id]
  (let [response-chan (chan)
        handlers {:on-message #(do
                                 (message-handler (fmt/read fmt/json (.-data %)))
                                 (put! response-chan (fmt/read fmt/json (.-data %))))}
        conn (ws/create (:conn-url config/env) handlers)]
    (go
      (let [data (<! response-chan)]
        (if (= "ready" (:type data))
          (do
            (ws/send conn (clj->js {:command "authenticate" :token goog-auth-id}) fmt/json)
            (if (= "success" (:auth-status (<! response-chan)))
              (rf/dispatch [:save-connection [response-chan conn]])
              (throw (ex-info "Authentication Failed" {}))))
          ;; TODO: Retry server connection
          (throw (ex-info "Server not ready" {})))))))

(rf/reg-fx :create-conn ws-create)

(defn ws-close [socket]
  (.log js/console "Closing websocket connection")
  (ws/close socket))

(rf/reg-fx :ws-close-connection ws-close)

(rf/reg-event-fx
 :save-connection
 (fn [{:keys [db] :as cofx} [_ sock]]
   {:dispatch [:add-db :conn sock]
    :heartbeat sock}))

(defn ws-ping [[_ sock]]
  (go
    (while (= :open (ws/status sock))
      (<! (timeout 10000))
      (ws/send sock (clj->js {:command "ping"}) fmt/json))))

(rf/reg-fx :heartbeat ws-ping)

;; ========== API ============

(rf/reg-event-db
 :add-db
 [db-spec-inspector ->local-store]
 (fn [db [_ k v]]
   (assoc db k v)))

(rf/reg-event-db
 :add-timers-to-db
 [db-spec-inspector ->local-store]
 (fn [db [_ timers]]
   (let [state #(timer-state %)
         timers-map (reduce #(assoc %1 (:id %2)
                                    (-> %2
                                        (assoc :state (state %2))
                                        (assoc :elapsed (:duration %2)))) {} timers)]
     (assoc db :timers timers-map))))

(rf/reg-event-db
 :http-failure
 (fn [_ [_ e]]
   (error e)))

(defn get-projects [cofx [_ auth-token]]
   {:http-xhrio {:method :get
                 :uri "/api/projects/"
                 :timeout 8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :headers {"Authorization" (str "Bearer " auth-token)
                           "Access-Control-Allow-Origin" "*"}
                 :on-success [:add-db :projects]
                 :on-failure [:http-failure]}})

(rf/reg-event-fx :list-all-projects get-projects)

(defn get-timers [cofx [_ auth-token timer-date]]
  (let [start-epoch (t-coerce/to-epoch timer-date)
        end-epoch   (-> timer-date
                        (t-core/plus (t-core/days 1))
                        t-coerce/to-epoch)]
    {:http-xhrio {:method          :get
                  :uri             "/api/timers/"
                  :params          {:start start-epoch
                                    :end   end-epoch}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :headers         {"Authorization"               (str "Bearer " auth-token)
                                    "Access-Control-Allow-Origin" "*"}
                  :on-success      [:add-timers-to-db]
                  :on-failure      [:http-failure]}}))

(rf/reg-event-fx :list-all-timers get-timers)

;; ====== Websocket ==========

(defn ws-send [[data socket]]
  (ws/send socket (clj->js data) fmt/json))

(rf/reg-fx :ws-send ws-send)
