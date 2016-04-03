(ns swirl.common.core)

(defn listen!
  [target evt-id evt-fn]
  (.addEventListener target evt-id evt-fn)
  (fn stop-listening! []
    (.removeEventListener target evt-id evt-fn)))
