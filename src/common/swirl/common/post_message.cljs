(ns swirl.common.post-message
  (:require [cljs.core.async :as a]))

(defn post!
  [other-window id data]
  (let [message {:id id :data data}
        js-message (clj->js message)]
    (.postMessage 
     other-window 
     js-message
     "*")))

(defn missing-route
  [route raw-message]
  (println (str "couldn't find matching route for " route ": ") raw-message))

(defn router
  [routes]
  (fn [message]
    (let [clj-message (js->clj (.-data message) :keywordize-keys true)
          {:keys [id data]} clj-message
          route (keyword id)
          route-ch (get routes route)]
      (if route-ch
        (a/put! route-ch data)
        (missing-route route message)))))

(defn listen!
  [evt-id evt-fn]
  (.addEventListener js/window evt-id evt-fn)
  (fn stop-listening! []
    (.removeEventListener js/window evt-id evt-fn)))

(defn start-router!
  [routes]
  (let [route-fn (router routes)
        stop-router (listen! "message" route-fn)]
    stop-router))
