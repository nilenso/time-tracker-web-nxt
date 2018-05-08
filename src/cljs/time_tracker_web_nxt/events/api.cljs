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

(defn registered-users-retrieved [db [_ user-list]]
  (assoc db :registered-users user-list))

(defn get-registered-users [cofx [_ auth-token]]
  {:http-xhrio {:method          :get
                :uri             "/api/users/"
                :timeout         5000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization"               (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:registered-users-retrieved]
                :on-failure      [:request-failed]}})

(defn user-details-retrieved [db [_ user]]
  (assoc-in db [:user :role] (:role user)))

(defn get-user-details [cofx [_ auth-token]]
  {:http-xhrio {:method          :get
                :uri             "/api/users/me/"
                :timeout         5000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization"               (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:user-details-retrieved]
                :on-failure      [:request-failed]}})

(defn get-projects [cofx [_ auth-token]]
  {:http-xhrio {:method          :get
                :uri             "/api/projects/"
                :timeout         8000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization"               (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:projects-retrieved]
                :on-failure      [:request-failed]}})

(defn get-tasks [cofx [_ auth-token]]
  {:http-xhrio {:method          :get
                :uri             "/api/tasks/"
                :timeout         8000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization"               (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:tasks-retrieved]
                :on-failure      [:request-failed]}})

(defn projects-retrieved [db [_ projects]]
  (assoc db :projects projects))

(defn project-created [{:keys [db]}]
  {:dispatch       [:get-projects (get-in db [:user :token])]
   :notify-success "Project created successfully."})

(defn project-creation-failed [{:keys [db]}]
  {:notify-error "Failed to create project"})

(defn create-project
  [{:keys [db] :as cofx} [_ data]]
  (let [token (get-in db [:user :token])]
    {:http-xhrio {:method          :post
                  :uri             "/api/projects/"
                  :headers         {"Authorization"               (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:project-created]
                  :on-failure      [:project-creation-failed]}}))

(defn user-invited [{:keys [db]}]
  {:notify-success "User invited successfully."})

(defn user-invitation-failed
  [{:keys [db]}]
  {:notify-error "Failed to invite user"})

(defn invite-user
  [{:keys [db] :as cofx} [_ data]]
  (let [token (get-in db [:user :token])]
    {:http-xhrio {:method          :post
                  :uri             "/api/invited-users/"
                  :headers         {"Authorization"               (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:user-invited]
                  :on-failure      [:user-invitation-failed]}}))

(defn tasks-retrieved [db [_ tasks]]
  (assoc db :tasks tasks))

(defn task-created
  [{:keys [db]}]
  {:dispatch       [:get-tasks (get-in db [:user :token])]
   :notify-success "Task created successfully."})

(defn task-creation-failed
  [cofx]
  {:notify-error "Failed to create task"})

(defn create-task
  [{:keys [db] :as cofx} [_ data]]
  (let [token (get-in db [:user :token])]
    {:http-xhrio {:method          :post
                  :uri             "/api/tasks/"
                  :headers         {"Authorization"               (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:task-created]
                  :on-failure      [:task-creation-failed]}}))

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
  (when-let [running-timer (first (filter :started-time timers))]
    (rf/dispatch [:start-timer (select-keys running-timer [:id])]))
  (assoc db :timers (utils/->timer-map timers)))

(defn clients-retrieved [{:keys [db] :as cofx} [_ clients]]
  (let [transform (fn [{:keys [points-of-contact]}]
                    (zipmap (range) points-of-contact))]
    {:db       (assoc db
                      :clients
                      (map #(assoc % :points-of-contact (transform %))
                           clients))}))

(defn get-clients [{:keys [db] :as cofx} [_ auth-token]]
  {:http-xhrio {:method          :get
                :uri             "/api/clients/"
                :timeout         5000
                :response-format (ajax/json-response-format {:keywords? true})
                :headers         {"Authorization"               (str "Bearer " auth-token)
                                  "Access-Control-Allow-Origin" "*"}
                :on-success      [:clients-retrieved]
                :on-failure      [:request-failed]}})

(defn create-client [{:keys [db] :as cofx} [_ data]]
  (let [token (get-in db [:user :token])
        pocs  (or (vals (:points-of-contact data)) [])
        data  (assoc data :points-of-contact pocs)]
    {:http-xhrio {:method          :post
                  :uri             "/api/clients/"
                  :headers         {"Authorization"               (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:client-created]
                  :on-failure      [:client-creation-failed]}}))

(defn client-created [{:keys [db]}]
  {:dispatch-n     [[:get-clients (get-in db [:user :token])]
                    [:goto [:clients]]]
   :notify-success "Client created successfully."})

(defn client-creation-failed [{:keys [db]}]
  {:notify-error "Failed to create client"})

(defn update-client [{:keys [db] :as cofx} [_ data]]
  (let [token     (get-in db [:user :token])
        client-id (:id data)
        pocs      (or (vals (:points-of-contact data)) [])
        data      (assoc data :points-of-contact pocs)]
    {:http-xhrio {:method          :put
                  :uri             (str "/api/clients/" client-id "/")
                  :headers         {"Authorization"               (str "Bearer " token)
                                    "Access-Control-Allow-Origin" "*"}
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:client-updated]
                  :on-failure      [:client-update-failed]}}))

(defn client-updated [{:keys [db]}]
  {:dispatch     [:get-clients (get-in db [:user :token])]
   :notify-success "Client updated successfully."})

(defn client-update-failed [{:keys [db]}]
  {:notify-error "Failed to update client"})

(defn http-failure [_ [_ e]]
  (cond
    (#{401 403} (:status e)) {:notify-error "Could not authenticate"}
    :else                    {:notify-error "Could not connect to server"}))

(defn init []
  (rf/reg-event-fx :request-failed http-failure)

  (rf/reg-event-fx :get-clients get-clients)
  (rf/reg-event-fx :create-client create-client)
  (rf/reg-event-fx :update-client update-client)

  (rf/reg-event-fx :get-projects get-projects)
  (rf/reg-event-fx :create-project create-project)

  (rf/reg-event-fx :get-tasks get-tasks)
  (rf/reg-event-fx :create-task create-task)

  (rf/reg-event-fx :get-timers get-timers)

  (rf/reg-event-fx :get-registered-users get-registered-users)
  (rf/reg-event-fx :get-user-details get-user-details)
  (rf/reg-event-fx :invite-user invite-user)
  (intr/tt-reg-event-db
   :projects-retrieved
   [intr/db-spec-inspector]
   projects-retrieved)

  (intr/tt-reg-event-db
   :tasks-retrieved
   [intr/db-spec-inspector]
   tasks-retrieved)

  (intr/tt-reg-event-db
   :timers-retrieved
   [intr/db-spec-inspector]
   timers-retrieved)

  (intr/tt-reg-event-db
   :registered-users-retrieved
   [intr/db-spec-inspector]
   registered-users-retrieved)

  (intr/tt-reg-event-db
   :user-details-retrieved
   [intr/db-spec-inspector]
   user-details-retrieved)

  (intr/tt-reg-event-fx
   :clients-retrieved
   [intr/db-spec-inspector]
   clients-retrieved)

  (intr/tt-reg-event-fx
   :client-updated
   [intr/db-spec-inspector]
   client-updated)

  (intr/tt-reg-event-fx :client-update-failed client-update-failed)
  (intr/tt-reg-event-fx :client-created client-created)
  (intr/tt-reg-event-fx :client-creation-failed client-creation-failed)

  (intr/tt-reg-event-fx :project-created project-created)
  (intr/tt-reg-event-fx :project-creation-failed project-creation-failed)

  (intr/tt-reg-event-fx :user-invited user-invited)
  (intr/tt-reg-event-fx :user-invitation-failed user-invitation-failed)

  (intr/tt-reg-event-fx :task-created task-created)
  (intr/tt-reg-event-fx :task-creation-failed task-creation-failed))
