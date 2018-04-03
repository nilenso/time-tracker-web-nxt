(ns time-tracker-web-nxt.events.core
  (:require
   [re-frame.core :as rf]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.events.api :as api-events]
   [time-tracker-web-nxt.events.auth :as auth-events]
   [time-tracker-web-nxt.events.timer :as timer-events]
   [time-tracker-web-nxt.events.utils :as utils-events]
   [time-tracker-web-nxt.events.ui :as ui-events]
   [time-tracker-web-nxt.events.ws :as ws-events]))

(defn initialize-db [{:keys [db local-store-app-db]} cofx]
  (if local-store-app-db
    ;; FIXME: We assume that the existance of localstorage means that the user is logged in.
    {:db (assoc (merge db/default-db local-store-app-db) :active-panel :timers)}
    {:db db/default-db}))

(defn init []
  (rf/reg-event-fx
   :initialize-db
   [(rf/inject-cofx :local-store-app-db)]
   initialize-db)

  (api-events/init)
  (auth-events/init)
  (timer-events/init)
  (utils-events/init)
  (ui-events/init)
  (ws-events/init))
