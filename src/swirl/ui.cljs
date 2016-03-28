(ns swirl.ui
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]
            [swirl.mouse :as mouse]))



;; ------------------------------------------------ Behaviors

(defn trigger-eval
  [text-ch text]
  (a/put! text-ch text))

(defn stop-watch
  [{:keys [text* autobuild]}]
  (remove-watch text* :put-text)
  (reset! autobuild false))

(defn start-watch
  [{:keys [text* text-ch autobuild]}]
  (add-watch 
   text* :put-text
   (fn put-text [_ _ _ text]
     (trigger-eval text-ch text)))
  (reset! autobuild true))

(defn toggle-autobuild
  [{:keys [autobuild] :as context}]
  (if @autobuild
    (stop-watch context)
    (start-watch context)))

(defn resize-handler
  [{:keys [width]}]
  (let [start-width @width]
    (fn resize
      [[x-offset _]]
      (println x-offset)
      (reset! width (- start-width x-offset)))))



;; ------------------------------------------------ Reagent Components

(defn autobuild-toggle
  [{:keys [autobuild] :as context}]
  [:button#autobuild-toggle
   {:on-click #(toggle-autobuild context)}   
   [:span "autobuild:"]
   (if @autobuild
     [:span.on "on"]
     [:span.off "off"])])

(defn eval-button
  [{:keys [text-ch text*]}]
  [:button#eval-btn
   {:on-click #(trigger-eval text-ch @text*)}
   "eval"])

(defn textarea
  [{:keys [textarea-id text*]}]
  [:textarea
   {:id textarea-id
    :value @text*
    :autocomplete false}])

(defn log
  [{:keys [history*]}]
  [:div#log @history*])

(defn controls
  [context]
  [:div.control-group
   [autobuild-toggle context]
   [eval-button context]])

(defn app
  [{:keys [width] :as context}]
  [:div#app-container 
   {:style {:width @width}
    :on-mouse-down (fn [e] 
                     (mouse/drag-start
                      (resize-handler context)
                      e))}
   [:div#app
    {:on-mouse-down (fn [e] (.stopPropagation e))}
    [:div#editor
     [textarea context]]
    [:div#controls
     [log context]
     [controls context]]]])



;; ------------------------------------------------ Lifecycle

(defn start-render
  [{:keys [mount-pt] :as context}]
  (reagent/render-component
   [app context]
   mount-pt)
  (fn stop-render
    []
    (reagent/render-component
     [:div "stopped"]
     mount-pt)))

(defn start
  [context]
  (let [stop-render (start-render context)]
    (fn stop
      []
      (stop-render)
      (stop-watch context))))

(defn component
  [text* history* mount-pt]
  (let [text-ch (a/chan)
        textarea-id "editor-mount"
        context {:text* text*
                 :history* history*
                 :text-ch text-ch
                 :textarea-id textarea-id
                 :autobuild (reagent/atom false)
                 :width (reagent/atom 500)
                 :mount-pt mount-pt}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:text-ch text-ch
     :textarea-id textarea-id
     :start-ui! start!
     :stop-ui! stop!}))
