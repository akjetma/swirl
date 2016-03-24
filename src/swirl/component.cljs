(ns swirl.component)

(defn textarea
  [text*]
  [:textarea
   {:value @text*
    :on-change 
    (fn [e]
      (reset! text* (.. e -target -value)))}])
