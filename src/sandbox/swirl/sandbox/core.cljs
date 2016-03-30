(ns swirl.sandbox.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [replumb.core :as replumb]
            [devtools.core :as devtools]
            [swirl.common.post-message :as pm])
  (:import goog.net.XhrIo))

(def creator
  (or js/window.opener
      js/window.parent))

(defn fetch-file!
  [file-url src-cb]
  (try
    (.send XhrIo file-url
           (fn [e]
             (if (.isSuccess (.-target e))
               (src-cb (.. e -target getResponseText))
               (src-cb nil))))
    (catch :default e
      (src-cb nil))))

(defn handle-result!
  [result-ch result]
  (a/put! result-ch 
          (replumb/result->string true ;; include stack trace
                                  true ;; include warnings
                                  result)))

(defn eval-async
  [repl-opts text]
  (let [result-ch (a/chan)
        result-fn (partial handle-result! result-ch)]
    (replumb/read-eval-call repl-opts result-fn text)
    result-ch))

(defn start-eval-loop
  [{:keys [text-in other-window repl-opts]}]
  (let [stop-ch (a/chan)]
    (go-loop []
      (when-let [[text port] (a/alts! [text-in stop-ch])]
        (when (= port text-in)
          (let [result (a/<! (eval-async repl-opts text))]
            (pm/post! other-window :result result)
            (recur)))))
    (fn stop-eval-loop
      []
      (a/put! stop-ch :stop))))

(defn start
  [{:keys [text-in] :as context}]
  (let [stop-router (pm/start-router! {:text text-in})
        stop-eval-loop (start-eval-loop context)]
    (fn stop
      []
      (stop-router)
      (stop-eval-loop))))

(defn component
  [other-window]
  (let [repl-opts (merge (replumb/options :browser ["/js/repl_libs"] fetch-file!)
                         {:context :statement})
        context {:text-in (a/chan)
                 :repl-opts repl-opts
                 :other-window other-window}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:start-monitor! start!
     :stop-monitor! stop!}))

(defonce reload
  (do
    (devtools/enable-feature! :sanity-hints :dirac)
    (devtools/install!)    
    (enable-console-print!)
    (set! *print-fn* 
          (fn [& args] 
            (.apply js/console.debug 
                    js/console 
                    (into-array args))))
    (let [{:keys [start-monitor!]} (component creator)
          reload* (fn [] (start-monitor!))]
      (reload*)
      reload*)))
