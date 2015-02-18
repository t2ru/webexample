(defproject webexample "0.1.0-SNAPSHOT"
  :description "example of web service"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2760"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.clojure/data.json "0.2.5"]
                 [org.immutant/web "2.0.0-beta2"]
                 [org.immutant/transactions "2.0.0-beta2"]
                 [compojure "1.3.1"]
                 [liberator "0.12.0"]
                 [reagent "0.4.3"]
                 [cljs-ajax "0.3.9"]
                 [yesql "0.4.0"]]
  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-immutant "2.0.0-beta1"]]
  :main webexample.core
  :aot :all
  :cljsbuild
  {:builds [{:source-paths ["src-cljs"]
             :compiler {:output-to "resources/public/generated/main.js"
                        :output-dir "resources/public/generated"
                        :preamble ["reagent/react.js"]
                        :optimizations :whitespace
                        :pretty-print true
                        :jar true}}]}
  :hooks [leiningen.cljsbuild]
  :profiles
  {:provided {:dependencies [[com.h2database/h2 "1.4.185"]]}})
