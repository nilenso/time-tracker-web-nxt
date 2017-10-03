(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(defn subscribe [db-var]
  (re-frame/reg-sub
   db-var
   (fn [db]
     (db-var db))))

(subscribe :app-name)
(subscribe :timers)
(subscribe :projects)
(subscribe :user)
(subscribe :conn)
(subscribe :timer-date)
