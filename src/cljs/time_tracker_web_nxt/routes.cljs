(ns time-tracker-web-nxt.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre]))


(def routes ["/" {""         :timers
                  "about"    :about
                  "clients/" {""    :clients
                              "new" :create-client}}])

(def url-for (partial bidi/path-for routes))

(defn parse-url [url]
  (bidi/match-route routes url))

(defn dispatch-route [matched-route]
  (let [panel (-> matched-route
                 :handler
                 name
                 keyword)]
    (rf/dispatch [:set-active-panel panel])))

(defn dispatch-url [k]
  (pushy/pushy dispatch-route parse-url))

;; Start event listeners
(pushy/start! (pushy/pushy dispatch-route parse-url))
