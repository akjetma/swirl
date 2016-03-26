(ns swirl.client
  (:require [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.peer :as peer]
            [swirl.repl :as repl]
            [swirl.log :as log]
            [swirl.ui :as ui]))

(defn sandbox-window
  []
  (.-contentWindow 
   (.getElementById js/document "sandbox")))

(defn ui-mount
  []
  (.getElementById js/document "mount"))

(defonce reload
  (let [{:keys [ch-recv]} (sente/make-channel-socket! 
                           "/chsk" {:type :auto :wrap-recv-evs? false})
        sandbox (sandbox-window)
        mount (ui-mount)
        history* (reagent/atom nil)
        {:keys [start-peer! text*]} (peer/component ch-recv)
        {:keys [start-ui! text-ch]} (ui/component text* history* mount)
        {:keys [start-repl! result-ch]} (repl/component text-ch sandbox)
        {:keys [start-log!]} (log/component result-ch history*)
        reload* (fn []
                  (start-peer!)
                  (start-repl!)
                  (start-log!)
                  (start-ui!))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
