(ns time-tracker-web-nxt.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :app-name
 (fn [db]
   (:app-name db)))

(re-frame/reg-sub
 :user
 (fn [db]
   (:user db)))
