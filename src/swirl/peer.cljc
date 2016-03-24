(ns swirl.peer
  #?@(:cljs [(:refer-clojure :exclude [atom])
             (:require-macros [cljs.core.async.macros :refer [go go-loop]])
             (:require [reagent.core :refer [atom]]
                       [dmp-clj.core :as dmp]
                       [swirl.component :as component]
                       [cljs.core.async :as a])]
      :clj [(:require [dmp-clj.core :as dmp]
                      [clojure.string :refer [split]]
                      [clojure.core.async :refer [go go-loop] :as a])]))
 
(defn apply-patch!
  [{:keys [text* shadow*]} patch]
  (swap! shadow* dmp/apply-patch patch)
  (swap! text* dmp/apply-patch patch))

(defn make-patch!
  [{:keys [text* shadow*]}]
  (let [text @text*
        patch (dmp/make-patch @shadow* text)]
    (reset! shadow* text)
    patch))

(defn rotate!
  [{:keys [reply!] :as local} patch]
  (when-not (empty? patch)
    (apply-patch! local patch))
  (let [patch (make-patch! local)]
    (reply! patch)))

(defn get-server-shadow*
  [{:keys [shadows*]} uid]
  (if-let [shadow* (get @shadows* uid)]
    shadow*
    (let [shadow* (atom "")]
      (swap! shadows* assoc uid shadow*)
      shadow*)))

(defn peer-context
  [{:keys [peer-ev-id text* #?(:cljs shadow*)]
    :as app-context} 
   {:keys [uid send-fn]}]
  (let [#?@(:clj [shadow* (get-server-shadow* app-context uid)])
        reply! (fn [patch]
                 (send-fn 
                  #?(:clj uid) 
                  [peer-ev-id patch]))]
    {:shadow* shadow*
     :text* text*
     :reply! reply!}))

(defn handle-rotate
  [app-context {[_ patch] :event :as message}]
  (rotate!
   (peer-context app-context message)
   patch))

(defn handle-close
  [{:keys [shadows*]} {:keys [uid]}]
  (swap! shadows* dissoc uid))

(defn handle-open
  [app-context message]
  (let [{:keys [reply!]} (peer-context app-context message)]
    (reply! "")))

(defn swirl-router
  [{:keys [peer-ev-id] :as app-context}]
  (fn 
    [{[ev-id {:keys [first-open?] 
              :as ev-data}] :event 
      :as message}]
    (when-let [handler
               (cond
                 (= ev-id peer-ev-id) handle-rotate
           
                 #?@(:clj 
                     [(= ev-id :chsk/uidport-close) handle-close]
               
                     :cljs
                     [first-open? handle-open]))]
      (handler app-context message))))

(defn router-loop
  [{:keys [peer-ev-id freq]
    :as app-context} 
   chsk-recv]
  (let [route (swirl-router app-context)
        stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)]
    (go-loop []
      (when-let [[{[ev-id] :event :as message} chan] 
                 (a/alts! [stop-ch chsk-recv])]
        (when (= chan chsk-recv)
          #?(:cljs 
             (when (= ev-id peer-ev-id)
               (a/<! (a/timeout freq))))
          (route message)
          (recur))))
    stop-fn))

(defn system
  [chsk-recv]
  (let [text* (atom "")
        
        app-context
        {:peer-ev-id :swirl/rotate
         :freq 200
         :text* text*
         #?@(:clj  [:shadows* (atom {})]
             :cljs [:shadow*  (atom "")])}
        
        system* (atom (constantly nil))
        system-stop! (fn [] (@system*))
        system-start! (fn []
                        (system-stop!)
                        (reset! system* (router-loop app-context chsk-recv)))]
    {:text* text*
     :start! system-start!
     :stop! system-stop!}))
