(ns swirl.sync
  (:require [clojure.core.async :as a]
            [dmp-clj.core :as dmp]))

(defonce clients (atom {}))
(comment
  {"A" {:shadow "cat" :backup "cat" :m 0 :n 0}
   "B" {:shadow "cats" :backup "cats" :m 1 :n 1}})

(defonce server-text (atom ""))
(comment "cats")

(defn start
  [{:keys [uid send-fn]}]
  (println uid)
  (let [text @server-text
        client {:shadow text :text text :m 0 :n 0}
        server {:shadow text :backup text :m 0 :n 0}]
    (swap! clients assoc uid server)
    (send-fn uid [:swirl/start client])))

(defn patch-shadow!
  [uid patch]
  (swap! 
   clients update uid
   (fn [{:keys [shadow m n]}]
     (let [updated (dmp/apply-patch shadow patch)]
       {:shadow updated 
        :backup updated 
        :n (inc n)
        :m m}))))

(defn patch-text!
  [patch]
  (swap! server-text dmp/apply-patch patch))

(defn patch?
  [uid e-m e-n]
  (let [{:keys [m n]} (get @clients uid)]
    (and (= e-m m)
         (= e-n n))))

(defn revolve
  [{[_ {patch :patch e-m :m e-n :n}] :event :keys [uid send-fn]}]
  (patch-shadow! uid patch)
  (patch-text! patch)
  (let [return-text @server-text
        {:keys [m n]} (get @clients uid)
        return-patch (dmp/make-patch (get-in @clients [uid :shadow]) return-text)]
    (swap! clients update uid
           #(-> %
                (assoc :shadow return-text)
                (update :m inc)))
    (println @server-text)
    (send-fn uid [:swirl/revolve {:patch return-patch :m m :n n}])))
