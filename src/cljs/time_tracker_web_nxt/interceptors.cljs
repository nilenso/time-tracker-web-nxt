(ns time-tracker-web-nxt.interceptors
  (:require
   [cljs.spec.alpha :as s]
   [hodgepodge.core :refer [local-storage set-item]]
   [re-frame.core :as rf]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "Spec failed => " (s/explain-str a-spec db)) {}))))

(def db-spec-inspector
  ;; This Interceptor runs `check-and-throw` `after` the event handler has finished, checking
;; the value for `app-db` against a spec.
;; If the event handler corrupted the value for `app-db` an exception will be
;; thrown. This helps us detect event handler bugs early.
;; Because all state is held in `app-db`, we are effectively validating the
;; ENTIRE state of the application after each event handler runs.  All of it.
  (rf/after (partial check-and-throw :time-tracker-web-nxt.db/db)))


(defn db->local-store
  "Persists app db to local storage"
  [app-db]
  ;; Remove websocket and response channel
  (let [new-db (assoc app-db :conn [])]
    (set-item local-storage "db" (str new-db))))

;; This interceptor persists the rf db to local-storage
(def ->local-store (rf/after db->local-store))
