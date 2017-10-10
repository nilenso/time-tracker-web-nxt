(ns time-tracker-web-nxt.events
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [chan put! <! close! alts! take! timeout]]
   [cljs.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.config :as config]
   [time-tracker-web-nxt.auth :as auth]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [cljs-time.core :as t-core]
   [cljs-time.coerce :as t-coerce]
   [cljs-time.format :as t-format]
   [clojure.string :as clj-s]
   [taoensso.timbre :as timbre :refer-macros [log  trace  debug  info  warn  error  fatal  report
                                              logf tracef debugf infof warnf errorf fatalf reportf
                                              spy get-env]]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "Spec failed => " (s/explain-str a-spec db)) {}))))

;; This Interceptor runs `check-and-throw` `after` the event handler has finished, checking
;; the value for `app-db` against a spec.
;; If the event handler corrupted the value for `app-db` an exception will be
;; thrown. This helps us detect event handler bugs early.
;; Because all state is held in `app-db`, we are effectively validating the
;; ENTIRE state of the application after each event handler runs.  All of it.
(def db-spec-inpector (re-frame/after (partial check-and-throw :time-tracker-web-nxt.db/db)))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :show-add-timer-widget
 [db-spec-inpector]
 (fn [db [_ state]]
   (assoc db :show-add-timer-widget? state)))

;; Note:
;; We're doing redundant time conversions for the most common case
;; i.e. today's date. One way to avoid that would have been to
;; compare the timer-date arg with current DateTime. That is always
;; going to differ as timer-date, being selected from a DatePicker
;; doesn'have HH:mm:ss parts.
;; TODO:
;; We might be able to avoid some of this by getting the midnight corresponding
;; to now-datetime and comparing it with timer-datetime.
(defn timer-created-time
  "Takes a date of the form 'Tue Oct 10 2017 11:30:21 GMT+0530 (IST)'
  and returns a corresponding Unix epoch"
  [timer-date now-datetime]
  (let [timer-format "E MMM dd yyyy HH:mm:ss"
        ;; Timer date in db is of the form "Tue Oct 10 2017 11:30:21 GMT+0530 (IST)"
        ;; We choose a format to extract the date in UTC midnight
        ;; To match it with formatter we have to drop the last 15 chars
        timer-datetime (t-format/parse
                        (t-format/formatter timer-format)
                        (clj-s/join (drop-last 15 timer-date)))
        ;; Timer date stores 00:00:00 for HH:mm:ss
        ;; so we need to advance it by a Period corresponding to now
        created-datetime (t-core/plus
                          timer-datetime
                          (t-core/hours (t-core/hour now-datetime))
                          (t-core/minutes (t-core/minute now-datetime))
                          (t-core/seconds (t-core/second now-datetime)))]
    (t-coerce/to-epoch created-datetime)))

(re-frame/reg-event-fx
 :add-timer
 (fn [{:keys [db] :as cofx} [_ timer-project timer-note]]
   (let [[_ socket] (:conn db)
         timer-date (str (:timer-date db))
         now (t-core/now)]
     {:ws-send [{:command "create-and-start-timer"
                 :project-id (js/parseInt (:id timer-project))
                 :created-time (timer-created-time timer-date now)
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
 [db-spec-inpector]
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   {:db (assoc-in db [:timers timer-id :state] :running)
    :set-clock timer-id}))

(re-frame/reg-event-fx
 :resume-timer
 [db-spec-inpector]
 (fn [{:keys [db] :as cofx} [_ timer-id]]
   (let [[_ socket] (:conn db)]
     {:db (assoc-in db [:timers timer-id :state] :running)
      :set-clock timer-id
      :ws-send [{:command "start-timer"
                 :timer-id timer-id} socket]})))

(re-frame/reg-event-db
 :add-interval
 [db-spec-inpector]
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
 [db-spec-inpector]
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
 [db-spec-inpector]
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
 [db-spec-inpector]
 (fn [{:keys [db] :as cofx} [_ user]]
   (let [user-profile (auth/user-profile user)]
     {:db          (assoc db :user user-profile)
      :create-conn (:token user-profile)
      :dispatch-n  [[:list-all-projects (:token user-profile)]
                    [:list-all-timers (:token user-profile) (t-core/today-at-midnight)]]})))

(re-frame/reg-event-db
 :log-out
 [db-spec-inpector]
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
            (re-frame/dispatch [:stop-timer data]))))
      (debug "Default: " data))))

(defn timer-state
  [{:keys [duration] :as timer}]
  (if (= 0 duration) :running :paused))

(re-frame/reg-event-fx
 :timer-date-changed
 [db-spec-inpector]
 (fn [{:keys [db] :as cofx} [_ key date]]
   (let [auth-token (get-in db [:user :token])]
     {:db (assoc db key date)
      :dispatch [:list-all-timers auth-token (t-coerce/from-date date)]})))

(re-frame/reg-event-fx
 :add-timer-to-db
 [db-spec-inpector]
 (fn [{:keys [db] :as cofx} [_ timer]]
   (let [timer-id (:id timer)]
     {:db (-> db
              (assoc-in [:timers timer-id]
                        (-> timer
                            (assoc :state (timer-state timer))
                            (assoc :elapsed 0))))})))

(re-frame/reg-fx
 :create-conn
 (fn [goog-auth-id]
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
 [db-spec-inpector]
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :add-timers-to-db
 [db-spec-inpector]
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
 (fn [cofx [_ auth-token timer-date]]
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
                   :on-failure      [:http-failure]}})))

;; ====== Websocket ==========

(re-frame/reg-fx
 :ws-send
 (fn [[data socket]]
   (ws/send socket (clj->js data) fmt/json)))
