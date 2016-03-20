(ns swirl.client
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.peer :as peer]))

(defn editor
  [text]
  [:textarea 
   {:value @text
    :on-change #(reset! text (.. % -target -value))}])

(defn app
  [text]
  [:div#app
   [editor text]])

(defn websocket
  []
  (let [{:keys [ch-recv]}
        (sente/make-channel-socket! 
         "/chsk" {:type :auto :wrap-recv-evs? false})]
    ch-recv))

(defonce reload
  (let [ws-channel (websocket)
        ws-handler (atom nil)
        reload* (fn []
                  (when-let [stop-ws @ws-handler] 
                    (stop-ws))
                  (reset! ws-handler (peer/vortex ws-channel))
                  (reagent/render-component
                   [app peer/text]
                   (.getElementById js/document "mount")))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
