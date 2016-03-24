(ns swirl.client
  (:require [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.peer :as peer]
            [swirl.component :as component]
            [swirl.cross :as cross]
            [swirl.log :as log]))

(defn app
  [text* history*]
  [:div#app
   [component/textarea text*]
   [log/component history*]])
 
(defonce reload
  (let [{:keys [ch-recv]} (sente/make-channel-socket! 
                           "/chsk" {:type :auto :wrap-recv-evs? false})
        {:keys [start! text* text-ch]} (peer/system ch-recv)
        {:keys [result-ch stop-comms]} (cross/start-comms text-ch)
        {:keys [history* stop-recorder]} (log/start-recorder result-ch)
        reload* (fn []
                  (start!)
                  (reagent/render-component
                   [app text* history*]
                   (.getElementById js/document "mount")))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
