(ns swirl.log
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]))

(defn component
  [history*]
  [:div @history*])

(defn start-recorder
  [result-in]
  (let [history* (reagent/atom nil)
        stop-ch (a/chan)
        stop-recorder (fn []
                        (a/put! stop-ch :stop))]
    (go-loop []
      (when-let [[msg port] (a/alts! [result-in stop-ch])]
        (when (= port result-in)
          (reset! history* (str msg))
          (recur))))
    {:history* history*
     :stop-recorder stop-recorder}))
