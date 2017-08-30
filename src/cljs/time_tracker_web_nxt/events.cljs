(ns time-tracker-web-nxt.events
  (:require [re-frame.core :as re-frame]
            [time-tracker-web-nxt.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
