(ns time-tracker-web-nxt.core-test
  (:require [cljs-time.core :as t-core]
            [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [time-tracker-web-nxt.core :as core]
            [time-tracker-web-nxt.db :as db]
            [time-tracker-web-nxt.events.core :as e]
            [time-tracker-web-nxt.events.ws :as ws-events]
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
      :create-and-start-timer
      (update-in context [:effects] dissoc :send)
      :start-timer
      (update-in context [:effects] dissoc :set-clock)
      :stop-timer
      (update-in context [:effects] dissoc :send)
      :update-timer
      (update-in context [:effects] dissoc :send)

      context)))

(def stubbed-interceptor
  (rf/->interceptor
   :id :standard-interceptor
   :before before-handler
   :after  after-handler))

(defn test-fixtures []
  (rf/reg-event-db
   :add-user
   (fn [db [_ user]]
     (assoc db :user user)))

  (rf/dispatch [:initialize-db])
  (rf/dispatch [:add-user test-user]))

(defn fixtures [f]
  (rf-test/run-test-sync
   (with-redefs [interceptors/standard-interceptor stubbed-interceptor]
     (e/init)
     (test-fixtures)
     (f))))

(use-fixtures :once fixtures)

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

(deftest create-and-start-timer-test
  (testing "user can create a new timer"
    (let [project      {:id 12}
          notes        "My notes for this timer"
          ws-response  {:id           1
                        :project-id   (:id project)
                        :started-time nil
                        :duration     0
                        :time-created (utils/datepicker-date->epoch (str (js/Date.)) (t-core/now))
                        :notes        notes
                        :type         "create"}
          show-widget? (rf/subscribe [:show-create-timer-widget?])
          timers       (rf/subscribe [:timers])
          expected     (-> ws-response (dissoc :type) (assoc :state :running))]
      (rf/dispatch [:create-and-start-timer project notes])
      (is (= false @show-widget?))
      ;; Assuming websocket sends response as expected
      (ws-events/ws-receive ws-response)
      (is (= expected (get @timers (:id ws-response)))))))


(deftest update-timer-test
  (testing "user can only update a stopped timer"
    (let [timer    (helpers/make-timer {:id 2})
          elapsed1 {:elapsed-hh 0 :elapsed-mm 0 :elapsed-ss 10}
          elapsed2 {:elapsed-hh 0 :elapsed-mm 5 :elapsed-ss 10}
          elapsed3 {:elapsed-hh 2 :elapsed-mm 5 :elapsed-ss 10}
          notes    "....."
          timer-id (:id timer)
          timers   (rf/subscribe [:timers])]
      (rf/dispatch [:add-timer-to-db timer])

      (rf/dispatch [:update-timer timer-id elapsed1 notes])
      (is (= 10 (get-in @timers [timer-id :elapsed])))

      (rf/dispatch [:update-timer timer-id elapsed2 notes])
      (is (= (+ 10 (* 5 60)) (get-in @timers [timer-id :elapsed])))

      (rf/dispatch [:update-timer timer-id elapsed3 notes])
      (is (= (+ 10 (* 5 60) (* 2 60 60)) (get-in @timers [timer-id :elapsed]))))))
