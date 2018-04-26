(ns time-tracker-web-nxt.views.client
  (:require
   [re-frame.core :as rf]
   [re-frame-datatable.core :as rdt]
   [re-frame-datatable.views :as rdt-views]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]))


(defn points-of-contact [client]
  (let [update-poc (fn [path val]
                     (rf/dispatch [:update-point-of-contact path val]))]
    (conj
     [:div.poc-parent]
     (for [[id data] (:points-of-contact client)]
       ^{:key id}
       [:div.poc-child
        [:label.cclabel "Name: "]
        [:input.poc-input
         {:type      "text"
          :name      (str "poc-name-" id)
          :value     (:name data)
          :on-change #(update-poc [id :name] (common/element-value %))}]
        [:label.cclabel "Phone: "]
        [:input.poc-input
         {:type      "text"
          :name      (str "poc-phone-" id)
          :value     (:phone data)
          :on-change #(update-poc [id :phone] (common/element-value %))}]
        [:label.cclabel "Email: "]
        [:input.poc-input
         {:type      "text"
          :name      (str "poc-email-" id)
          :value     (:email data)
          :on-change #(update-poc [id :email] (common/element-value %))}]])
     [:a.link.link-primary
      {:href     "javascript:void(0)"
       :on-click #(rf/dispatch [:add-point-of-contact {:name      ""
                                                       :phone     ""
                                                       :email     ""
                                                       :action    "insert"
                                                       :client_id (:id client)}])}
      [:i.fa.fa-plus-square {:aria-hidden "true"}]
      [:span "Point of Contact"]])))

(defn client-form [{:keys [source source-update-fn submit cancel]}]
  [:div.create-client-form
   [:div
    [:label.cclabel "Name: "]
    [:input.ccinput {:type      "text"
                     :name      "name"
                     :value     (:name @source)
                     :on-change #(source-update-fn :name (common/element-value %))}]]
   [:div
    [:label.cclabel "Address: "]
    [:textarea.cctextarea {:name      "address"
                           :value     (:address @source)
                           :on-change #(source-update-fn :address (common/element-value %))}]]
   [:div
    [:label.cclabel "GSTIN: "]
    [:input.ccinput {:type      "text"
                     :name      "gstin"
                     :value     (:gstin @source)
                     :on-change #(source-update-fn :gstin (common/element-value %))}]]
   [:div
    [:label.cclabel "PAN: "]
    [:input.ccinput {:type      "text"
                     :name      "pan"
                     :value     (:pan @source)
                     :on-change #(source-update-fn :pan (common/element-value %))}]]
   [points-of-contact @source]
   [:div.button-group.actions
    [:button.btn.btn-primary
     {:type "input" :on-click (:handler submit)}
     (:name submit)]
    [:button.btn.btn-secondary
     {:type "input" :on-click (:handler cancel)}
     (:name cancel)]]])

(defn create-client-panel []
  (let [client         (rf/subscribe [:client])
        update-fn      (fn [k v]
                         (rf/dispatch [:update-client-details k v]))
        submit-handler #(rf/dispatch [:create-client @client])
        cancel-handler #(rf/dispatch [:cancel-form-and-return
                                      {:panel         :clients
                                       :remove-db-key :client}])]
    (fn []
      [:div.page
       [common/header]
       (client-form {:source           client
                     :source-update-fn update-fn
                     :submit           {:name    "Create"
                                        :handler submit-handler}
                     :cancel           {:name    "Cancel"
                                        :handler cancel-handler}})])))

(defn edit-client-panel []
  (let [client         (rf/subscribe [:client])
        update-fn      (fn [k v]
                         (rf/dispatch [:update-client-details k v]))
        submit-handler #(rf/dispatch [:update-client @client])
        cancel-handler #(rf/dispatch [:cancel-form-and-return
                                      {:panel         :clients
                                       :remove-db-key :client}])]
    (fn []
      [:div.page
       [common/header]
       (client-form {:source           client
                     :source-update-fn update-fn
                     :submit           {:name    "Update"
                                        :handler submit-handler}
                     :cancel           {:name    "Cancel"
                                        :handler cancel-handler}})])))

(defn clients-panel []
  [:div.page
   [common/header]
   [:div.panel
    [:button.btn.btn-primary
     {:type     "input"
      :on-click #(rf/dispatch [:goto :create-client])}
     "+ Add Client"]
    [rdt/datatable
     :client-datatable
     [:clients]
     [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
      {::rdt/column-key [:name] ::rdt/column-label "Name" ::rdt/sorting {::rdt/enabled? true}}
      {::rdt/column-key [:address] ::rdt/column-label "Address"}
      {::rdt/column-key [:gstin] ::rdt/column-label "GSTIN"}
      {::rdt/column-key [:pan] ::rdt/column-label "PAN"}
      {::rdt/column-key   []
       ::rdt/column-label ""
       ::rdt/render-fn
       (fn [client]
         [:a {:href     "javascript:void(0)"
              :on-click #(rf/dispatch [:show-edit-client-form client])}
          [:i.fa.fa-pencil-square-o {:aria-hidden "true"}]])}]
     {::rdt/pagination {::rdt/enabled? true
                        ::rdt/per-page 10}}]
    [rdt-views/default-pagination-controls
     :client-datatable
     [:clients]]]])
