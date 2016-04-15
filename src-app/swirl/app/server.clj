(ns swirl.app.server
  (:gen-class)
  (:require [org.httpkit.server :as http]
            [polaris.core :as polaris]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.util.response :refer [resource-response]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [swirl.app.peer :as peer]
            [environ.core :as environ]))



;; -------------------------------------------- ws -------------------

(defn random-uuid 
  [& _] 
  (str (java.util.UUID/randomUUID)))

(defonce ws-ch
  (let [{:keys [ajax-post-fn ajax-get-or-ws-handshake-fn ch-recv]}
        (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn random-uuid})]
    (defonce ring-ajax-post ajax-post-fn)
    (defonce ring-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
    ch-recv))

(defonce peer
  (peer/component ws-ch))

;; -------------------------------------------------- http -----------

(def http-routes
  [["/" :app (fn [req] (resource-response "public/app.html"))]
   ["/chsk" :socket {:GET ring-get-or-ws-handshake
                     :POST ring-ajax-post}]])

(def http-router
  (-> http-routes
      polaris/build-routes
      polaris/router
      (wrap-resource "public")
      wrap-file-info
      wrap-keyword-params
      wrap-params))

(defonce http-server (atom nil))

(defn stop-http!
  []
  (when-not (nil? @http-server)
    (@http-server :timeout 100)
    (println "server stopped")))

(defn start-http!
  ([] (start-http! 5000))
  ([port]
   (stop-http!)
   (let [server* (http/run-server #'http-router {:port port})]
     (println "server started on port" port)
     (reset! http-server server*))))



;; -------------------------------------------------------------------

(defn start! [server-port]
  ((:start-peer! peer))
  (start-http! server-port))

(defn -main  
  []
  (println "starting" (environ/env :build) "server")
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (start! port)))
