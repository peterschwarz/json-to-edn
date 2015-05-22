(defproject json-to-edn "0.1.0-SNAPSHOT"
  :description "A Simple utility page for converting JSON to EDN, and back again."
  :url "http://peterschwarz.github.io/json-to-edn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [endophile "0.1.2"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.3"]]

  :profiles {:dev {:dependencies [[figwheel "0.3.3"]]}}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/public/js/test"
                                    "target"]
  
  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]

              :figwheel { :on-jsload "json-to-edn.core/on-js-reload" }

              :compiler {:main json-to-edn.core
                         :asset-path "js/compiled/out"
                         :output-to "resources/public/js/compiled/json_to_edn.js"
                         :output-dir "resources/public/js/compiled/out"
                         :source-map-timestamp true }}

             {:id "test"
              :source-paths ["src" "test"]

              :compiler {:main json-to-edn.test-runner
                         :asset-path "js/test/out"
                         :output-to "resources/public/js/test/test.js"
                         :output-dir "resources/public/js/test/out"
                         :source-map-timestamp true }}
             {:id "min"
              :source-paths ["src"]
              :compiler {:output-to "resources/public/js/compiled/json_to_edn.js"
                         :main json-to-edn.core                         
                         :optimizations :advanced
                         :pretty-print false}}]}

  :figwheel {
    :css-dirs ["resources/public/css"] })
