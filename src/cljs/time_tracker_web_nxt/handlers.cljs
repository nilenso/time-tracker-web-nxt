(ns time-tracker-web-nxt.handlers
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [ajax.core :as ajax]
   [cljs.core.async :as async :refer [chan put! <! close! alts! take! timeout]]
   [cljs-time.coerce :as t-coerce]
   [cljs-time.core :as t-core]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.config :as config]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.utils :as utils]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]))


(defn login
  [{:keys [db] :as cofx} [_ user]]
  (let [user-profile (auth/user-profile user)]
    {:db          (assoc db :user user-profile)
     :dispatch-n  [[:api/get-projects (:token user-profile)]
                   [:api/get-timers
                    (:token user-profile)
                    (t-coerce/from-date (:timer-date db))]]}))

(defn logout
  [{:keys [db] :as cofx} [_ user]]
  (let [[_ socket] (:conn db)]
    {:db db/default-db
     :ws/close socket
     :clear-local-storage nil}))

(defn timer-date-changed
  [{:keys [db] :as cofx} [_ key date]]
  (let [auth-token (get-in db [:user :token])]
    {:db       (assoc db key date)
     :dispatch [:api/get-timers auth-token (t-coerce/from-date date)]}))

(defn start-timer [{:keys [db] :as cofx} [_ {:keys [id]}]]
  {:db        (assoc-in db [:timers id :state] :running)
   :set-clock id})

(defn resume-timer [{:keys [db] :as cofx} [_ timer-id]]
  (let [[_ socket] (:conn db)]
    {:db        (assoc-in db [:timers timer-id :state] :running)
     :set-clock timer-id
     :ws/send   [{:command  "start-timer"
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
     :ws/send [{:command  "update-timer"
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
     :ws/send     [{:command  "stop-timer"
                    :timer-id timer-id} socket]}))

(defn set-clock [timer-id]
  (let [dispatch-fn #(rf/dispatch [:increment-timer-duration timer-id])
        interval-id (js/setInterval dispatch-fn 1000)]
    (rf/dispatch [:add-interval timer-id interval-id])))

(defn clear-clock [interval-id]
   (js/clearInterval interval-id))

(defn ws-receive
  [{:keys [id started-time duration type] :as data}]
  (case type
    "create" (do
               (timbre/info "Create: " data)
               (rf/dispatch [:add-timer-to-db (dissoc data :type)])
               (timbre/debug "Starting timer: " id)
               (rf/dispatch [:start-timer (dissoc data :type)]))
    "update" (do
               (timbre/info "Update: " data)
               (if (and (nil? started-time) (> duration 0))
                 (do (timbre/debug "Stopping timer: " id)
                     (rf/dispatch [:stop-timer data]))))
    (timbre/debug "Unknown Action: " data)))

(defn get-projects [cofx [_ auth-token]]
  {:http-xhrio {:method :get
                :uri "/api/projects/"
                :timeout 8000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers {"Authorization" (str "Bearer " auth-token)
                          "Access-Control-Allow-Origin" "*"}
                :on-success [:projects-retrieved]
                :on-failure [:api/request-failed]}})

(defn projects-retrieved [db [_ projects]]
  (assoc db :projects projects))

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
                  :on-success      [:timers-retrieved]
                  :on-failure      [:api/request-failed]}}))


(defn timers-retrieved [db [_ timers]]
  (assoc db :timers (utils/->timer-map timers)))

(defn http-failure [_ [_ e]]
  {:error e})

(defn ws-send [[data socket]]
  (ws/send socket (clj->js data) fmt/json))

(defn ws-create-and-start-timer
  [{:keys [db current-timestamp] :as cofx} [_ timer-project timer-note]]
  (let [[_ socket] (:conn db)
        timer-date (str (:timer-date db))]
    {:db (assoc db :show-create-timer-widget? false)
     :ws/send [{:command "create-and-start-timer"
                :project-id (js/parseInt (:id timer-project) 10)
                :created-time (utils/timer-created-time timer-date current-timestamp)
                :notes timer-note} socket]}))

(defn ws-create [goog-auth-id]
  (let [response-chan (chan)
        handlers {:on-message #(do
                                 (ws-receive (fmt/read fmt/json (.-data %)))
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

(defn ws-ping [[_ sock]]
  (go
    (while (= :open (ws/status sock))
      (<! (timeout 10000))
      (ws/send sock (clj->js {:command "ping"}) fmt/json))))

(defn ws-close [socket]
  (.log js/console "Closing websocket connection")
  (ws/close socket))
