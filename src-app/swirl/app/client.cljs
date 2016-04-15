(ns swirl.app.client
  (:require-macros [swirl.common.env :refer [env]])
  (:require [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.app.editor :as editor]
            [swirl.app.peer :as peer]
            [swirl.app.repl :as repl]
            [swirl.app.log :as log]
            [swirl.app.ui :as ui]
            [swirl.app.overlay :as overlay]))

(defn get-id
  [id]
  (.getElementById js/document id))

(defn sandbox-window
  []
  (.-contentWindow 
   (get-id "sandbox")))

(defn app-mount
  []
  (get-id "app-mount"))

(defn overlay-mount
  []
  (get-id "overlay-mount"))

(defn websocket
  []
  (:ch-recv
   (sente/make-channel-socket! 
    "/chsk" {:type :auto :wrap-recv-evs? false})))

(defonce reload
  (do
    (enable-console-print!)
    (when (= (env :build) "dev")
      (devtools/install! [:custom-formatters :sanity-hints]))
    (let [{:keys [start-overlay! set-ready!]} (overlay/component (overlay-mount))
          {:keys [start-peer! text*]} (peer/component (websocket))
          {:keys [start-ui! history* text-ch textarea-id]} (ui/component (app-mount) text*)
          {:keys [start-repl! result-ch]} (repl/component text-ch (sandbox-window))
          {:keys [start-log!]} (log/component result-ch history*)
          {:keys [start-editor!]} (editor/component text* textarea-id)
          reload* (fn []
                    (set-ready! false)
                    (start-overlay!)
                    (start-ui!)
                    (start-peer!)
                    (start-log!)
                    (start-repl!)
                    (start-editor!)
                    (set-ready! true))]
      (reload*)
      reload*)))
