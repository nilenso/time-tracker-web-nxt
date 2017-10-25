(ns time-tracker-web-nxt.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::app-name string?)
(s/def ::conn coll?)
(s/def ::show-create-timer-widget? boolean?)
(s/def ::timer-date inst?)

;; User
(s/def ::name string?)
(s/def ::image-url string?)
(s/def ::token string?)
(s/def ::signed-in? boolean?)
(s/def ::user (s/nilable (s/keys :req-un [::name ::image-url ::token ::signed-in?])))

;; Project
(s/def ::id int?)
(s/def ::project (s/keys :req-un [::id ::name]))
(s/def ::projects (s/coll-of ::project))

;; Timer
(s/def ::id int?)
(s/def ::project-id int?)
(s/def ::duration (s/nilable int?))
(s/def ::elapsed (s/nilable int?))
(s/def ::state keyword?)
(s/def ::time-created int?)
(s/def ::notes string?)
(s/def ::app-user-id (s/nilable int?))
(s/def ::started-time (s/nilable int?))
(s/def ::timer (s/keys :req-un [::id ::project-id ::duration
                                ::state ::time-created ::notes
                                ::started-time]
                       :opt-un [::app-user-id ::elapsed]))
(s/def ::timers (s/every-kv int? ::timer))

;; App DB
(s/def ::db (s/keys :req-un [::app-name ::projects ::show-create-timer-widget?
                             ::timers ::user ::conn]
                    :opt-un [::intervals ::timer-date]))

(def default-db
  {:app-name "the future Time Tracker"
   :active-panel :sign-in-panel
   :timers {}
   :timer-date (js/Date. (.setHours (js/Date.) 0 0 0 0))
   :intervals {}
   :projects []
   :conn []
   :app-user-id nil
   :show-create-timer-widget? false
   :boot-from-local-storage? false})
