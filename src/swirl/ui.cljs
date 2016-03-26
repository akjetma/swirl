(ns swirl.ui)

(defn textarea
  [text*]
  [:textarea
   {:value @text*
    :on-change 
    (fn [e]
      (reset! text* (.. e -target -value)))}])

(defn log
  [history*]
  [:div @history*])

(defn app
  [text* history*]
  [:div#app
   [textarea text*]
   [log history*]])
