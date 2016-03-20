(ns swirl.client
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [taoensso.sente :as sente]
            [reagent.core :as reagent]
            [devtools.core :as devtools]
            [swirl.actions :as actions]
            [dmp-clj.core :as dmp]
            [swirl.ui :as ui]))

(defn send-out
  [state send-fn]
  (let [{:keys [shadow text m n]} @state
        patch (dmp/make-patch shadow text)]
    (swap! state assoc :shadow text :n (inc n))
    (send-fn [:swirl/revolve {:patch patch :m m :n n}])))

(defn sync-loop
  [state loop-ch send-fn]
  (go-loop []
    (let [{patch :patch s-m :m s-n :n} (a/<! loop-ch)
          {:keys [shadow text m n]} @state
          updated-shadow (dmp/apply-patch shadow patch)
          updated-text (dmp/apply-patch text patch)]
      (swap! state #(-> %
                        (update :shadow dmp/apply-patch patch)
                        (update :text dmp/apply-patch patch)
                        (update :m inc)))
      (a/<! (a/timeout 200))
      (send-out state send-fn)
      (recur))))

(defn sync-start
  [state send-fn]
  (let [start-ch (a/chan)
        loop-ch (a/chan)]
    (go
      (when-let [{:keys [shadow text m n] :as status} (a/<! start-ch)]
        (swap! state merge (assoc status :app-ready true))
        (send-out state send-fn)
        (sync-loop state loop-ch send-fn)))
    [start-ch loop-ch]))

(defn sync
  [state]
  (let [{:keys [ch-recv send-fn]}
        (sente/make-channel-socket! "/chsk" {:type :auto :wrap-recv-evs? false})
        [start-ch loop-ch] (sync-start state send-fn)]
    (go-loop []
      (when-let [{[ev-id ev-data] :event} (a/<! ch-recv)]
        (cond
          (= :swirl/start ev-id) (a/>! start-ch ev-data)
          (= :swirl/revolve ev-id) (a/>! loop-ch ev-data)
          (:first-open? ev-data) (send-fn [:swirl/start {}])))
      (recur))))

(defonce reload
  (let [state (reagent/atom {})
        reload* (fn []
                  (let [actions (actions/setup state)]
                    (reagent/render-component
                     [ui/app state actions]
                     (.getElementById js/document "mount"))))]
    (enable-console-print!)
    (devtools/install!)
    (reload*)
    (sync state)
    reload*))
