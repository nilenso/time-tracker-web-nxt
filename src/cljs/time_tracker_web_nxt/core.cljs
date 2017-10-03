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
  "Does specific development related setup."
  (when config/debug?
    (enable-console-print!)             ;; so that println writes to `console.log`
    (println "App is in development mode")))

(defn loading []
  "Shows a loader message while waiting for page load."
  (reagent/render [:h2 "Loading"]
                  (.getElementById js/document "app")))

(defn mount-root []
  "Root view for the UI."
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "app")))

;; TODO: Consider using https://github.com/Day8/re-frame-async-flow-fx
(defn ^:export init []
  ;; Put an initial value into app-db.
  ;; The event handler for `:initialise-db` can be found in `events.cljs`
  ;; Using the sync version of dispatch means that value is in
  ;; place before we go onto the next step.
  (re-frame/dispatch-sync [:initialize-db])

  (dev-setup)
  (go (when-let [c (<! (auth/init!))])
      (mount-root))
  (loading))
