(ns swirl.app.overlay
  (:require [reagent.core :as reagent]
            [swirl.common.core :as common]))

(defn link-state
  [app-status*]
  (swap! 
   app-status* assoc :ready-state
   (= "complete" js/document.readyState)))

(defn start-rs-listener
  [{:keys [app-status*]}]
  (common/listen! js/document "readystatechange"
                  #(link-state app-status*)))

(defn overlay?
  [app-status]
  (not-every? true? (vals app-status)))

(defn overlay
  [app-status*]
  (when (overlay? @app-status*)
    [:div#overlay
     "Loading..."]))

(defn start-overlay
  [{:keys [app-status* mount]}]
  (reagent/render-component
   [overlay app-status*]
   mount)
  (fn stop-overlay
    []
    (reagent/render-component
     [:div "hi"]
     mount)))

(defn start
  [context]
  (let [stop-rs-listener (start-rs-listener context)
        stop-overlay (start-overlay context)]
    (fn stop
      []
      (stop-rs-listener)
      (stop-overlay))))

(defn component
  [mount]
  (let [app-status* (reagent/atom
                     {:ready-state false
                      :system-state false})       
        context {:app-status* app-status*
                 :mount mount}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:start-overlay! start!
     :stop-overlay! stop!
     :set-ready! (fn [status] 
                   (swap! app-status* 
                          assoc :system-state status))}))
