(ns time-tracker-web-nxt.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [taoensso.timbre :as timbre]))


(def routes ["/" {""         :timers
                  "sign-in"  :sign-in
                  "about"    :about
                  "manage-users"    :manage-users
                  "clients/" {""    :clients
                              "new" :create-client
                              [:client-id "/"] {"" :client
                                                "projects/" {"" :client
                                                             [:project-id] :project}}}}])

(def url-for (partial bidi/path-for routes))

(defn parse-url [url]
  (bidi/match-route routes url))

(defn dispatch-route [matched-route]
  (let [panel (-> matched-route
                  :handler
                  name
                  keyword)]
    (case panel
      :client (rf/dispatch [:select-client (-> matched-route
                                               (get-in [:route-params :client-id])
                                               int)])
      :project (rf/dispatch [:select-project (-> matched-route
                                                 (get-in [:route-params :project-id])
                                                 int)])
      true)
    (timbre/info "dispatch-route called with" matched-route)
    (rf/dispatch [:set-active-panel panel])
    nil))

(def history
  (pushy/pushy dispatch-route parse-url))

(defn goto [_ [_ route-vec]]
  (let [url (apply url-for route-vec)]
    ;; TODO: If URL is nil redirect to a 404 page
    (pushy/set-token! history url)
    (dispatch-route (parse-url url))
    {}))

;; Start event listeners
(defn init []
  (rf/reg-event-fx :goto goto)
  (pushy/start! history))
