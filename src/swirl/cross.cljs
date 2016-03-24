(ns swirl.cross
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]))

(defn post-message
  [other-window message]
  (.postMessage other-window message "*"))

(defn bind-listener
  [channel]
  (let [put-message (fn [message]
                      (a/put! channel (.-data message)))
        listener (.addEventListener 
                  js/window "message"
                  put-message)]
    (fn detach-listener []
      (.removeEventListener
       js/window "message"
       put-message))))

(defn start-comms
  ([text-in] (start-comms text-in
                          (.-contentWindow 
                           (.getElementById js/document "sandbox"))))
  ([text-in other-window]
   (let [result-in (a/chan)
         result-out (a/chan)
         stop-ch (a/chan)
         detach-listener (bind-listener result-in)
         stop-comms (fn [] 
                      (detach-listener)
                      (a/put! stop-ch :stop))]
     (go-loop []
       (when-let [[msg port] (a/alts! [text-in stop-ch])]
         (when (= port text-in)
           (post-message other-window msg)
           (let [result (a/<! result-in)]
             (println result)
             (a/put! result-out result)
             (recur)))))
     {:stop-comms stop-comms
      :result-ch result-out})))
