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

(defonce reload
  (let [{:keys [ch-recv]} (sente/make-channel-socket! 
                           "/chsk" {:type :auto :wrap-recv-evs? false})
        sandbox (sandbox-window)
        {:keys [start-peer! text* text-ch]} (peer/component ch-recv)
        {:keys [start-repl! result-ch]} (repl/component text-ch sandbox)
        {:keys [start-log! history*]} (log/component result-ch)
        reload* (fn []
                  (start-peer!)
                  (start-repl!)
                  (start-log!)
                  (reagent/render-component
                   [ui/app text* history*]
                   (.getElementById js/document "mount")))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
