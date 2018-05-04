(ns time-tracker-web-nxt.views.client
  (:require
   [re-frame.core :as rf]
   [re-frame-datatable.core :as rdt]
   [re-frame-datatable.views :as rdt-views]
   [reagent.core :as reagent]
   [time-tracker-web-nxt.views.common :as common]
   [time-tracker-web-nxt.views.project :as project]))

(def ^:private empty-client
  {:name    ""
   :address ""
   :gstin   ""
   :pan     ""
   :points-of-contact {}})

(defn points-of-contact [client]
  (let [poc (reagent/atom (:points-of-contact @client))
        update-poc (fn [path v]
                     (swap! poc assoc-in path v)
                     (swap! client assoc :points-of-contact @poc))]
    (fn []
      (conj
       [:div.poc-parent]
       (for [[id data] (:points-of-contact @client)]
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
         :on-click (fn []
                     (let [k (inc (count @poc))]
                       (swap! poc assoc k {:name      ""
                                           :phone     ""
                                           :email     ""
                                           :action    "insert"
                                           :client_id (:id @client)})
                       (swap! client assoc :points-of-contact @poc)))}
        [:i.fa.fa-plus-square {:aria-hidden "true"}]
        [:span "Point of Contact"]]))))

(defn client-form [{:keys [source source-update-fn submit cancel show?]}]
  [:div.create-client-form {:style (if @show? {} {:display "none"})}
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
   [points-of-contact source]
   [:div.button-group.actions
    [:button.btn.btn-primary
     {:type "input" :on-click (:handler submit)}
     (:name submit)]
    [:button.btn.btn-secondary
     {:type "input" :on-click (:handler cancel)}
     (:name cancel)]]])

(defn create-client-form [show?]
  (let [client         (reagent/atom empty-client)
        update-fn      (fn [k v]
                         (swap! client assoc k v))
        submit-handler #(rf/dispatch [:create-client @client])
        cancel-handler (fn []
                         (reset! show? false))]
    [:div.create-client-form
     {:style (if @show? {} {:display "none"})}
     [:h2 "Create a client"]
     [client-form {:source           client
                   :source-update-fn update-fn
                   :submit           {:name    "Create"
                                      :handler submit-handler}
                   :cancel           {:name    "Cancel"
                                      :handler cancel-handler}
                   :show?            show?}]]))

(defn edit-client-form [show? client]
  (let [update-fn      (fn [k v]
                         (swap! client assoc k v))
        submit-handler (fn []
                         (rf/dispatch [:update-client @client])
                         (reset! show? false))
        cancel-handler (fn []
                         (reset! show? false))]
    [client-form {:source           client
                  :source-update-fn update-fn
                  :submit           {:name    "Update"
                                     :handler submit-handler}
                  :cancel           {:name    "Cancel"
                                     :handler cancel-handler}
                  :show?            show?}]))

(defn client-panel []
  (let [selected-client-id     (rf/subscribe [:selected-client])
        all-clients            (rf/subscribe [:clients])
        selected-client        (reagent/atom (first (filter #(= (:id %) @selected-client-id) @all-clients)))
        show-project-form?     (reagent/atom false)
        show-edit-client-form? (reagent/atom false)]
    (fn []
      [:div.page
       [common/header]
       [:div.panel
        [:h2 (:name @selected-client)]
        [:hr]
        [:br]
        [project/project-form show-project-form?]
        [edit-client-form show-edit-client-form? selected-client]
        [:button.btn.btn-primary
         {:type     "input"
          :on-click #(reset! show-project-form? true)
          :style    (if-not @show-project-form? {} {:display "none"})}
         "+ Add Project"]
        [:button.btn.btn-primary
         {:type     "input"
          :on-click (fn []
                      (reset! show-edit-client-form? true)
                      (.log js/console "selected-client" @selected-client)
                      (rf/dispatch [:show-edit-client-form @selected-client]))
          :style    (if-not @show-edit-client-form? {} {:display "none"})}
         "Edit Client"]
        [rdt/datatable
         :project-datatable
         [:projects-for-client]
         [#_{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
          {::rdt/column-key [] ::rdt/column-label "Project Name" ::rdt/sorting {::rdt/enabled? true}
           ::rdt/render-fn (fn [project]
                             [:a {:href (str "/clients/" @selected-client-id "/projects/" (:id project))}
                              (:name project)])}]
         {::rdt/pagination {::rdt/enabled? true
                            ::rdt/per-page 10}}]
        [rdt-views/default-pagination-controls
         :project-datatable
         [:projects-for-client]]]])))

(defn clients-panel []
  (let [show-client-creation-form? (reagent/atom false)]
    (fn []
      [:div.page
       [common/header]
       [:div.panel
        [:button.btn.btn-primary
         {:type     "input"
          :on-click #(reset! show-client-creation-form? true)
          :style    (if-not @show-client-creation-form? {} {:display "none"})}
         "+ Add Client"]
        [create-client-form show-client-creation-form?]

        [rdt/datatable
         :client-datatable
         [:clients]
         [{::rdt/column-key [:id] ::rdt/column-label "#" ::rdt/sorting {::rdt/enabled? true}}
          {::rdt/column-key   []
           ::rdt/column-label "Name"
           ::rdt/sorting      {::rdt/enabled? true}
           ::rdt/render-fn
           (fn [client]
             [:a {:href (str "/clients/" (:id client) "/")}
              (:name client)])}
          {::rdt/column-key [:address] ::rdt/column-label "Address"}
          {::rdt/column-key [:gstin] ::rdt/column-label "GSTIN"}
          {::rdt/column-key [:pan] ::rdt/column-label "PAN"}]
         {::rdt/pagination {::rdt/enabled? true
                            ::rdt/per-page 10}}]
        [rdt-views/default-pagination-controls
         :client-datatable
         [:clients]]]])))
