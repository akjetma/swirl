(ns sandbox.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [cljs.js :as cljs]))

(def creator
  (or js/window.opener
      js/window.parent))

(defn eval-str
  [cb input]
  (cljs/eval-str
   (cljs/empty-state)
   input
   'cljs.user
   {:eval cljs/js-eval}
   cb))

(defn eval-async
  [input]
  (let [result-ch (a/chan)]
    (eval-str
     (fn [{:keys [value error]}]
       (a/put! result-ch (or value "fuck")))
     input)
    result-ch))

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

(defn start-monitor
  ([] (start-monitor creator))
  ([other-window]
   (let [in-ch (a/chan)
         stop-ch (a/chan)
         detach-listener (bind-listener in-ch)
         stop-monitor (fn [] 
                        (detach-listener)
                        (a/put! stop-ch :stop))]
     (go-loop []
       (when-let [[msg port] (a/alts! [in-ch stop-ch])]
         (when (= port in-ch)
           (let [result (a/<! (eval-async msg))]
             ;; (println result)
             (post-message other-window result)
             (recur)))))
     stop-monitor)))

(defonce reload
  (let [monitor (atom (constantly nil))
        reload* (fn []
                  (@monitor)
                  (reset! monitor (start-monitor)))]
    (enable-console-print!)
    
    (reload*)
    reload*))
