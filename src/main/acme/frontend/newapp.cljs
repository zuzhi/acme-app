(ns acme.frontend.newapp
  (:require
   [helix.core :refer [defnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   ["react-dom/client" :as rdom]
   ["@instantdb/react" :as instantdb]))

;; ID for app: chaptify
(def app-id "71eb5a33-d683-4c63-8638-109df239ec0a")

;; Initialize the InstantDB
(def db (instantdb/init #js {:appId app-id}))

(defnc projects-page
  []
  (let [result (.useQuery db (clj->js {:projects {}}))
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (cond
      isLoading
      (d/div "loading")

      error
      (d/div (str "Error fetching data: " (.-message error) error))

      :else
      (d/ul
       (for [p projects] (d/li {:key (:id p)} (:name p)))))))

;; define components using the `defnc` macro
(defnc greeting
  "A component which greets a user."
  [{:keys [name]}]
  ;; use helix.dom to create DOM elements
  (d/div "Hello, " (d/strong name) "!"))

(defnc app []
  (let [[state set-state] (hooks/use-state {:name "Helix User"})]
    (d/div
     (d/h1 "Welcome!")
     ;; create elements out of components
     ($ greeting {:name (:name state)})
     (d/input {:value (:name state)
               :on-change #(set-state assoc :name (.. % -target -value))}))))

;; start your app with your favorite React renderer
(defn init
  []
  (let [root (rdom/createRoot (js/document.getElementById "root"))]
    (.render root ($ projects-page))))
