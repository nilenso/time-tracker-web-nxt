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
   (filter #(= (:client_id %) (:id selected-client)) all-projects)))

(rf/reg-sub
 :all-tasks
 (fn [db]
   (:tasks db)))

(rf/reg-sub
 :tasks-for-project
 :<- [:all-tasks]
 :<- [:selected-project]
 (fn [[all-tasks selected-project] _]
   (filter #(= (:project_id %) (:id selected-project)) all-tasks)))

(rf/reg-sub
 :db-selected-project-id
 (fn [db]
   (:selected-project-id db)))

(rf/reg-sub
 :db-selected-task-id
 (fn [db]
   (:selected-task-id db)))

(rf/reg-sub
 :selected-client
 (fn [db]
   (if-let [selected-client-id (:selected-client-id db)]
     (first (filter #(= (:id %) selected-client-id) (:clients db)))
     (first (:clients db)))))

(rf/reg-sub
 :selected-project
 :<- [:projects-for-client]
 :<- [:db-selected-project-id]
 (fn [[projects-for-client db-selected-project-id] _]
   (if db-selected-project-id
     (first (filter #(= (:id %) db-selected-project-id) projects-for-client))
     (first projects-for-client))))

(rf/reg-sub
 :selected-task
 :<- [:tasks-for-project]
 :<- [:db-selected-task-id]
 (fn [[tasks-for-project db-selected-task-id] _]
   (if db-selected-task-id
     (first (filter #(= (:id %) db-selected-task-id) tasks-for-project))
     (first tasks-for-project))))
