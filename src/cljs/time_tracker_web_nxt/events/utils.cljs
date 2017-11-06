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
       (assoc cofx
              :local-store-app-db
              (if db
                (assoc db :boot-from-local-storage? true)
                nil)))))

  (rf/reg-fx
   :clear-local-storage
   (fn []
     (clear! local-storage)))

  (rf/reg-cofx
   :current-timestamp
   (fn [cofx _]
     (assoc cofx :current-timestamp (t-core/now))))


  (rf/reg-fx
   :error
   (fn [e]
     (timbre/error e))))
