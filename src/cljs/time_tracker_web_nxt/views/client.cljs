(ns time-tracker-web-nxt.views.client
  (:require
   [re-frame.core :as rf]
   [re-frame-datatable.core :as rdt]
   [re-frame-datatable.views :as rdt-views]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]
   [time-tracker-web-nxt.views.project :as project]))


(defn points-of-contact [client]
  (let [update-poc (fn [path val]
                     (rf/dispatch [:update-point-of-contact path val]))]
    (conj
     [:div.poc-parent]
     (for [[id data] (:points-of-contact client)]
       ^{:key id}
       [:div.poc-child
        [:label.cclabel "Name: "]
        [common/input
         {:type      "text"
          :name      (str "poc-name-" id)
          :value     (:name data)
          :class     "poc-input"
          :on-change #(update-poc [id :name] %)}]
        [:label.cclabel "Phone: "]
        [common/input
         {:type      "text"
          :name      (str "poc-phone-" id)
          :value     (:phone data)
          :class     "poc-input"
          :on-change #(update-poc [id :phone] %)}]
        [:label.cclabel "Email: "]
        [common/input
         {:type      "text"
          :name      (str "poc-email-" id)
          :value     (:email data)
          :class     "poc-input"
          :on-change #(update-poc [id :email] %)}]])
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
    [common/input {:type      "text"
                   :name      "name"
                   :value     (:name @source)
                   :class     "ccinput"
                   :on-change #(source-update-fn :name %)}]]
   [:div
    [:label.cclabel "Address: "]
    [:textarea.cctextarea {:name      "address"
                           :value     (:address @source)
                           :on-change #(source-update-fn :address (common/element-value %))}]]
   [:div
    [:label.cclabel "GSTIN: "]
    [common/input {:type      "text"
                   :name      "gstin"
                   :value     (:gstin @source)
                   :class     "ccinput"
                   :on-change #(source-update-fn :gstin %)}]]
   [:div
    [:label.cclabel "PAN: "]
    [common/input {:type      "text"
                   :name      "pan"
                   :value     (:pan @source)
                   :class     "ccinput"
                   :on-change #(source-update-fn :pan %)}]]
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
    [:div.page
     [common/header]
     [client-form {:source           client
                   :source-update-fn update-fn
                   :submit           {:name    "Create"
                                      :handler submit-handler}
                   :cancel           {:name    "Cancel"
                                      :handler cancel-handler}}]]))

(defn edit-client-panel []
  (let [client         (rf/subscribe [:client])
        update-fn      (fn [k v]
                         (rf/dispatch [:update-client-details k v]))
        submit-handler #(rf/dispatch [:update-client @client])
        cancel-handler #(rf/dispatch [:cancel-form-and-return
                                      {:panel         :clients
                                       :remove-db-key :client}])]
    [:div.page
     [common/header]
     [client-form {:source           client
                    :source-update-fn update-fn
                    :submit           {:name    "Update"
                                       :handler submit-handler}
                    :cancel           {:name    "Cancel"
                                       :handler cancel-handler}}]]))

(defn client-panel []
  (let [selected-client-id (rf/subscribe [:selected-client])
        all-clients (rf/subscribe [:clients])
        selected-client (first (filter #(= (:id %) @selected-client-id) @all-clients))]
    (fn []
      [:div.page
       [common/header]
       ;; TODO: Show name of client and other information nicely
       [:h3 (:name selected-client)]
       [project/project-form]
       [:div.panel
        #_[:button.btn.btn-primary
         {:type     "input"
          :on-click #(rf/dispatch [:goto [:create-project]])}
         "+ Add Project"]
        [rdt/datatable
         :project-datatable
         [:projects]
         [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
          {::rdt/column-key [:name] ::rdt/column-label "Name" ::rdt/sorting {::rdt/enabled? true}}]
         {::rdt/pagination {::rdt/enabled? true
                            ::rdt/per-page 10}}]
        [rdt-views/default-pagination-controls
         :project-datatable
         [:projects]]]])))

(defn clients-panel []
  [:div.page
   [common/header]
   [:div.panel
    [:button.btn.btn-primary
     {:type     "input"
      :on-click #(rf/dispatch [:goto [:create-client]])}
     "+ Add Client"]
    [rdt/datatable
     :client-datatable
     [:clients]
     [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
      {::rdt/column-key []
       ::rdt/column-label "Name"
       ::rdt/sorting {::rdt/enabled? true}
       ::rdt/render-fn
       (fn [client]
         [:a {:href (str "/clients/" (:id client))}
          (:name client)])}
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
