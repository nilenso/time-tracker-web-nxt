(ns time-tracker-web-nxt.events.ui
  (:require
   [cljsjs.toastr]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector tt-reg-event-db]]))


(defn set-active-panel-handler [db [_ panel]]
  (-> db
      (assoc :active-panel panel)
      (assoc :show-user-menu? false)))

(defn hide-task-form
  [db _]
  (assoc db :show-task-form? false))

(defn show-task-form
  [db _]
  (assoc db :show-task-form? true))

(defn init []

  ;; Set options for Toastr.js notifications
  (aset js/toastr
        "options"
        (clj->js {:closeButton       false
                  :debug             false
                  :newestOnTop       true
                  :progressBar       false
                  :positionClass     "toast-top-center"
                  :preventDuplicates true
                  :onclick           nil
                  :showDuration      "20000"
                  :hideDuration      "1000"
                  :timeOut           "1500"
                  :extendedTimeOut   "1000"
                  :showEasing        "swing"
                  :hideEasing        "linear"
                  :showMethod        "fadeIn"
                  :hideMethod        "fadeOut"}))

  (tt-reg-event-db
   :show-widget
   [db-spec-inspector]
   (fn [db _]
     (assoc db :show-create-timer-widget? true)))

  (rf/reg-event-db
   :hide-widget
   (fn [db _]
     (assoc db :show-create-timer-widget? false)))

  (rf/reg-event-db
   :set-active-panel
   set-active-panel-handler)

  (rf/reg-event-db
   :toggle-user-menu
   (fn [db _]
     (let [status (:show-user-menu? db)]
       (assoc db :show-user-menu? (not status)))))

  (rf/reg-event-db
   :hide-task-form
   hide-task-form)

  (rf/reg-event-db
   :show-task-form
   show-task-form)

  (rf/reg-event-db
   :select-client
   (fn [db [_ id]]
     (assoc db :selected-client-id id)))

  (rf/reg-event-db
   :select-project
   (fn [db [_ id]]
     (assoc db :selected-project-id id)))

  (rf/reg-event-db
   :select-task
   (fn [db [_ id]]
     (assoc db :selected-task-id id)))

  (rf/reg-event-fx
   :show-notification
   (fn [cofx [_ type msg]]
     (condp = type
       :success {:notify-success msg}
       :error   {:notify-error msg}
       {})))

  (rf/reg-fx
   :notify-success
   (fn [msg]
     ((aget js/toastr "success") msg)))

  (rf/reg-fx
   :notify-error
   (fn [error-msg]
     ((aget js/toastr "error") error-msg))))
