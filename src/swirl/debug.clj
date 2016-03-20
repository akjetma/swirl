(ns swirl.debug
  (:require [clojure.pprint :refer [pprint]])
  (:refer-clojure :exclude [print]))

(defonce default-key :last)
(defonce state (atom {}))
(defonce status (atom {:store {} :print {}}))

(defn perform?
  [action k]
  (boolean (get-in @status [action k])))

(def store? (partial perform? :store))
(def print? (partial perform? :print))

(defn toggle
  [t k]
  (swap! status update-in [t k] not))

(defn store
  [k v]
  (swap! state assoc k v))

(defn print
  [k v]
  (println "\n" k) 
  (pprint v))

(defn debug
  ([v] (debug default-key v))
  ([k v] 
   (when (store? k) (store k))
   (when (print? k) (print k))
   v))

(defn wrap-debug 
  ([f] (wrap-debug f default-key))
  ([f k]
   (comp (partial debug k) f)))
