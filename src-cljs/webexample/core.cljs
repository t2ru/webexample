(ns webexample.core
  (:require [clojure.browser.dom :as dom]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))

;;; handlers

(defn list-tasks [a]
  (GET "/task"
       {:handler #(reset! a %)
        :error-handler #(js/alert %)}))

(defn add-task [a]
  (let [elem (dom/get-element "newtask")
        title (.-value elem)
        f (fn [v response] (cons {"id" (get response "id") "title" title} v))]
    (when-not (= title "")
      (set! (.-value elem) "")
      (POST "/task"
            {:params {:title title}
             :format :json
             :handler #(swap! a f %)
             :error-handler #(js/alert %)})))
  false)

(defn update-task [a id]
  (let [elem (dom/get-element "newtitle")
        new-title (.-value elem)
        f (fn [v] (map (fn [{xid "id" xtitle "title" :as t}]
                         (if (= id xid) {"id" id "title" new-title} t)) v))]
    (when-not (= new-title "")
      (PUT (str "/task/" id)
           {:params {:title new-title}
            :format :json
            :handler #(swap! a f)
            :error-handler #(js/alert %)})))
  false)

(defn delete-task [a id]
  (let [f (fn [v] (filter #(not= id (get % "id")) v))]
    (DELETE (str "/task/" id)
            {:format :json
             :handler #(swap! a f)
             :error-handler #(js/alert %)})))


;;; dom generation

(defn task-app []
  (let [task-list (atom nil)
        editing (atom nil)]
    (list-tasks task-list)
    (fn []
      [:div
       [:input {:type :submit :value "Sync"
                :on-click (fn []
                             (reset! editing nil)
                             (list-tasks task-list))}]
       [:form {:on-submit #(add-task task-list)}
        [:input#newtask {:type :text}]
        [:input {:type :submit}]]
       (->> (for [task @task-list
                  :let [{id "id" title "title"} task]]
              [:li {:on-click #(reset! editing id)}
               id " "
               (if (= id @editing)
                 [:input#newtitle
                  {:type :text :defaultValue title
                   :on-blur
                   (fn []
                     (reset! editing nil)
                     (update-task task-list id))}]
                 [:span title " "
                  [:span {:on-click #(delete-task task-list id)} "[x]"]])])
            (cons :ul)
            vec)])))

(defn main-app []
  [:div
   [:h1 "list tasks"]
   [(task-app)]])

(defn ^:export run []
  (reagent/render-component [main-app] (.-body js/document)))
