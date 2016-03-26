(ns swirl.mouse
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]))

(defn start-listener
  [event f]
  (.addEventListener js/window event f)
  (fn stop-listener
    []
    (.removeEventListener js/window event f)))

(defn mouse-coords
  [e]
  [(.-pageX e)
   (.-pageY e)])

(defn move-distance
  [start pos]
  (mapv - pos start))

(defn coord-listener
  [ev-id ev-ch]
  (start-listener 
   ev-id
   (fn [e] 
     (a/put! 
      ev-ch 
      (mouse-coords e)))))

(defn drag-start
  [xy-action e]
  (let [anchor (mouse-coords e)
        move-ch (a/chan)
        up-ch (a/chan)
        stop-move-listener (coord-listener "mousemove" move-ch)
        stop-up-listener (coord-listener "mouseup" up-ch)
        drag-stop (fn []
                    (stop-move-listener)
                    (stop-up-listener))]
    (go-loop []
      (when-let [[coords port] (a/alts! [move-ch up-ch])]
        (if (= port move-ch)
          (do
            (xy-action
             (move-distance anchor coords))
            (recur))
          (drag-stop))))))
