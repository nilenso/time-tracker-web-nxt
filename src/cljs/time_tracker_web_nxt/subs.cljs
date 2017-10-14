(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(defn create-subscription [db-var]
  (rf/reg-sub
   db-var
   (fn [db]
     (db-var db))))

(create-subscription :app-name)
(create-subscription :timers)
(create-subscription :projects)
(create-subscription :user)
(create-subscription :conn)
(create-subscription :timer-date)
(create-subscription :show-add-timer-widget?)
(create-subscription :boot-from-local-storage?)
