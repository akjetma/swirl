(ns swirl.ui
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]
            [swirl.mouse :as mouse]))



;; ------------------------------------------------ Behaviors

(defn trigger-eval
  [text-ch text]
  (a/put! text-ch text))

(defn stop-watch
  [{:keys [text* control-state]}]
  (remove-watch text* :put-text)
  (swap! control-state assoc :autobuild false))

(defn start-watch
  [{:keys [text* text-ch control-state]}]
  (add-watch 
   text* :put-text
   (fn put-text [_ _ _ text]
     (trigger-eval text-ch text)))
  (swap! control-state assoc :autobuild true))

(defn toggle-autobuild
  [{:keys [control-state] :as context}]
  (if (:autobuild @control-state)
    (stop-watch context)
    (start-watch context)))

(defn resize-handler
  [{:keys [control-state]}]
  (let [start-width (:width @control-state)]
    (fn resize
      [[x-offset _]]
      (println x-offset)
      (swap! control-state assoc :width (- start-width x-offset)))))



;; ------------------------------------------------ Reagent Components

(defn toggle-btn
  [label state toggle-fn]
  [:button.toggle
   {:on-click toggle-fn}
   [:span.label (str label ":")]
   (if state
     [:span.on "on"]
     [:span.off "off"])])

(defn autobuild-toggle
  [{:keys [control-state] :as context}]
  [toggle-btn
   "autobuild"
   (:autobuild @control-state)
   #(toggle-autobuild context)])

(defn output-toggle
  [{:keys [control-state]}]
  [toggle-btn
   "show output"
   (:show-output @control-state)
   #(swap! control-state update :show-output not)])

(defn eval-button
  [{:keys [text-ch text*]}]
  [:button
   {:on-click #(trigger-eval text-ch @text*)}
   "eval"])

(defn textarea
  [{:keys [textarea-id text*]}]
  [:textarea
   {:id textarea-id
    :value @text*
    :autocomplete false}])

(defn log
  [{:keys [history* control-state]}]
  [:div#log 
   (when (:show-output @control-state)
     @history*)])

(defn controls
  [context]
  [:div.control-group
   [output-toggle context]
   [autobuild-toggle context]
   [eval-button context]])

(defn app
  [{:keys [control-state] :as context}]
  [:div#app-container.full-height
   {:style {:width (:width @control-state)}
    :on-mouse-down (fn [e] 
                     (mouse/drag-start
                      (resize-handler context)
                      e))}
   [:div#app.full-height
    {:on-mouse-down (fn [e] (.stopPropagation e))}
    [:div#editor.full-height
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
                 :control-state (reagent/atom {:autobuild false
                                               :show-output true
                                               :width 500})
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
