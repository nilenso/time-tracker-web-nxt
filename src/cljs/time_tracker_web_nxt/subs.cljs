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
(create-subscription :show-task-form?)
(create-subscription :clients)
(create-subscription :client)

(rf/reg-sub
 :all-projects
 (fn [db]
   (:projects db)))

(rf/reg-sub
 :projects-for-client
 :<- [:all-projects]
 :<- [:selected-client]
 (fn [[all-projects selected-client] _]
   (filter #(= (:client_id %) selected-client) all-projects)))

(rf/reg-sub
 :all-tasks
 (fn [db]
   (:tasks db)))

(rf/reg-sub
 :tasks-for-project
 :<- [:all-tasks]
 :<- [:selected-project]
 (fn [[all-tasks selected-project] _]
   (filter #(= (:project_id %) selected-project) all-tasks)))

(rf/reg-sub
 :db-selected-project
 (fn [db]
   (:selected-project db)))

(rf/reg-sub
 :db-selected-task
 (fn [db]
   (:selected-task db)))

(rf/reg-sub
 :selected-client
 (fn [db]
   (or (:selected-client db)
       (:id (first (:clients db))))))

(rf/reg-sub
 :selected-project
 :<- [:projects-for-client]
 :<- [:db-selected-project]
 (fn [[projects db-selected-project] _]
   (or db-selected-project
       (:id (first projects)))))

(rf/reg-sub
 :selected-task
 :<- [:tasks-for-project]
 :<- [:db-selected-task]
 (fn [[tasks db-selected-task] _]
   (or db-selected-task
       (:id (first tasks)))))
