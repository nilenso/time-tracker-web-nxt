(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :app-name
 (fn [db]
   (:app-name db)))

(re-frame/reg-sub
 :timers
 (fn [db]
   (:timers db)))

(re-frame/reg-sub
 :projects
 (fn [db]
   (:projects db)))

(re-frame/reg-sub
 :user
 (fn [db]
   (:user db)))

(re-frame/reg-sub
 :conn
 (fn [db]
   (:conn db)))

(re-frame/reg-sub
 :timer-date
 (fn [db]
   (:timer-date db)))
