(ns swirl.repl
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]))

(defn post-message
  [{:keys [other-window]} message]
  (.postMessage other-window message "*"))

(defn start-listener
  [{:keys [result-in]}]
  (let [receive-msg (fn [message]
                      (a/put! result-in (.-data message)))
        listener (.addEventListener 
                  js/window "message"
                  receive-msg)]
    (fn stop-listener 
      []
      (.removeEventListener
       js/window "message"
       receive-msg))))

(defn start-comms
  [{:keys [text-in result-in result-out] :as context}]
  (let [stop-ch (a/chan)]
    (go-loop []
      (when-let [[msg port] (a/alts! [text-in stop-ch])]
        (when (= port text-in)
          (post-message context msg)
          (let [result (a/<! result-in)]
            (println result)
            (a/put! result-out result)
            (recur)))))
    (fn stop-comms
      []
      (a/put! stop-ch :stop))))

(defn start
  [context]
  (let [stop-listener (start-listener context)
        stop-comms (start-comms context)]
    (fn stop
      []
      (stop-listener)
      (stop-comms))))

(defn component
  [text-in other-window]
  (let [result-out (a/chan)
        context {:text-in text-in                 
                 :other-window other-window
                 :result-out result-out
                 :result-in (a/chan)}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]    
    {:start-repl! start!
     :stop-repl! stop!
     :result-ch result-out}))
