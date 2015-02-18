(ns webexample.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [immutant.web]
            [immutant.transactions]
            [immutant.transactions.jdbc]
            [compojure.core :refer [defroutes routes ANY GET POST PUT DELETE]]
            [compojure.route]
            [liberator.core :refer [resource]]
            [yesql.core :refer [defqueries]])
  (:gen-class))

;;; utilities

(defn as-json [obj & options]
  (condp instance? obj
    String (apply json/read-str obj options)
    java.io.InputStream (apply json/read (io/reader obj) options)
    java.io.Reader (apply json/read (io/reader obj) options)))

;;; services

(defqueries "data/task.sql")

(defroutes task-service
  (GET "/task" {:keys [db]}
    (resource
      :allowed-methods [:get]
      :available-media-types ["application/json"]
      :handle-ok (list-tasks db)))

  (POST "/task" {:keys [db body]}
    (let [data (as-json body :key-fn keyword)
          new-id (or (:newid (first (next-task-id db))) 0)]
      (resource
        :allowed-methods [:post]
        :available-media-types ["application/json"]
        :malformed? (nil? (:title data))
        :handle-malformed (pr-str data)
        :post! (fn [_] (new-task! db new-id (:title data)))
        :handle-created {:id new-id})))

  (GET "/task/:id" [id :as {:keys [db]}]
    (resource
      :allowed-methods [:get]
      :available-media-types ["application/json"]
      :handle-ok (fn [_] (first (get-task db)))))

  (PUT "/task/:id" [id :as {:keys [db body]}]
    (let [data (as-json body :key-fn keyword)]
      (resource
        :allowed-methods [:put]
        :available-media-types ["application/json"]
        :malformed? (nil? (:title data))
        :put! (fn [_] (update-task! db (:title data) id))
        :new? false)))

  (DELETE "/task/:id" [id :as {:keys [db]}]
    (resource
      :allowed-methods [:delete]
      :available-media-types ["application/json"]
      :delete! (fn [_] (delete-task! db id)))))


;;; configuration

(defn wrap-transaction [handler dbspec]
  (fn [request]
    (immutant.transactions/transaction
      (sql/with-db-transaction [txn dbspec :isolation :serializable]
        (handler (assoc request :db txn))))))

(defn handler [& {db :db}]
  (routes
    (-> task-service
        (wrap-transaction db))
    (ANY "/" [] (io/resource "public/index.html"))
    (compojure.route/resources "/" :root "public")
    (ANY "*" []
         (resource
           :available-media-types ["*"]
           :exists? false))))

(defn make-db [db]
  (create-task-table! db))

(defn run []
  (let [db {:classname "org.h2.Driver"
            :subprotocol "h2"
            :subname "./db/test"
            :factory immutant.transactions.jdbc/factory}]
    (make-db db)
    (immutant.web/run (handler :db db) :path "/webexample")))

(defn stop []
  (immutant.web/stop))

(defn -main []
  (let [db {:name "java:/datasource/example"
            :factory immutant.transactions.jdbc/factory}]
    (make-db db)
    (immutant.web/run (handler :db db))))
