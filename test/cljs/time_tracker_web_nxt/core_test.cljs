(ns time-tracker-web-nxt.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [time-tracker-web-nxt.core :as core]
            [time-tracker-web-nxt.db :as db]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]))

(def test-user
  {:name "Kiran"
   :image-url ""
   :token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBlNzhlN2FlZj"
   :signed-in? true})

(defn test-fixtures []
  (rf/reg-event-db
   :add-user
   (fn [db [_ user]]
     (assoc db :user user))))

(deftest test-adding-timer-to-db
  (rf-test/run-test-sync
   (test-fixtures)
   (rf/dispatch [:initialize-db])
   (rf/dispatch [:add-user test-user])

   (let [timers    (rf/subscribe [:timers])
         new-timer {:id           1
                    :project-id   1
                    :started-time nil
                    :duration     10
                    :time-created 1508095069
                    :notes        "dddd"
                    :state        :paused
                    :elapsed      10}]

     (rf/dispatch [:add-timer-to-db new-timer])

     (is (= 1 (count @timers)))
     (is (some #(= (:id new-timer) %) (keys @timers)))
     (is (= new-timer (get @timers (:id new-timer)))))))
