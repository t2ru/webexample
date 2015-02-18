(ns webexample.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [immutant.web]
            [immutant.transactions]
            [immutant.transactions.jdbc]
            [compojure.core :refer [defroutes routes ANY]]
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
  (ANY "/task" {:keys [request-method db body]}
       (let [data (when (= request-method :post) (as-json body :key-fn keyword))
             new-id (when (= request-method :post)
                      (let [r (next-task-id db)]
                        (or (:newid (first r)) 0)))]
         (resource
           :allowed-methods [:get :post]
           :malformed? (and (= request-method :post) (nil? data))
           :available-media-types ["application/json"]
           :handle-ok (list-tasks db)
           :handle-malformed (pr-str data)
           :post! (fn [ctx] (new-task! db new-id (:title data)))
           :handle-created {:id new-id})))

  (ANY "/task/:id" [id :as {:keys [request-method db body]}]
       (let [data (when (= request-method :put) (as-json body :key-fn keyword))]
         (resource
           :allowed-methods [:get :put :delete]
           :available-media-types ["application/json"]
           :malformed? (and (= request-method :put) (nil? data))
           :handle-ok (fn [_] (first (get-task db)))
           :put!  (fn [_] (update-task! db (:title data) id))
           :new? false
           :delete! (fn [_] (delete-task! db id))))))


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
