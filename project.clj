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
                 [binaryage/devtools "0.5.2"]
                 [reagent "0.5.1"]
                 [com.taoensso/sente "1.8.1"]
                 [akjetma/dmp-clj "0.1.2"]]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-1"]]
  :clean-targets ^{:protect false} ["resources/public/js/swirl.js"
                                    "resources/public/js/out"
                                    "resources/public/js/sandbox.js"
                                    "resources/public/js/out_sb"
                                    "target"]
  :source-paths ["src"]
  :main swirl.server
  :profiles {:dev {:figwheel {:css-dirs ["resources/public/css"]}
                   :cljsbuild {:builds 
                               {:client {:figwheel {:on-jsload "swirl.client/reload"}
                                         :compiler {:main "swirl.client"
                                                    :asset-path "js/out"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none}}
                                :sandbox {:figwheel {:on-jsload "sandbox.core/reload"}
                                          :compiler {:main "sandbox.core"
                                                     :asset-path "js/out_sb"
                                                     :output-dir "resources/public/js/out_sb"
                                                     :optimizations :none}}}}}
             :prod {:cljsbuild {:builds 
                                {:client {:compiler {:optimizations :advanced}}
                                 :sandbox {:compiler {:optimizations :simple}}}}}}
  :cljsbuild {:builds 
              {:client {:source-paths ["src"]
                        :compiler 
                        {:output-to "resources/public/js/swirl.js"}}
               :sandbox {:source-paths ["src-sandbox"]
                         :compiler
                         {:output-to "resources/public/js/sandbox.js"}}}})
