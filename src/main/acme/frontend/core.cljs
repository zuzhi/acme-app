(ns acme.frontend.core
  (:require
   [helix.core :refer [$]]
   [reagent.core :as r]
   [reitit.frontend :as rt]
   [reitit.frontend.easy :as rt-easy]
   [acme.frontend.app :as app]
   [acme.frontend.newapp :as newapp]
   [reagent.dom.client :as rdc]))

(defn app-page
  []
  [app/simple-component])

(defn newapp-page
  []
  ($ newapp/app))

(defn profile
  [name tab]
  [:div
   [:p "hi, " name]
   [:p "the tab is: " tab]])

(defn profile-page
  [{:keys [name]} {:keys [tab]}]
  [profile name tab])

(defn root []
  [:div.header
   [:a {:href (rt-easy/href ::app)} "app"]
   [:a {:href (rt-easy/href ::newapp)
        :style {:padding-left 10}} "newapp"]
   [:a {:href (rt-easy/href ::profile {:name "zuzhi"} {:tab "test"})
        :style {:padding-left 10}} "profile"]])

(def routes
   [["/" {:name ::root
          :view root}]
    ["/app" {:name ::app
             :view app-page}]
    ["/newapp" {:name ::newapp
                :view newapp-page}]
    ["/profile/:name" {:name ::profile
                       :view profile-page}]])

(def router
  (rt/router routes))

(defonce app-state (r/atom {:current-view nil}))

(defn on-navigate
  [match _]
  (let [view (:view (:data match))
        route-params (:path-params match)
        query-params (:query-params match)]
    (swap! app-state assoc :current-view view
           :route-params route-params
           :query-params query-params)))

(defn init-router
  []
  (rt-easy/start!
   router
   on-navigate
   {:use-fragment false}))

(defn app []
  (let [current-view (:current-view @app-state)
        route-params (:route-params @app-state)
        query-params (:query-params @app-state)]
    (if current-view
      [current-view route-params query-params]
      [:div "Page not found"])))

(defn mount-root
  []
  (let [root (rdc/create-root (js/document.getElementById "root"))]
    (rdc/render root [:div
                      [app]])))

(defn init
  []
  (init-router)
  (mount-root))
