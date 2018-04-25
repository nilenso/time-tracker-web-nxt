(ns time-tracker-web-nxt.events.ws
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [chan put! <! close! alts! take! timeout]]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre]
   [time-tracker-web-nxt.config :as config]
   [time-tracker-web-nxt.utils :as utils]
   [wscljs.client :as ws]
   [wscljs.format :as fmt]))

(defn ws-receive
  [{:keys [id started-time duration type] :as data}]
  ;; In response to a "create-and-start-timer" message, the websocket
  ;; receives two messages- one "create" and one "update". We only
  ;; dispatch :start-timer after receiving the "update".
  (case type
    "create" (do
               (timbre/info "Create: " data)
               (rf/dispatch [:add-timer-to-db (dissoc data :type)]))
    "update" (do
               (timbre/info "Update: " data)
               (if (and (nil? started-time) (> duration 0))
                 (do (timbre/debug "Stopping timer: " id)
                     (rf/dispatch [:stop-or-update-timer data]))
                 (do  (timbre/debug "Received start timer message")
                      (rf/dispatch [:start-timer (dissoc data :type)]))))
    "delete" (do
               (timbre/info "Delete: " data)
               (rf/dispatch [:delete-timer data]))
    (timbre/debug "Unknown Action: " data)))

(defn ws-send [[data socket]]
  (ws/send socket (clj->js data) fmt/json))

(defn ws-create [goog-auth-id]
  (let [response-chan (chan)
        handlers      {:on-message #(do
                                      (ws-receive (fmt/read fmt/json (.-data %)))
                                      (put! response-chan (fmt/read fmt/json (.-data %))))}
        conn          (ws/create (:conn-url config/env) handlers)]
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
  (when socket
    (ws/close socket)))

(defn init []
  (rf/reg-event-fx
   :create-ws-connection
   (fn [cofx [_ google-auth-token]]
     {:create google-auth-token}))

  (rf/reg-event-fx
   :save-connection
   (fn [{:keys [db] :as cofx} [_ sock]]
     {:db   (assoc db :conn sock)
      :ping sock}))

  (rf/reg-fx :send ws-send)
  (rf/reg-fx :create ws-create)
  (rf/reg-fx :close ws-close)
  (rf/reg-fx :ping ws-ping))
