(ns time-tracker-web-nxt.events.api
  (:require
   [ajax.core :as ajax]
   [cljs-time.coerce :as t-coerce]
   [cljs-time.core :as t-core]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.interceptors :as intr]
   [time-tracker-web-nxt.utils :as utils]
   [time-tracker-web-nxt.events.ui :as ui-events]))

(defn user-details-retrieved [db [_ user]]
  (assoc-in db [:user :role] (:role user)))

(defn get-user-details [cofx [_ auth-token]]
  {:http-xhrio {:method :get
                :uri "/api/users/me/"
                :timeout 5000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers {"Authorization" (str "Bearer " auth-token)
                          "Access-Control-Allow-Origin" "*"}
                :on-success [:user-details-retrieved]
                :on-failure [:request-failed]}})

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
                  :headers         {"Authorization" (str "Bearer " auth-token)
                                    "Access-Control-Allow-Origin" "*"}
                  :on-success      [:timers-retrieved]
                  :on-failure      [:request-failed]}}))


(defn timers-retrieved [db [_ timers]]
  (assoc db :timers (utils/->timer-map timers)))

(defn clients-retrieved [db [_ clients]]
  (let [transform (fn [{:keys [points-of-contact]}]
                    (zipmap (range) points-of-contact))]
    (assoc db
           :clients
           (map #(assoc % :points-of-contact (transform %))
              clients))))

(defn get-all-clients [{:keys [db] :as cofx} [_ auth-token]]
  {:db (ui-events/set-active-panel-handler db [:set-active-panel :clients])
   :http-xhrio {:method :get
                :uri "/api/clients/"
                :timeout 5000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization" (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:clients-retrieved]
                :on-failure      [:request-failed]}})

(defn create-client [{:keys [db] :as cofx} [_ data]]
  (let [token (get-in db [:user :token])]
    {:http-xhrio {:method          :post
                  :uri             "/api/clients/"
                  :headers         {"Authorization" (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:client-created]
                  :on-failure      [:client-creation-failed]}}))

(defn client-created [{:keys [db]}]
  {:dispatch-n [[:get-all-clients (get-in db [:user :token])]
                [:set-active-panel :clients]]
   :notify-success "Client created successfully."})

(defn client-creation-failed [{:keys [db]}]
  {:db (assoc db :client-creation-status "failed")
   :notify-error "Failed to create client"})

(defn http-failure [_ [_ e]]
  {:error e})

(defn init []
  (rf/reg-event-fx :request-failed http-failure)
  (rf/reg-event-fx :get-projects get-projects)
  (rf/reg-event-fx :get-timers get-timers)
  (rf/reg-event-fx :get-all-clients get-all-clients)
  (rf/reg-event-fx :create-client create-client)
  (rf/reg-event-fx :get-user-details get-user-details)

  (intr/tt-reg-event-db
   :projects-retrieved
   [intr/db-spec-inspector intr/->local-store]
   projects-retrieved)

  (intr/tt-reg-event-db
   :timers-retrieved
   [intr/db-spec-inspector intr/->local-store]
   timers-retrieved)

  (intr/tt-reg-event-db
   :user-details-retrieved
   [intr/db-spec-inspector intr/->local-store]
   user-details-retrieved)

  (intr/tt-reg-event-db
   :clients-retrieved
   [intr/db-spec-inspector intr/->local-store]
   clients-retrieved)

  (intr/tt-reg-event-fx :client-created client-created)
  (intr/tt-reg-event-fx :client-creation-failed client-creation-failed))
