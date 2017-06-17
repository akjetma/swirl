(ns swirl.app.ui
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]
            [swirl.app.mouse :as mouse]))



;; ------------------------------------------------ Behaviors

(defn trigger-eval
  [text-ch text]
  (a/put! text-ch text))

(defn stop-watch
  [{:keys [text* control-state]}]
  (remove-watch text* :put-text)
  (swap! control-state assoc :autoeval false))

(defn start-watch
  [{:keys [text* text-ch control-state]}]
  (add-watch 
   text* :put-text
   (fn put-text [_ _ _ text]
     (trigger-eval text-ch text)))
  (swap! control-state assoc :autoeval true))

(defn toggle-autoeval
  [{:keys [control-state] :as context}]
  (if (:autoeval @control-state)
    (stop-watch context)
    (start-watch context)))

(defn toggle-output-size
  [control-state]
  (swap! control-state update :expand-output not))

(defn resize-handler
  [{:keys [control-state]}]
  (let [start-width (:width @control-state)]
    (fn resize
      [[x-offset _]]
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

(defn autoeval-toggle
  [{:keys [control-state] :as context}]
  [toggle-btn
   "autoeval"
   (:autoeval @control-state)
   #(toggle-autoeval context)])

(defn eval-button
  [{:keys [text-ch text*]}]
  [:button.eval-btn
   {:on-click #(trigger-eval text-ch @text*)}
   "eval!"])

(defn textarea
  [{:keys [textarea-id text*]}]
  [:textarea
   {:id textarea-id
    :value @text*
    :auto-complete false}])

(defn log
  [{:keys [history* control-state]}]
  (let [{:keys [expand-output]} @control-state]
    [:div#log
     {:on-click #(toggle-output-size control-state)
      :class (if expand-output "large" "small")}
     [:code
      [:pre
       @history*]]]))

(defn controls
  [context]
  [:div.control-group
   [autoeval-toggle context]
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
    (start-watch context)
    (fn stop
      []
      (stop-render)
      (stop-watch context))))

(defn component
  [mount-pt text*]
  (let [text-ch (a/chan)
        history* (reagent/atom nil)
        textarea-id "editor-mount"
        context {:text* text*
                 :history* history*
                 :text-ch text-ch
                 :textarea-id textarea-id
                 :control-state (reagent/atom {:autoeval true
                                               :expand-output false
                                               :width 600})
                 :mount-pt mount-pt}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:text-ch text-ch
     :history* history*
     :textarea-id textarea-id
     :start-ui! start!
     :stop-ui! stop!}))
