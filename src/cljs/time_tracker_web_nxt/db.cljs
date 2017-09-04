(ns time-tracker-web-nxt.db)

(def default-db
  {:name "the future Time Tracker"
   :timers {:timer1 {:id 1 :elapsed 20 :state :paused :project "C1/P1/T1" :note "Notes for Timer1"}
            :timer2 {:id 2 :elapsed 30 :state :paused :project "C2/P2/T2" :note "Notes for Timer2"}
            :timer3 {:id 3 :elapsed 40 :state :paused :project "C1/P1/T1" :note "Notes for Timer3"}}
   :intervals {}
   :projects {:project1 {:id 1 :task "C1/P1/T1"}
              :project2 {:id 2 :task "C2/P2/T2"}}
   :last-timer 3})
