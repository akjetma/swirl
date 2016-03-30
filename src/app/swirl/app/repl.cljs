(ns swirl.app.repl
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [swirl.common.post-message :as pm]))

;; (when (= id "result")
;;   (a/put! result-in data))

(defn start-comms
  [{:keys [text-in result-in result-out other-window] :as context}]
  (let [stop-ch (a/chan)]
    (go-loop []
      (when-let [[msg port] (a/alts! [text-in stop-ch])]
        (when (= port text-in)
          (pm/post! other-window :text msg)
          (let [result (a/<! result-in)]
            (println result)
            (a/put! result-out result)
            (recur)))))
    (fn stop-comms
      []
      (a/put! stop-ch :stop))))

(defn start
  [{:keys [result-in] :as context}]
  (let [routes {:result result-in}
        stop-router (pm/start-router! routes)
        stop-comms (start-comms context)]
    (fn stop
      []
      (stop-router)
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
