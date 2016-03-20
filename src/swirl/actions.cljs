(ns swirl.actions)

(defn set-text
  [state]
  (fn [text]
    (swap! state assoc :text text)))

(defn setup
  [state]
  {:set-text (set-text state)})
