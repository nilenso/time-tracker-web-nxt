(ns time-tracker-web-nxt.db)

(def default-db
  {:name "the future Time Tracker"
   :timers {:timer1 {:id 1 :elapsed 20 :state :paused}
            :timer2 {:id 2 :elapsed 30 :state :paused}
            :timer3 {:id 3 :elapsed 40 :state :paused}}
   :intervals {}
   :last-timer 3})
