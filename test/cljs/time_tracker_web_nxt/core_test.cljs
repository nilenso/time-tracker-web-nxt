(ns time-tracker-web-nxt.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [time-tracker-web-nxt.core :as core]
            [time-tracker-web-nxt.db :as db]
            [time-tracker-web-nxt.events :as e]
            [time-tracker-web-nxt.handlers :as handlers]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [cljs-time.core :as t-core]
            [time-tracker-web-nxt.utils :as utils]))

(def test-user
  {:name "Kiran"
   :image-url ""
   :token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBlNzhlN2FlZj"
   :signed-in? true})

(defn test-fixtures []
  (rf/reg-event-db
   :add-user
   (fn [db [_ user]]
     (assoc db :user user)))

  (rf/dispatch [:initialize-db])
  (rf/dispatch [:add-user test-user]))

(deftest add-timer-to-db-test
  (rf-test/run-test-sync
   (test-fixtures)

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

(defn check-contains [a-map key]
  (some #(= key %) (keys a-map)))

(deftest create-and-start-timer-test
  (testing "user can create a new timer"
    (let [project           {:id 12}
          notes             "Random gibberish"
          connection-params [nil "this is a socket"]
          timer-date        (js/Date.)
          now               (t-core/now)
          cofx              {:db {:conn       connection-params
                                  :timer-date timer-date}
                             :current-timestamp now}
          result-fx-map     (handlers/ws-create-and-start-timer cofx [nil project notes])
          ws-response {:id 1
                       :project-id (:id project)
                       :started-time nil
                       :duration 0
                       :time-created (utils/timer-created-time (str timer-date) now)
                       :notes notes
                       :type "create"}]
      (is (check-contains result-fx-map :ws/send))
      (let [ws-send-params  (:ws/send result-fx-map)
            [params socket] ws-send-params
            expected-params {:command      "create-and-start-timer"
                             :project-id   (:id project)
                             :created-time (utils/timer-created-time (str timer-date) now)
                             :notes        notes}]
        (is (not-any? nil? ws-send-params))
        (is (= (second connection-params) socket))
        (is (= expected-params params)))

      ;; Assuming websocket sends response as expected
      (rf-test/run-test-sync
       (test-fixtures)
       (handlers/ws-receive ws-response)

       (let [timers (rf/subscribe [:timers])
             expected (assoc (dissoc ws-response :type) :state :running)]
         (is (= expected (get @timers (:id ws-response)))))))))
