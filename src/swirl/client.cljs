(ns swirl.client
  (:require [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.editor :as editor]
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
        {:keys [start-ui! text-ch textarea-id]} (ui/component text* history* mount)
        {:keys [start-repl! result-ch]} (repl/component text-ch sandbox)
        {:keys [start-log!]} (log/component result-ch history*)
        {:keys [start-editor!]} (editor/component text* textarea-id)
        reload* (fn []
                  (start-ui!)
                  (start-peer!)
                  (start-log!)
                  (start-repl!)
                  (start-editor!))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
