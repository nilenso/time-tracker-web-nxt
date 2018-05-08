(ns time-tracker-web-nxt.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::app-name string?)
(s/def ::conn coll?)
(s/def ::show-create-timer-widget? boolean?)
(s/def ::show-user-menu? boolean?)
(s/def ::timer-date inst?)

;; Common
(s/def ::id int?)
(s/def ::name string?)

;; User
(s/def ::image-url string?)
(s/def ::token string?)
(s/def ::signed-in? boolean?)
(s/def ::user (s/nilable (s/keys :req-un [::name ::image-url ::token ::signed-in?])))

;; Client
(s/def ::gstin (s/or ::gstin-str (s/and string? #(= 15 (count %)))
                     ::empty empty?))
(s/def ::pan (s/or ::pan-str (s/and string? #(= 10 (count %)))
                   ::empty empty?))
(s/def ::client (s/keys :req-un [::id ::name]
                        :opt-un [::gstin ::pan]))
(s/def ::clients (s/coll-of ::client))
(s/def ::selected-client-id int?)

;; Project
(s/def ::client-id int?)
(s/def ::project (s/keys :req-un [::id ::name ::client-id]))
(s/def ::projects (s/coll-of ::project))
(s/def ::selected-project-id int?)

;; Task
(s/def ::project-id int?)
(s/def ::task (s/keys :req-un [::id ::name ::project-id]))
(s/def ::tasks (s/coll-of ::task))
(s/def ::selected-task-id int?)

;; Timer
(s/def ::task-id int?)
(s/def ::duration (s/nilable int?))
(s/def ::state keyword?)
(s/def ::time-created int?)
(s/def ::notes string?)
(s/def ::app-user-id (s/nilable int?))
(s/def ::started-time (s/nilable int?))
(s/def ::timer (s/keys :req-un [::id ::task-id ::duration
                                ::state ::time-created ::notes
                                ::started-time]
                       :opt-un [::app-user-id]))
(s/def ::timers (s/every-kv int? ::timer))

;; App DB
(s/def ::db (s/keys :req-un [::app-name ::clients ::projects ::tasks
                             ::show-create-timer-widget?
                             ::timers ::user ::conn ::show-user-menu?]
                    :opt-un [::intervals ::timer-date ::selected-client-id
                             ::selected-project-id ::selected-task-id]))

(def default-db
  {:app-name                  "the future Time Tracker"
   :timers                    {}
   :timer-date                (js/Date. (.setHours (js/Date.) 0 0 0 0))
   :clients                   []
   :projects                  []
   :tasks                     []
   :conn                      []
   :app-user-id               nil
   :show-create-timer-widget? false
   :show-user-menu?           false})
