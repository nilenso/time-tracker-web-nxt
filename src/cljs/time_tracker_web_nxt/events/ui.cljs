(ns time-tracker-web-nxt.events.ui
  (:require
   [re-frame.core :as rf]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store tt-reg-event-db]]))

(defn init []
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
   :set-active-panel
   (fn [db [_ panel]]
     (let [status (:show-user-menu? db)]
       (-> db
          (assoc :active-panel panel)
          (assoc :show-user-menu? false)))))

  (rf/reg-event-db
   :toggle-user-menu
   (fn [db _]
     (let [status (:show-user-menu? db)]
       (assoc db :show-user-menu? (not status))))))
