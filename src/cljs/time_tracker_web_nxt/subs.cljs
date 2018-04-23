(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :sorted-timers
 :<- [:timers]
 (fn [timers _]
   (->> timers vals (sort-by :id) reverse)))

(defn create-subscription [db-var]
  (rf/reg-sub
   db-var
   (fn [db]
     (db-var db))))

(create-subscription :app-name)
(create-subscription :active-panel)
(create-subscription :timers)
(create-subscription :user)
(create-subscription :conn)
(create-subscription :timer-date)
(create-subscription :show-create-timer-widget?)
(create-subscription :show-user-menu?)
(create-subscription :clients)
(create-subscription :client)
(create-subscription :selected-client)
(create-subscription :selected-project)
(create-subscription :selected-task)
(create-subscription :projects)
(create-subscription :tasks)
