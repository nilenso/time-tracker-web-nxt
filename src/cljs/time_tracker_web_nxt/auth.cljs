(ns time-tracker-web-nxt.auth
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.core.async :refer [put! chan <! >! buffer]]
   [cljs.reader :refer [read read-string]]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.config :as config]
   [goog.object :as object]))

(def user (reagent/atom {}))

(defn load-gapi-auth2 []
  (let [c (chan)]
    (.load js/gapi "auth2" #(go (>! c true)))
    c))

(defn auth-instance []
  (.getAuthInstance (object/get js/gapi "auth2")))

(defn get-auth-token []
  (-> (auth-instance) .-currentUser .get .getAuthResponse .-id_token))

(defn user-profile
  [u]
  (let [profile (.getBasicProfile u)]
    {:name       (if profile (.getName profile) nil)
     :image-url  (if profile (.getImageUrl profile) nil)
     :token      (get-auth-token)
     :signed-in? (.isSignedIn u)}))

(defn init! []
  (let [c (chan)]
    ;; TODO: Add timeout
    (.load js/gapi "auth2" (fn [] (-> (.init (object/get js/gapi "auth2")
                                            (clj->js {"client_id" (:client-id config/env)
                                                      "scope"     (:scope config/env)}))
                                     (.then #(go (>! c true))
                                            #(go (>! c false))))))
    c))
