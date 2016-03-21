(ns example.client
  (:require [taoensso.sente :as sente]
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

(defonce reload
  (let [{ws-ch :ch-recv} (sente/make-channel-socket! 
                          "/chsk" {:type :auto :wrap-recv-evs? false})
        cycle-fn (peer/lifecycle-fn ws-ch)
        reload* (fn []
                  (cycle-fn)
                  (reagent/render-component
                   [app peer/text]
                   (.getElementById js/document "mount")))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    reload*))
