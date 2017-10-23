(ns time-tracker-web-nxt.test-helpers
  (:require
   [cljs.test :as t :include-macros true]
   [cljs-time.core :as t-core]
   [time-tracker-web-nxt.utils :as utils]))

(defn check-contains [a-map key]
  (some #(= key %) (keys a-map)))

(defn make-timer [opts]
  (let [now (t-core/now)
        created-at (utils/timer-created-time (str (js/Date.)) now)
        default {:id 1
                 :project-id 1
                 :started-time nil
                 :duration 0
                 :time-created created-at
                 :notes "Foo"
                 :type "create"}]
    (merge default opts)))

(defn make-n-timers [n]
  (->> (range 1 (inc n))
       (map (fn [id] [id (make-timer {:id id})]))
       (into {})))
