(ns swirl.ui
  (:require [swirl.test :as test]))

(defn editor
  [text set-text]
  [:textarea 
   {:value text
    :on-change #(set-text (.. % -target -value))}])

(defn app
  [state actions]
  (println @state)
  [:div#app
   (if (:app-ready @state)
     [editor (:text @state) (:set-text actions)]
     [test/component state])])
