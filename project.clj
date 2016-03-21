(defproject swirl "0.1.0-SNAPSHOT"
  :min-lein-version "2.5.3"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [akjetma/dmp-clj "0.1.1"]
                 [binaryage/devtools "0.5.2"]
                 [reagent "0.5.1"]
                 [com.taoensso/sente "1.8.1"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-1"]]

  :profiles {
             #_#_
             :dev {:source-paths ["src" "test"]
                   :cljsbuild {:builds {:client
                                        {:source-paths ["src" "test"]
                                         :figwheel {:on-jsload "swirl.test/reload"}
                                         :compiler {:main "swirl.test"
                                                    :optimizations :none}}}}}

             :prod {:source-paths ["src"]
                    :cljsbuild {:builds {:client
                                         {:source-paths ["src"]
                                          :compiler {:optimizations :advanced
                                                     :pretty-print false}}}}}}

  :cljsbuild {:builds {:client 
                       {:compiler {:output-dir "target/client"
                                   :output-to "target/client.js"}}}})
