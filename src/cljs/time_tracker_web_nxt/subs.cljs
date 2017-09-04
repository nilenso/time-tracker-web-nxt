(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :timers
 (fn [db]
   (:timers db)))

(re-frame/reg-sub
 :projects
 (fn [db]
   (:projects db)))
