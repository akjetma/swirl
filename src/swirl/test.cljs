(ns swirl.test
  (:require [dmp-clj.core :as dmp]))

(defn example-data
  []
  (let [shadow "cat"
        text "cats"
        patch (dmp/make-patch shadow text)
        updated (dmp/apply-patch shadow patch)]
    {:shadow shadow
     :text text
     :patch patch
     :updated updated}))

(defn component
  [state]
  (let [{:keys [shadow text patch updated]} (example-data)]
    [:ul
     [:li "shadow | " shadow]
     [:li "text | " text]
     [:li "patch | " patch]
     [:li "updated | " updated]]))
