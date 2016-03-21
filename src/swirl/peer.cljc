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
     :cljs 100))

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
    (when-not (empty? patch)
      (swirl-in text shadow patch))
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

(defn vortex-fn
  [message]
  (let [shadow (shadow-impl message)
        ws-send (ws-send-impl message)
        message (assoc message :ws-send ws-send)]
    (revolve text shadow message)))

(defn router-fn
  [vt-ch {[ev-id ev-data] :event :as message}]
  (cond
    (= ev-id :swirl/revolve) (a/put! vt-ch message)
    #?@(:clj  [(= ev-id :chsk/uidport-close) (swap! shadow* dissoc (:uid message))]
        :cljs [(:first-open? ev-data) (swirl-out text shadow* (:send-fn message))])))

(defn stoppable
  ;; Starts a go-loop whose body is a function that consumes
  ;; the value from one input channel. Returns a function that
  ;; exits the loop when called.
  [f in-ch process-name]
  (println "starting " process-name)
  (let [stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)]
    (go-loop []
      (when-let [[msg chan] (a/alts! [stop-ch in-ch])]
        (if (= chan stop-ch)
          (println "stopping " process-name)
          (do (f msg)
              (recur)))))
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
      (reset! ws-handler 
              (stoppable (partial router-fn vt-ch) ws-ch "ws router"))
      (reset! vt-handler 
              (stoppable vortex-fn vt-ch "peer vortex")))))
