(ns time-tracker-web-nxt.events.ui
  (:require
   [cljsjs.toastr]
   [re-frame.core :as rf]
   [time-tracker-web-nxt.interceptors :refer [db-spec-inspector tt-reg-event-db]]))


(defn set-active-panel-handler [db [_ panel]]
  (let [status (:show-user-menu? db)]
    (-> db
       (assoc :active-panel panel)
       (assoc :show-user-menu? false))))

(defn init []

  ;; Set options for Toastr.js notifications
  (aset js/toastr
        "options"
        (clj->js {:closeButton false
                  :debug false
                  :newestOnTop true
                  :progressBar false
                  :positionClass "toast-top-center"
                  :preventDuplicates true
                  :onclick nil
                  :showDuration "20000"
                  :hideDuration "1000"
                  :timeOut "1500"
                  :extendedTimeOut "1000"
                  :showEasing "swing"
                  :hideEasing "linear"
                  :showMethod "fadeIn"
                  :hideMethod "fadeOut"}))

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
   :show-edit-client-form
   (fn [db [_ client]]
     (-> db
        (assoc :client client)
        (set-active-panel-handler [:set-active-panel :edit-client]))))

  (rf/reg-event-db
   :cancel-form-and-return
   (fn [db [_ {:keys [remove-db-key panel]}]]
     (-> (if remove-db-key (dissoc db remove-db-key) db)
        (set-active-panel-handler [:set-active-panel panel]))))

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
