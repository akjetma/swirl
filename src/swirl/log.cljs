(ns swirl.log
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]))

(defn start
  [{:keys [result-in history*]}]
  (let [stop-ch (a/chan)
        stop-fn (fn []
                  (a/put! stop-ch :stop))]
    (go-loop []
      (when-let [[msg port] (a/alts! [result-in stop-ch])]
        (when (= port result-in)
          (reset! history* (str msg))
          (recur))))
    stop-fn))

(defn component
  [result-in history*]
  (let [context {:history* history*
                 :result-in result-in}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn*
                         (start context)))]
    
    {:stop-log! stop!
     :start-log! start!}))
