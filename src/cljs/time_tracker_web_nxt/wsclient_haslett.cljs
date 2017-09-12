(ns time-tracker-web-nxt.wsclient-haslett
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]))


(def ping {:command "ping"})

(def data (->> ping
               clj->js
               (.stringify js/JSON)))

(defonce wschan (atom nil))
(defn start []
  (go (let [stream (<! (ws/connect "ws://localhost:8000/api/timers/ws-connect/"))]
        (prn "Received 1 => " (<! (:source stream)))
        (>! (:sink stream) data)
        (reset! wschan stream)
        #_(ws/close stream))))


(defn reset []
  (reset! wschan nil))
