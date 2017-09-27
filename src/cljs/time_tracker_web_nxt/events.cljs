(ns time-tracker-web-nxt.events
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [chan put! <! close! alts! take! timeout]]
   [re-frame.core :as re-frame]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.env-vars :as env]
   [time-tracker-web-nxt.auth :as auth]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [cljs-time.core :as t-core]
   [cljs-time.coerce :as t-coerce]
   [taoensso.timbre :as timbre :refer-macros [log  trace  debug  info  warn  error  fatal  report
                                              logf tracef debugf infof warnf errorf fatalf reportf
                                              spy get-env]]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-fx
 :add-timer
 (fn [{:keys [db] :as cofx} [_ timer-project timer-note]]
   (let [[_ socket] (:conn db)]
     {:ws-send [{:command "create-and-start-timer"
                 :project-id (js/parseInt (:id timer-project))
                 :created-time (t-coerce/to-epoch (t-core/now))
                 :notes timer-note} socket]})))

(re-frame/reg-event-db
 :inc-timer-dur
 (fn [db [_ timer-id]]
   (if (= :running
          (get-in db [:timers timer-id :state]))
     (update-in db [:timers timer-id :elapsed]
                inc)
     db)))

(re-frame/reg-event-fx
 :start-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers timer-id :state] :running)
    :set-clock timer-id}))

(re-frame/reg-event-fx
 :resume-timer
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   (let [[_ socket] (:conn db)] 
     {:db (assoc-in db [:timers timer-id :state] :running)
      :set-clock timer-id
      :ws-send [{:command "start-timer"
                 :timer-id timer-id} socket]})))

(re-frame/reg-event-db
 :add-interval
 (fn [db [_ timer-id interval-id]]
   (assoc-in db [:intervals timer-id] interval-id)))

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

(re-frame/reg-event-fx
 :update-timer
 (fn [{:keys [db] :as cofx} [_ timer-id {:keys [elapsed-hh elapsed-mm elapsed-ss notes] :as new}]]
   (let [elapsed (+ (* 60 60 elapsed-hh)
                    (* 60 elapsed-mm)
                    elapsed-ss)
         new (assoc new :elapsed elapsed)
         ori (-> db :timers (get timer-id) (select-keys [:notes :elapsed]))
         new-map (merge ori new)
         [_ socket] (:conn db)]
     {:db (-> db
              (assoc-in [:timers timer-id :elapsed] (:elapsed new-map))
              (assoc-in [:timers timer-id :notes] (:notes new-map)))
      :ws-send [{:command "update-timer"
                 :timer-id timer-id
                 :duration elapsed 
                 :notes notes} socket]})))

(re-frame/reg-event-fx
 :log-in
 (fn [{:keys [db] :as cofx} [_ user]]
   (let [user-profile (auth/user-profile user)]
     {:db          (assoc db :user user-profile)
      :create-conn (:token user-profile)
      :dispatch-n  [[:list-all-projects (:token user-profile)]
                    [:list-all-timers (:token user-profile)]]})))

(re-frame/reg-event-db
 :log-out
 (fn [db [_ user]]
   (assoc db :user nil)))

(defn message-handler [{:keys [id started-time duration type] :as data}]
  (if (= "create" type)
    (do
      (info "Create: " data)
      (re-frame/dispatch [:add-timer-to-db (dissoc data :type)])
      (debug "Starting timer: " id)
      (re-frame/dispatch [:start-timer id]))
    (if (= "update" type)
      (do
        (info "Update: " data)
        (if (and (nil? started-time) (> duration 0))
          (do
            (debug "Stopping timer: " id)
            (re-frame/dispatch [:stop-timer data]))
          ))
      (debug "Default: " data))))

(defn timer-state
  [{:keys [duration] :as timer}]
  (if (= 0 duration) :running :paused))

(re-frame/reg-event-fx
 :add-timer-to-db
 (fn [{:keys [db] :as cofx} [_ timer]]
   (let [timer-id (:id timer)]
     {:db (-> db
              (assoc-in [:timers timer-id]
                        (assoc timer :state (timer-state timer))))})))

(re-frame/reg-fx
 :create-conn
 (fn [goog-auth-id]
   (let [response-chan (chan)
         handlers {:on-message #(do 
                                  (message-handler (fmt/read fmt/json (.-data %)))
                                  (put! response-chan (fmt/read fmt/json (.-data %))))}
         conn (ws/create (:conn-url env/env) handlers)]
     (go
       (let [data (<! response-chan)]
         (if (= "ready" (:type data))
           (do
             (ws/send conn (clj->js {:command "authenticate" :token goog-auth-id}) fmt/json)
             (if (= "success" (:auth-status (<! response-chan)))
               (re-frame/dispatch [:save-connection [response-chan conn]])
               (throw (ex-info "Authentication Failed" {}))))
           ;; TODO: Retry server connection
           (throw (ex-info "Server not ready" {}))))))))

(re-frame/reg-event-fx
 :save-connection
 (fn [{:keys [db] :as cofx} [_ sock]]
   {:dispatch [:add-db :conn sock]
    :heartbeat sock}))

(re-frame/reg-fx
 :heartbeat
 (fn [[_ sock]]
   (go
     (while (= :open (ws/status sock))
       (<! (timeout 10000))
       (ws/send sock (clj->js {:command "ping"}) fmt/json)))))
;; ========== API ============

(re-frame/reg-event-db
 :add-db
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :add-timers-to-db
 (fn [db [_ timers]]
   (let [state #(timer-state %)
         timers-map (reduce #(assoc %1 (:id %2)
                                    (-> %2 
                                        (assoc :state (state %2))
                                        (assoc :elapsed (:duration %2)))) {} timers)]
     (assoc db :timers timers-map))))

(re-frame/reg-event-db
 :http-failure
 (fn [_ [_ e]]
   (error e)))

;; Possible Solution: https://github.com/JulianBirch/cljs-ajax/issues/93
(re-frame/reg-event-fx
 :list-all-projects
 (fn [cofx [_ auth-token]]
   {:http-xhrio {:method :get
                 :uri "/api/projects/"
                 :timeout 8000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :headers {"Authorization" (str "Bearer " auth-token)
                           "Access-Control-Allow-Origin" "*"}
                 :on-success [:add-db :projects]
                 :on-failure [:http-failure]}}))

(re-frame/reg-event-fx
 :list-all-timers
 (fn [cofx [_ auth-token]]
   (let [today-epoch    (t-coerce/to-epoch (t-core/today-at-midnight))
         tomorrow-epoch (-> (t-core/today-at-midnight)
                            (t-core/plus (t-core/days 1))
                            t-coerce/to-epoch)]
     {:http-xhrio {:method          :get
                   :uri             "/api/timers/"
                   :params          {:start 0
                                     :end   tomorrow-epoch}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :headers         {"Authorization"               (str "Bearer " auth-token)
                                     "Access-Control-Allow-Origin" "*"}
                   :on-success      [:add-timers-to-db]
                   :on-failure      [:http-failure]}})))

;; ====== Websocket ==========

(re-frame/reg-fx
 :ws-send
 (fn [[data socket]]
   (ws/send socket (clj->js data) fmt/json)))
