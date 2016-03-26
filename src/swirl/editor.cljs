(ns swirl.editor
  (:require [reagent.core :as reagent]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure))

(defn text->editor
  [{:keys [cm]} text]
  (.setValue cm text))

(defn editor->text*
  [{:keys [text* cm]}]
  (let [text (.getValue cm)]
    (reset! text* text)))

(defn start-text*-watch
  [{:keys [text* cm] :as context}]
  (add-watch 
   text* :editor-sync
   (fn [_ _ _ new-text]
     (let [editor (.getValue cm)]
       (when-not (= editor new-text)
         (text->editor context new-text)))))
  (fn stop-text*-watch
    []
    (remove-watch text* :editor-sync)))

(defn start-editor-watch
  [{:keys [cm] :as context}]
  (let [handler (fn [] (editor->text* context))]
    (.on cm "change" handler)
    (fn stop-editor-watch
      []
      (.off cm "change" handler))))

(defn textarea
  [{:keys [textarea-id]}]
  (.getElementById js/document textarea-id))

(defn attach
  [context]
  (.fromTextArea 
   js/CodeMirror (textarea context)
   #js {:mode "clojure"
        :theme "material"
        :lineNumbers true
        :lineWrapping true}))
 
(defn start
  [context]
  (let [cm (attach context)
        context (assoc context :cm cm)
        stop-text*-watch (start-text*-watch context)
        stop-editor-watch (start-editor-watch context)]
    (fn stop
      []
      (stop-text*-watch)
      (stop-editor-watch))))

(defn component
  [text* textarea-id]
  (let [context {:textarea-id textarea-id
                 :text* text*}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:start-editor! start!
     :stop-editor! stop!}))
