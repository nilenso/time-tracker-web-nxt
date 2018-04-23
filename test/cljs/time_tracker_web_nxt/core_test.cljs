(ns time-tracker-web-nxt.core-test
  (:require [cljs-time.core :as t-core]
            [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [time-tracker-web-nxt.core :as core]
            [time-tracker-web-nxt.db :as db]
            [time-tracker-web-nxt.events.core :as e]
            [time-tracker-web-nxt.routes :as routes]
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
      :trigger-create-timer
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
  (rf/dispatch [:add-user user]))

(defn fixtures [f]
  (rf-test/run-test-sync
   (with-redefs [interceptors/standard-interceptor stubbed-interceptor]
     (e/init)
     (routes/init)
     (test-fixtures)
     (f))))

(use-fixtures :once fixtures)

;; ===================== Tests ============================

(deftest add-timer-to-db-test
  (testing "adding a new timer to db"
    (let [timers    (rf/subscribe [:timers])
          new-timer {:id           1
                     :task-id   1
                     :started-time nil
                     :duration     10
                     :time-created 1508095069
                     :notes        "dddd"
                     :state        :paused}]

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
      (rf/dispatch [:stop-or-update-timer timer])
      (is (= :paused (get-in @timers [2 :state]))))))

(deftest create-timer-test
  (testing "user can create a new timer"
    (let [task      {:id 12}
          data          {:notes "My notes for this timer"
                         :elapsed-hh 0
                         :elapsed-mm 0
                         :elapsed-ss 0}
          ws-response  {:id           1
                        :task-id   (:id task)
                        :started-time nil
                        :duration     0
                        :time-created (utils/datepicker-date->epoch (str (js/Date.)) (t-core/now))
                        :notes        (:notes data)
                        :type         "create"}
          show-widget? (rf/subscribe [:show-create-timer-widget?])
          timers       (rf/subscribe [:timers])
          expected     (-> ws-response (dissoc :type) (assoc :state :paused))]
      (rf/dispatch [:trigger-create-timer task data])
      (is (= false @show-widget?))
      ;; Assuming websocket sends response as expected
      (ws-events/ws-receive ws-response)
      (is (= expected (get @timers (:id ws-response)))))))


(deftest update-timer-test
  (testing "user can only update a stopped timer"
    (let [timer    (helpers/make-timer {:id 2})
          duration 400
          notes    "some note"
          timer-id (:id timer)
          timers   (rf/subscribe [:timers])]
      (rf/dispatch [:add-timer-to-db timer])

      (rf/dispatch [:stop-or-update-timer {:id timer-id :duration duration :notes notes}])
      (is (= duration (get-in @timers [timer-id :duration])))
      (is (= notes (get-in @timers [timer-id :notes]))))))

(deftest delete-timer-test
  (testing "user can delete a timer"
    (let [timer    (helpers/make-timer {:id 2})
          duration 400
          notes    "some note"
          timer-id (:id timer)
          timers   (rf/subscribe [:timers])]
      (rf/dispatch [:add-timer-to-db timer])

      (is (not-empty (get @timers timer-id)))

      (rf/dispatch [:delete-timer {:id timer-id}])
      (is (nil? (get @timers timer-id))))))
