(defproject webexample "0.1.0-SNAPSHOT"
  :description "example of web service"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2760"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/data.json "0.2.5"]
                 [org.immutant/web "2.0.0-beta1"]
                 [org.immutant/transactions "2.0.0-beta1"]
                 [compojure "1.3.1"]
                 [liberator "0.12.0"]
                 [reagent "0.4.3"]
                 [cljs-ajax "0.3.9"]
                 [com.h2database/h2 "1.4.185"]]
  :plugins [[lein-cljsbuild "1.0.4"]]
  :cljsbuild
  {:builds [{:source-paths ["src-cljs"]
             :compiler {:output-to "resources/public/generated/main.js"
                        :output-dir "resources/public/generated"
                        :preamble ["reagent/react.js"]
                        :optimizations :whitespace
                        :pretty-print true}}]})
