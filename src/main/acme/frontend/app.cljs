(ns acme.frontend.app
  (:require
    ["react" :as react]
    [clojure.string :as str]
    [reagent.core :as r]
    [reagent.dom :as rd]
    ["@instantdb/react" :as instantdb]
    [reagent.dom.client :as rdc]))


(defn simple-component
  []
  [:div
   [:p "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])


(defn simple-parent
  []
  [:div
   [:p "I include simple-component."]
   [simple-component]])


(defn hello-component
  [name]
  [:p "Hello, " name "!"])


(defn say-hello
  []
  [hello-component "world"])


(defn lister
  [items]
  [:ul
   (for [item items]
     ^{:key item} [:li "Item " item])])


(defn lister-user
  []
  [:div
   "Here is a list:"
   [lister (range 3)]])


(def click-count (r/atom 0))


(defn counting-component
  []
  [:div
   "The atom " [:code "click-count"] " has value: "
   [:code @click-count] "."
   [:input {:type "button" :value "Click me!"
            :on-click #(swap! click-count inc)}]])


(defn timer-component
  []
  (let [seconds-elapsed (r/atom 0)]
    (fn []
      (js/setTimeout #(swap! seconds-elapsed inc) 1000)
      [:div
       "Seconds Elapsed: " @seconds-elapsed])))


(defn atom-input
  [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])


(defn shared-state
  []
  (let [val (r/atom "foo")]
    (fn []
      [:div
       [:p "The value is now: " @val]
       [:p "Change it here: " [atom-input val]]])))


(defn calc-bmi
  [{:keys [height weight bmi] :as data}]
  (let [h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))


(def bmi-data (r/atom (calc-bmi {:height 180 :weight 80})))


(defn slider
  [param value min max invalidates]
  [:input {:type "range" :value value :min min :max max
           :style {:width "100%"}
           :on-change (fn [e]
                        (let [new-value (js/parseInt (.. e -target -value))]
                          (swap! bmi-data
                                 (fn [data]
                                   (-> data
                                       (assoc param new-value)
                                       (dissoc invalidates)
                                       calc-bmi)))))}])


(defn bmi-component
  []
  (let [{:keys [weight height bmi]} @bmi-data
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25) ["inherit" "normal"]
                           (< bmi 30) ["orange" "overweight"]
                           :else ["red" "obese"])]
    [:div
     [:h3 "BMI calculator"]
     [:div
      "Height: " (int height) "cm"
      [slider :height height 100 220 :bmi]]
     [:div
      "Weight: " (int weight) "kg"
      [slider :weight weight 30 150 :bmi]]
     [:div
      "BMI: " (int bmi) " "
      [:span {:style {:color color}} diagnose]
      [slider :bmi bmi 10 50 :weight]]]))


(defonce timer (r/atom (js/Date.)))

(defonce time-color (r/atom "#f34"))


(defonce time-updater (js/setInterval
                        #(reset! timer (js/Date.)) 1000))


(defn greeting
  [message]
  [:h1 message])


(defn clock
  []
  (let [time-str (-> @timer .toTimeString (str/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))


(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])


(defn simple-example
  []
  [:div
   [greeting "Hello world, it is now"]
   [clock]
   [color-input]])


(defonce todos (r/atom (sorted-map)))

(defonce counter (r/atom 0))


(defn add-todo
  [text]
  (let [id (swap! counter inc)]
    (swap! todos assoc id {:id id :title text :done false})))


(defn toggle
  [id]
  (swap! todos update-in [id :done] not))


(defn save
  [id title]
  (swap! todos assoc-in [id :title] title))


(defn delete
  [id]
  (swap! todos dissoc id))


(defn mmap
  [m f a]
  (->> m (f a) (into (empty m))))


(defn complete-all
  [v]
  (swap! todos mmap map #(assoc-in % [1 :done] v)))


(defn clear-done
  []
  (swap! todos mmap remove #(get-in % [1 :done])))


(defonce init-todos (do
                      (add-todo "Rename Cloact to Reagent")
                      (add-todo "Add undo demo")
                      (add-todo "Make all rendering async")
                      (add-todo "Allow any arguments to component functions")
                      (complete-all true)))


(defn todo-input
  [{:keys [title on-save on-stop input-ref]}]
  (let [val (r/atom title)]
    (fn [{:keys [id class placeholder]}]
      (let [stop (fn [_e]
                   (reset! val "")
                   (when on-stop (on-stop)))
            save (fn [e]
                   (let [v (-> @val str str/trim)]
                     (when-not (empty? v)
                       (on-save v))
                     (stop e)))]
        [:input {:type "text"
                 :value @val
                 :ref input-ref
                 :id id
                 :class class
                 :placeholder placeholder
                 :on-blur save
                 :on-change (fn [e]
                              (reset! val (-> e .-target .-value)))
                 :on-key-down (fn [e]
                                (case (.-which e)
                                  13 (save e)
                                  27 (stop e)
                                  nil))}]))))


(defn todo-edit
  [props]
  (let [ref (react/useRef)]
    (react/useEffect (fn []
                       (.focus (.-current ref))
                       js/undefined))
    [todo-input (assoc props :input-ref ref)]))


(defn todo-stats
  [{:keys [filt active done]}]
  (let [props-for (fn [name]
                    {:class (when (= name @filt) "selected")
                     :on-click #(reset! filt name)})]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :done) "Complete"]]]
     (when (pos? done)
       [:button#clear-completed {:on-click clear-done}
        "Clear completed " done])]))


(defn todo-item
  []
  (let [editing (r/atom false)]
    (fn [{:keys [id done title]}]
      [:li
       {:class [(when done "completed ")
                (when @editing "editing")]}
       [:div.view
        [:input.toggle
         {:type "checkbox"
          :checked done
          :on-change #(toggle id)}]
        [:label
         {:on-double-click #(reset! editing true)}
         title]
        [:button.destroy {:on-click #(delete id)}]]
       (when @editing
         [:f> todo-edit {:class "edit"
                         :title title
                         :on-save #(save id %)
                         :on-stop #(reset! editing false)}])])))


(defn todo-app
  []
  (let [filt (r/atom :all)]
    (fn []
      (let [items (vals @todos)
            done (->> items (filter :done) count)
            active (- (count items) done)]
        [:div
         [:section#todoapp
          [:header#header
           [:h1 "todos"]
           [todo-input {:id "new-todo"
                        :placeholder "What needs to be done?"
                        :on-save add-todo}]]
          (when (-> items count pos?)
            [:div
             [:section#main
              [:input#toggle-all {:type "checkbox" :checked (zero? active)
                                  :on-change #(complete-all (pos? active))}]
              [:label {:for "toggle-all"} "Mark all as complete"]
              [:ul#todo-list
               (for [todo (filter (case @filt
                                    :active (complement :done)
                                    :done :done
                                    :all identity) items)]
                 ^{:key (:id todo)} [todo-item todo])]]
             [:footer#footer
              [todo-stats {:active active :done done :filt filt}]]])]
         [:footer#info
          [:p "Double-click to edit a todo"]]]))))

;; ID for app: chaptify
(def app-id "71eb5a33-d683-4c63-8638-109df239ec0a")

;; Initialize the InstantDB
(def db (instantdb/init #js {:appId app-id}))

(defn projects-page
  []
  (let [query {:projects {:$ {:where {:status "normal"}}}}
        result (.useQuery db (clj->js query))
        {:keys [isLoading error data]} (js->clj result :keywordize-keys true)
        projects (or (:projects data) [])]
    (cond
      isLoading
      [:div "loading"]

      error
      [:div (str "Error fetching data: " (.-message error))]

      :else
      [:ul
       (for [p projects] [:li {:key (:id p)} (:name p)])])))


(defn init
  []
  (let [root (rdc/create-root (js/document.getElementById "root"))]
    (rdc/render root [:div
                      [:f> projects-page]
                      [simple-parent]
                      [say-hello]
                      [lister-user]
                      [counting-component]
                      [timer-component]
                      [shared-state]
                      [:p
                       "My BMI is: " (:bmi (calc-bmi {:height 172 :weight 57}))]
                      [bmi-component]
                      [simple-example]
                      [todo-app]])))
