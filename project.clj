(defproject swirl "0.1.0-SNAPSHOT"  
  :min-lein-version "2.5.3"  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374"]
                 [cljsjs/codemirror "5.11.0-1"]
                 [http-kit "2.1.18"]
                 [polaris "0.0.15"]
                 [ring "1.4.0"]
                 [ring-cors "0.1.7"]
                 [binaryage/devtools "0.6.0"]
                 [reagent "0.5.1"]
                 [com.taoensso/sente "1.8.1"]
                 [akjetma/dmp-clj "0.1.3"]
                 [replumb "0.2.1"]]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :clean-targets ^{:protect false} ["resources/public/js/app.js"
                                    "resources/public/js/out"
                                    "resources/public/js/sandbox.js"
                                    "resources/public/js/out_sb"
                                    "resources/public/js/_.js"
                                    "resources/public/js/repl_libs"
                                    "target"]
  :source-paths ["src-app"]
  :main swirl.app.server
  :uberjar-name "swirl.jar"
  :cljsbuild {:builds 
              {:client {:source-paths ["src-app" "src-common"]
                        :compiler {:optimizations :advanced
                                   :output-to "resources/public/js/app.js"}}
               :sandbox {:source-paths ["src-sandbox" "src-common"]
                         :compiler {:optimizations :simple
                                    :output-to "resources/public/js/sandbox.js"}}
               :repl-libs {:source-paths ["src-sandbox" "src-common" "src-app"]
                           :compiler {:optimizations :none
                                      :output-to "resources/public/js/_.js"
                                      :asset-path "js/repl_libs"
                                      :output-dir "resources/public/js/repl_libs"}}}}
  
  :profiles {:dev {:plugins [[lein-figwheel "0.5.0-1"]]
                   :figwheel {:css-dirs ["resources/public/css"]}
                   :cljsbuild {:builds {:client {:figwheel {:on-jsload "swirl.app.client/reload"}
                                                 :compiler {:optimizations :none
                                                            :main "swirl.app.client"
                                                            :asset-path "js/out"
                                                            :output-dir "resources/public/js/out"}}
                                        :sandbox {:figwheel {:on-jsload "swirl.sandbox.core/reload"}
                                                  :compiler {:optimizations :none
                                                             :main "swirl.sandbox.core"
                                                             :asset-path "js/out_sb"
                                                             :output-dir "resources/public/js/out_sb"}}}}}
             :uberjar {:hooks [leiningen.cljsbuild]
                       :aot :all}})
