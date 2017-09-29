(ns time-tracker-web-nxt.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [time-tracker-web-nxt.events]
            [time-tracker-web-nxt.subs]
            [time-tracker-web-nxt.views :as views]
            [time-tracker-web-nxt.config :as config]
            [time-tracker-web-nxt.auth :as auth]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "App is in development mode")))

(defn loading []
  (reagent/render [:h2 "Loading"]
                  (.getElementById js/document "app")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "app")))

;; TODO: Consider using https://github.com/Day8/re-frame-async-flow-fx
(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (go (when-let [c (<! (auth/init!))])
      (mount-root))
  (loading))
