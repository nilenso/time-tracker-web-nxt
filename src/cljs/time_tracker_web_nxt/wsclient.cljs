(ns time-tracker-web-nxt.wsclient
  (:require [clojure.spec.alpha :as s]))

;; start [url {:on-message #()}] -> start connection and return socket
;; send -> send data
;; close

(defprotocol Format
  "The format protocol."
  (read  [formatter string])
  (write [formatter value]))

(def json
  "Read and write data encoded in JSON."
  (reify Format
    (read  [_ s] (js->clj (js/JSON.parse s) :keywordize-keys true))
    (write [_ v] (js/JSON.stringify (clj->js v)))))

(defn status [socket]
  (condp = (.-readyState socket)
    0 :connecting
    1 :open
    2 :stopping
    3 :stopped))


(s/def ::websocket-open #(= 1 (.-readyState %)))

(defn start
  "Starts a websocket connection and returns it."
  [url {:keys [on-open on-message on-close]}]
  (if-let [sock (js/WebSocket. url)]
    (do
      (set! (.-onopen sock) on-open)
      (set! (.-onmessage sock) on-message)
      (set! (.-onclose sock) on-close)
      sock)
    (throw (js/Error. (str "Web socket connection failed: " url)))))

(defn send
  "Sends data over the socket in the specified format."
  ([socket data]
   (send socket data json))
  ([socket data format]
   {:pre [(s/valid? ::websocket-open socket)]}
   (.send socket (write format data))))

(defn close [socket] (.close socket))

(def ping {:command "ping"})

;; (def sock (ws/start "ws://...."))
;;
;; (ws/send sock data ws.format/json)
;; (ws/close sock)
;; (require '[wsclj.client :as ws])
;; (client/start)
