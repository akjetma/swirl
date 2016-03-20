(ns swirl.peer
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require [dmp-clj.core :as dmp]
            #?(:cljs [reagent.core :as reagent])
            [#?@(:clj [clojure.core.async :refer [go go-loop]]
                 :cljs [cljs.core.async]) 
             :as a]))

(def interval-ms
  ;; Duration to wait to begin processing a patch after receiving it
  #?(:clj 0
     :cljs 500))

(defonce shadow*
  ;; A shadow represents a peer's knowledge of its connected peer's
  ;; last state. Server side, this is a map from UID to shadow. 
  ;; Client side, it is just the shadow string.
  (atom 
   #?(:clj {}
      :cljs "")))

(defonce text
  ;; The text that all peers are nominally converging to. This
  ;; value is directly edited by users on the client side and 
  ;; modified by received patches on both ends.
  (#?(:clj atom
      :cljs reagent/atom) 
   ""))
 
(defn swirl-out
  ;; Last mile of peer's half of the synchronization loop. Computes
  ;; the patch from the shadow of the remote peer's text to the local
  ;; peer's text, optimistically updates the local peer's knowledge of
  ;; remote peer's state to be identical to local state, then sends
  ;; over the patch 
  [text shadow ws-send]
  (let [text-val @text
        patch (dmp/make-patch @shadow text-val)]
    (reset! shadow text-val)
    (ws-send [:swirl/revolve patch])))

(defn swirl-in
  ;; First part of synchronization loop (once, on the clojure side, we
  ;; have identified which client we are talking to).
  [text shadow patch]
  (swap! text dmp/apply-patch patch)
  (swap! shadow dmp/apply-patch patch))

(defn revolve
  ;; Container for the two parts of a peer's half of the sync loop.
  [text shadow {[_ patch] :event :keys [uid ws-send]}]
  (go 
    (a/<! (a/timeout interval-ms))
    (swirl-in text shadow patch)
    (swirl-out text shadow ws-send)))

(defn ws-send-impl
  ;; Normalize the send function between environments so we can act
  ;; as if we are a regular peer from the server. Goal was to have
  ;; server and client behave as if they were the only two people
  ;; in the world.
  [{:keys [send-fn uid]}]
  #?(:clj (partial send-fn uid)
     :cljs send-fn))

(defn shadow-impl
  ;; Memoized shadow retrieval on the server-side, passthrough on
  ;; the client.
  [{:keys [uid]}]
  #?(:clj (if-let [shadow (get @shadow* uid)]
            shadow
            (let [shadow (atom "")]
              (swap! shadow* assoc uid shadow)
              shadow))
     :cljs shadow*))

(defn vortex
  ;; vt-ch (vortex channel) receives only websocket messages with
  ;; the :swirl/revolve event id (data is just a patch string).
  ;; This loop transforms/normalizes raw websocket messages and 
  ;; initiates half of a sync loop.
  [vt-ch]
  (println "starting vortex")
  (let [stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)]
    (go-loop []
      (when-let [[v p] (a/alts! [stop-ch vt-ch])]
        (if (= p vt-ch)
          (let [shadow (shadow-impl v)
                ws-send (ws-send-impl v)
                message (assoc v :ws-send ws-send)]
            (revolve text shadow message)
            (recur))
          (println "stopping vortex"))))
    stop-fn))

(defn socket
  ;; This is an example implementation of how you might configure
  ;; a websocket router to interact with the vortex. The user of 
  ;; this library is expected to put appropriate messages onto the
  ;; vortex channel and make the initial 'swirl-out' call from 
  ;; the client.
  [ws-ch vt-ch]
  (println "starting socket")
  (let [stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)]
    (go-loop []
      (when-let [[v p] (a/alts! [stop-ch ws-ch])]
        (if (= p ws-ch)
          (let [{[ev-id ev-data] :event} v]
            (when (= ev-id :swirl/revolve) 
              (a/put! vt-ch v))
            #?(:cljs (when (:first-open? ev-data) 
                       (swirl-out text shadow* (:send-fn v))))
            (recur))
          (println "stopping socket"))))
    stop-fn))

(defn lifecycle-fn
  ;; Example reloadable websocket router/handler thing. pass this
  ;; the sente ch-recv channel (for example) on both the client 
  ;; and server and receive a function that performs the teardown
  ;; and setup of the vortex and socket loops.
  [ws-ch]
  (let [vt-ch (a/chan)
        ws-handler (atom (constantly nil))
        vt-handler (atom (constantly nil))]
    (fn restart []
      (@ws-handler)
      (@vt-handler)
      (reset! ws-handler (socket ws-ch vt-ch))
      (reset! vt-handler (vortex vt-ch)))))
