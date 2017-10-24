(ns time-tracker-web-nxt.events.api
  (:require
   [ajax.core :as ajax]
   [cljs-time.coerce :as t-coerce]
   [cljs-time.core :as t-core]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector ->local-store]]
   [time-tracker-web-nxt.utils :as utils]))

(defn get-projects [cofx [_ auth-token]]
  {:http-xhrio {:method :get
                :uri "/api/projects/"
                :timeout 8000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers {"Authorization" (str "Bearer " auth-token)
                          "Access-Control-Allow-Origin" "*"}
                :on-success [:projects-retrieved]
                :on-failure [:request-failed]}})

(defn projects-retrieved [db [_ projects]]
  (assoc db :projects projects))

(defn get-timers [cofx [_ auth-token timer-date]]
  (let [start-epoch (t-coerce/to-epoch timer-date)
        end-epoch   (-> timer-date
                       (t-core/plus (t-core/days 1))
                       t-coerce/to-epoch)]
    {:http-xhrio {:method          :get
                  :uri             "/api/timers/"
                  :params          {:start start-epoch
                                    :end   end-epoch}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :headers         {"Authorization"               (str "Bearer " auth-token)
                                    "Access-Control-Allow-Origin" "*"}
                  :on-success      [:timers-retrieved]
                  :on-failure      [:request-failed]}}))


(defn timers-retrieved [db [_ timers]]
  (assoc db :timers (utils/->timer-map timers)))

(defn http-failure [_ [_ e]]
  {:error e})

(defn init []
  (rf/reg-event-fx :request-failed http-failure)
  (rf/reg-event-fx :get-projects get-projects)
  (rf/reg-event-fx :get-timers get-timers)

  (rf/reg-event-db
   :projects-retrieved
   [db-spec-inspector ->local-store]
   projects-retrieved)

  (rf/reg-event-db
   :timers-retrieved
   [db-spec-inspector ->local-store]
   timers-retrieved))
