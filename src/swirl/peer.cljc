(ns swirl.peer
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require [dmp-clj.core :as dmp]
            #?(:cljs [reagent.core :as r])
            [#?@(:clj [clojure.core.async :refer [go go-loop]]
                 :cljs [cljs.core.async]) 
             :as a]))

(def interval-ms
  #?(:clj 0
     :cljs 500))

(defonce shadow
  (atom 
   #?(:clj  {}
      :cljs "")))

(defonce text
  (#?(:clj atom
      :cljs r/atom) 
   ""))
 
(defn swirl-out
  [text shadow ws-send]
  (let [text-val @text
        patch (dmp/make-patch @shadow text-val)]
    (reset! shadow text-val)
    (ws-send [:swirl/revolve patch])))

(defn swirl-in
  [text shadow patch]
  (swap! text dmp/apply-patch patch)
  (swap! shadow dmp/apply-patch patch))

(defn revolve
  [text shadow {[_ patch] :event :keys [uid ws-send]}]
  (go 
    (let [shadow #?(:clj (get @shadow uid)
                    :cljs shadow)]
      (a/<! (a/timeout interval-ms))
      (swirl-in text shadow patch)
      (swirl-out text shadow ws-send))))

(defn spin-up
  [text shadow {[_ ev-data] :event :keys [uid ws-send]}]
  #?@(:clj [(let [client-shadow (atom @text)] 
              (swap! shadow assoc uid client-shadow)
              (ws-send [:swirl/start @client-shadow]))]
      :cljs [(reset! text ev-data)
             (reset! shadow ev-data)
             (swirl-out text shadow ws-send)]))

(defn ws-send-impl
  [{:keys [send-fn #?(:clj uid)]}]
  (fn [event]
    (send-fn #?(:clj uid) event)))

(defn vortex
  [ch-recv]
  (let [stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)
        swirl-start (partial spin-up text shadow)
        swirl-revolve (partial revolve text shadow)]
    (println "vortex starting up")
    (go-loop []
      (when-let [[v p] (a/alts! [stop-ch ch-recv])]
        (if (= p stop-ch)
          (println "vortex shutting down")
          (let [{[ev-id ev-data] :event :as message} v
                ws-send (ws-send-impl message)
                message (assoc message :ws-send ws-send)]
            (case ev-id
              :swirl/start (swirl-start message)
              :swirl/revolve (swirl-revolve message)
              #?(:clj nil
                 :cljs (when (:first-open? ev-data) (ws-send [:swirl/start]))))
            (recur)))))
    stop-fn))
