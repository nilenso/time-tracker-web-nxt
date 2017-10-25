(ns time-tracker-web-nxt.events.auth
  (:require
   [cljs-time.coerce :as t-coerce]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.auth :as auth]
   [time-tracker-web-nxt.db :as db]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store]]))

(defn login
  [{:keys [db] :as cofx} [_ user]]
  (let [user-profile (auth/user-profile user)]
    {:db          (-> db
                     (assoc :user user-profile)
                     (assoc :active-panel :timers-panel))
     :dispatch-n  [[:get-projects (:token user-profile)]
                   [:get-timers
                    (:token user-profile)
                    (t-coerce/from-date (:timer-date db))]]}))

(defn logout
  [{:keys [db] :as cofx} [_ user]]
  (let [[_ socket] (:conn db)]
    {:db db/default-db
     :close socket
     :clear-local-storage nil}))


(defn init []
  (rf/reg-event-fx
   :log-in
   [db-spec-inspector ->local-store]
   login)

  (rf/reg-event-fx :log-out logout))
