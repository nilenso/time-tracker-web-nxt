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

(defn signed-in? [local-store-app-db]
  ;; FIXME: We assume that the existance of localstorage means that the user is logged in.
  (some? local-store-app-db))

(defn initialize-db [{:keys [db local-store-app-db]} cofx]
  (if (signed-in? local-store-app-db)
    {:db       (merge db/default-db local-store-app-db)
     :dispatch-n [[:create-ws-connection (get-in local-store-app-db [:user :token])]
                  [:fetch-data]]}
    {:db       db/default-db
     :dispatch [:goto [:sign-in]]}))


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
