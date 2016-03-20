(defproject swirl "0.1.0-SNAPSHOT"
  :min-lein-version "2.5.3"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [akjetma/dmp-clj "0.1.1"]
                 [http-kit "2.1.18"]
                 [polaris "0.0.15"]
                 [ring "1.4.0"]
                 [ring-cors "0.1.7"]
                 [binaryage/devtools "0.5.2"]
                 [reagent "0.5.1"]
                 [com.taoensso/sente "1.8.1"]]
  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-1"]]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/public/js/swirl.js"
                                    "resources/public/js/out"
                                    "target"]
  :cljsbuild {:builds 
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "swirl.client/reload"}
                :compiler {:main "swirl.client"
                           :asset-path "js/out"
                           :output-to "resources/public/js/swirl.js"
                           :output-dir "resources/public/js/out"}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:main "swirl.client"
                           :output-to "resources/public/js/swirl.js" 
                           :optimizations :advanced}}]}
  :figwheel {:css-dirs ["resources/public/css"]}
  :main swirl.server)
