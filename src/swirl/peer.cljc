(ns swirl.peer
  #?@(:cljs [(:refer-clojure :exclude [atom])
             (:require-macros [cljs.core.async.macros :refer [go go-loop]])
             (:require [reagent.core :refer [atom]]
                       [dmp-clj.core :as dmp]
                       [cljs.core.async :as a])]
      :clj [(:require [dmp-clj.core :as dmp]
                      [clojure.core.async :refer [go go-loop] :as a])]))
 
(defn apply-patch!
  [{:keys [text* shadow*]} patch]
  (swap! shadow* dmp/apply-patch patch)
  (swap! text* dmp/apply-patch patch))

(defn make-patch!
  [{:keys [shadow* text*]}]
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
  [{:keys [shadows*] :as app-context} uid]
  (if-let [shadow* (get @shadows* uid)]
    shadow*
    (let [shadow* (atom "")]
      (swap! shadows* assoc uid shadow*)
      shadow*)))

(defn peer-context
  [app-context {:keys [uid send-fn]}]
  (let [shadow* #?(:clj (get-server-shadow* app-context uid)
                   :cljs (:shadow* app-context))
        reply! (fn [patch]
                 (send-fn #?(:clj uid)
                          [(:swirl-event app-context)
                           patch]))]
    {:shadow* shadow*
     :text* (:text* app-context)
     :reply! reply!}))

(defn server-handle-close
  [app-context message]
  (swap! (:shadows* app-context) dissoc (:uid message)))

(defn client-handle-open
  [app-context message]
  ((:reply! 
    (peer-context app-context message))
   ""))

(defn ev-id
  [message]
  (first (:event message)))

(defn ev-data
  [message]
  (last (:event message)))

(defn peer-handle-rotate
  [app-context message]
  (rotate!
   (peer-context app-context message)
   (ev-data message)))

(defn swirl-router
  [app-context]
  (fn [message]
    (when-let [handler
          (cond
            (= (ev-id message) (:swirl-event app-context))
            peer-handle-rotate
            
            #?@(:clj 
                [(= (ev-id message) :chsk/uidport-close)
                 server-handle-close]
                
                :cljs
                [(:first-open? (ev-data message))
                 client-handle-open]))]
      (handler app-context message))))

(defn router-loop
  [app-context recv-ch]
  (let [route (swirl-router app-context)
        stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)]
    (go-loop []
      (when-let [[msg chan] (a/alts! [stop-ch recv-ch])]
        (when (= chan recv-ch)
          (when (= (ev-id msg)
                   (:swirl-event app-context))
            (a/<! (a/timeout @(:swirl-delay app-context))))
          (route msg)
          (recur))))
    stop-fn))

(defn system
  [chsk-recv]
  (let [app-context {:text* (atom "")
                     :swirl-event :swirl/rotate
                     :swirl-delay (atom #?(:clj 0 :cljs 200))
                     #?@(:clj [:shadows* (atom {})]
                         :cljs [:shadow* (atom "")])}
        system* (atom (constantly nil))
        system-stop! (fn [] (@system*))
        system-start! (fn []
                        (system-stop!)
                        (reset! system* (router-loop app-context chsk-recv)))]
    {:text (:text* app-context)
     :context app-context
     :start! system-start!
     :stop! system-stop!}))
