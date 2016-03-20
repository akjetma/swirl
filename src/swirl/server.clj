(ns swirl.server
  (:require [clojure.core.async :as a] 
            [org.httpkit.server :as http]
            [org.httpkit.client :as h]
            [polaris.core :as polaris]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.util.response :refer [resource-response]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [swirl.debug :as debug]
            [swirl.sync :as sync]))



;; -------------------------------------------- ws -------------------

(defn random-uuid 
  [& _] 
  (str (java.util.UUID/randomUUID)))

(defonce socket
  (let [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn random-uuid})]
    {:ring-ajax-post ajax-post-fn
     :ring-get-or-ws-handshake ajax-get-or-ws-handshake-fn
     :ch-chsk ch-recv
     :chsk-send! send-fn
     :connected-uids connected-uids}))

(def ws-routes 
  {:swirl/start sync/start
   :swirl/revolve sync/revolve})

(defn ws-router
  [{[ev-id _ :as event] :event :as message}]
  (when-let [route (get ws-routes ev-id)]
    (route message)))

(defonce ws-server (atom nil))

(defn stop-ws!
  [] 
  (when-let [stop-f @ws-server]
    (stop-f)
    (reset! ws-server nil)
    (println "ws stopped")))

(defn start-ws!
  []
  (stop-ws!)
  (reset! ws-server (sente/start-chsk-router! (:ch-chsk socket) #'ws-router))
  (println "ws started"))



;; -------------------------------------------------- http -----------

(def http-routes
  [["/" :app (fn [req] (resource-response "public/swirl.html"))]
   ["/chsk" :socket {:GET (:ring-get-or-ws-handshake socket)
                     :POST (:ring-ajax-post socket)}]])

(def http-router
  (-> http-routes
      polaris/build-routes
      polaris/router
      ;; (debug/wrap-debug :http-response)
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
  (start-ws!)
  (start-http! server-port))

(defn -main  
  []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (start! port)))
