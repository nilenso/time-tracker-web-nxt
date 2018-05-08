(ns time-tracker-web-nxt.events.auth
  (:require
   [cljs-time.coerce :as t-coerce]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store]]))

(defn login
  [{:keys [db] :as cofx} [_ user]]
  (let [user-profile (auth/user-profile user)
        token        (:token user-profile)]
    (taoensso.timbre/info "login called")
    {:db         (assoc db :user user-profile)
     :dispatch-n [[:create-ws-connection token]
                  [:get-user-details token]
                  [:goto [:timers]]]}))

(defn logout
  [{:keys [db] :as cofx} [_ user]]
  (let [[_ socket] (:conn db)]
    {:db                  db/default-db
     :close               socket
     :clear-local-storage nil
     :dispatch            [:goto [:sign-in]]}))


(defn init []
  (rf/reg-event-fx
   :log-in
   [db-spec-inspector ->local-store]
   login)

  (rf/reg-event-fx :log-out logout))
