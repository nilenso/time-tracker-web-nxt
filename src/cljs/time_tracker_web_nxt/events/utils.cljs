(ns time-tracker-web-nxt.events.utils
  (:require
   [cljs-time.core :as t-core]
   [hodgepodge.core :refer [local-storage set-item get-item clear!]]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre]))

;; This coeffect is injected into the `initialize-db` event
;; to add the db state stored in local-storage to the coeffects
;; of the event handler.

(defn init []
  (rf/reg-cofx
   :local-store-app-db
   (fn [cofx _]
     (let [db (-> local-storage (get-item "db") cljs.reader/read-string)]
       (assoc cofx :local-store-app-db db))))

  (rf/reg-fx
   :clear-local-storage
   (fn []
     (clear! local-storage)))

  (rf/reg-cofx
   :current-timestamp
   (fn [cofx _]
     (assoc cofx :current-timestamp (t-core/now))))

  (rf/reg-event-db
   :update-client-details
   (fn [db [_ key val]]
     (assoc-in db [:client key] val)))

  (rf/reg-event-db
   :update-point-of-contact
   (fn [db [_ path val]]
     (let [path (concat [:client :points-of-contact] path)]
       (assoc-in db path val))))

  (rf/reg-event-db
   :add-point-of-contact
   (fn [db [_ data]]
     (let [path [:client :points-of-contact]
           poc  (get-in db path)
           id   (inc (count poc))]
       (assoc-in db (conj path id) data))))

  (rf/reg-fx
   :error
   (fn [e]
     (timbre/error e))))
