(ns time-tracker-web-nxt.core-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [time-tracker-web-nxt.core :as core]
            [time-tracker-web-nxt.db :as db]
            [time-tracker-web-nxt.events :as e]
            [time-tracker-web-nxt.handlers :as handlers]
            [time-tracker-web-nxt.interceptors :as interceptors]
            [time-tracker-web-nxt.test-helpers :as helpers]
            [time-tracker-web-nxt.utils :as utils]))

;;; ==================== Test Fixtures =========================
(def user
  {:name "Kiran"
   :image-url ""
   :token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBlNzhlN2FlZj"
   :signed-in? true})

(defn before-handler [context]
  context)

(defn after-handler [context]
  (let [event (-> context :coeffects :event first)]
    (case event
      :start-timer
      (update-in context [:effects] dissoc :set-clock)
      :stop-timer
      (update-in context [:effects] dissoc :ws/send)

      context)))

(def stubbed-interceptor
  (rf/->interceptor
   :id :standard-interceptor
   :before before-handler
   :after  after-handler))

(defn fixtures [f]
  (rf-test/run-test-sync
   (test-fixtures)
   (with-redefs [interceptors/standard-interceptor stubbed-interceptor]
     (e/init)
     (f))))

(use-fixtures :once fixtures)

(defn test-fixtures []
  (rf/reg-event-db
   :add-user
   (fn [db [_ user]]
     (assoc db :user user)))

  (rf/dispatch [:initialize-db])
  (rf/dispatch [:add-user test-user]))

;; ===================== Tests ============================

(deftest add-timer-to-db-test
  (testing "adding a new timer to db"
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
      (is (= new-timer (get @timers (:id new-timer)))))))

(deftest start-timer-test
  (testing "user can start a timer"
    (let [timer    (helpers/make-timer {:id 1})
          timer-id (:id timer)
          timers   (rf/subscribe [:timers])]
      (rf/dispatch [:add-timer-to-db timer])
      (rf/dispatch [:start-timer timer])
      (is (= :running (get-in @timers [timer-id :state]))))))

(deftest stop-timer-test
  (testing "user can stop a timer"
    (let [timer    (helpers/make-timer {:id 2})
          timer-id (:id timer)
          timers   (rf/subscribe [:timers])]
      (rf/dispatch [:add-timer-to-db timer])
      (rf/dispatch [:start-timer timer])
      (rf/dispatch [:stop-timer timer])
      (is (= :paused (get-in @timers [2 :state]))))))
