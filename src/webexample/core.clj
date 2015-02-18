(ns webexample.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [immutant.web]
            [immutant.transactions]
            [immutant.transactions.jdbc]
            [compojure.core :refer [defroutes routes ANY]]
            [compojure.route]
            [liberator.core :refer [resource]])
  (:gen-class))

;;; utilities

(defn as-json [obj]
  (condp instance? obj
    String (json/read-str obj)
    java.io.InputStream (json/read (io/reader obj))
    java.io.Reader (json/read (io/reader obj))
    obj))

;;; services

(defroutes task-service
  (ANY "/task" {:keys [request-method db body]}
       (let [data (when (= request-method :post) (as-json body))
             new-id (when (= request-method :post)
                      (let [r (sql/query
                                db ["SELECT MAX(id)+1 AS newid FROM task"])]
                        (or (:newid (first r)) 0)))]
         (resource
           :allowed-methods [:get :post]
           :malformed? (and (= request-method :post) (nil? data))
           :available-media-types ["application/json"]
           :handle-ok (sql/query
                        db ["SELECT id, title FROM task ORDER BY id DESC"])
           :handle-malformed (pr-str data)
           :post!
           (fn [ctx]
             (let [title (get data "title")]
               (sql/execute! db ["INSERT INTO task (id, title) VALUES (?, ?)"
                                 new-id title])))
           :handle-created {:id new-id})))

  (ANY "/task/:id" [id :as {:keys [request-method db body]}]
       (let [data (when (= request-method :put) (as-json body))]
         (resource
           :allowed-methods [:get :put :delete]
           :available-media-types ["application/json"]
           :malformed? (and (= request-method :put) (nil? data))
           :handle-ok
           (fn [_] (->> (sql/query
                          db ["SELECT id, title FROM task WHERE id = ?" id])
                        first))
           :put!
           (fn [_]
             (let [title (get data "title")]
               (sql/execute! db ["UPDATE task SET title = ? WHERE id = ?"
                                 title id])))
           :new? false
           :delete!
           (fn [_] (sql/execute! db ["DELETE FROM task WHERE id = ?" id]))))))


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
  (sql/execute!
    db
    [(str "CREATE TABLE IF NOT EXISTS task ("
          " id INTEGER PRIMARY KEY,"
          " title VARCHAR(255))")]))

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
