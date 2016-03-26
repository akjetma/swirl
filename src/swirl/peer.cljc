(ns swirl.peer
  #?@(:cljs [(:refer-clojure :exclude [atom])
             (:require-macros [cljs.core.async.macros :refer [go go-loop]])])
  (:require [dmp-clj.core :as dmp]
            [#?@(:clj  [clojure.core.async :refer [go go-loop]] 
                 :cljs [cljs.core.async]) 
             :as a]
            #?(:cljs [reagent.core :refer [atom]])))
 
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

(defn local-state
  [{:keys [peer-ev-id text* #?(:cljs shadow*)] :as context} 
   {:keys [uid send-fn]}]
  (let [#?@(:clj [shadow* (get-server-shadow* context uid)])
        reply! (fn [patch]
                 (send-fn 
                  #?(:clj uid) 
                  [peer-ev-id patch]))]
    {:shadow* shadow*
     :text* text*
     :reply! reply!}))

(defn handle-rotate
  [context {[_ patch] :event :as message}]
  (rotate!
   (local-state context message)
   patch))

(defn handle-close
  [{:keys [shadows*]} {:keys [uid]}]
  (swap! shadows* dissoc uid))

(defn handle-open
  [context message]
  (let [{:keys [reply!]} (local-state context message)]
    (reply! "")))

(defn router-fn
  [{:keys [peer-ev-id] :as context}]
  (fn route-msg
    [{[ev-id ev-data] :event :as message}]
    (when-let [handler
               (cond
                 (= ev-id peer-ev-id) handle-rotate
           
                 #?@(:clj 
                     [(= ev-id :chsk/uidport-close) handle-close]
               
                     :cljs
                     [(:first-open? ev-data) handle-open]))]
      (handler context message))))

(defn start-router
  [{:keys [peer-ev-id freq chsk-recv] :as context}]
  (let [route-msg (router-fn context)
        stop-ch (a/chan)]
    (go-loop []
      (when-let [[message chan] (a/alts! [stop-ch chsk-recv])]
        (when (= chan chsk-recv)
          #?(:cljs 
             (when (= (first (:event message)) peer-ev-id)
               (a/<! (a/timeout freq))))
          (route-msg message)
          (recur))))
    (fn stop-router 
      [] 
      (a/put! stop-ch :stop))))

(defn start-watch
  [{:keys [text* text-ch]}]
  (add-watch 
   text* :put-text
   (fn put-text [_ _ _ text]
     (a/put! text-ch text)))
  (fn stop-watch []
    (remove-watch text* :put-text)))

(defn start
  [context]
  (let [stop-watch (start-watch context)
        stop-router (start-router context)]
    (fn stop
      []
      (stop-watch)
      (stop-router))))

(defn component
  [chsk-recv]
  (let [text* (atom "")
        text-ch (a/chan)
        context
        {:peer-ev-id :swirl/rotate
         :text-ch text-ch
         :chsk-recv chsk-recv
         :freq 200
         :text* text*
         #?@(:clj  [:shadows* (atom {})]
             :cljs [:shadow*  (atom "")])}        
        
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:text* text*
     :text-ch text-ch
     :start-peer! start!
     :stop-peer! stop!}))
