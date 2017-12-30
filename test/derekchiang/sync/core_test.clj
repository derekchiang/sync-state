(ns derekchiang.sync.core-test
  (:require [clojure.test :refer :all]
            [derekchiang.sync.state :as st]
            [derekchiang.sync.set-state :as sst]
            [derekchiang.sync.core :as sync]
            [clojure.set :as set]))

(deftest sync-sets
  (let [*ideal-set* (atom #{1})
        *actual-set* (atom #{})
        syncer (sync/start! (reify sst/SetStateManager
                              (ideal-state [_]
                                @*ideal-set*)
                              (actual-state [_] @*actual-set*)
                              (apply-pos-diff [_ x] (swap! *actual-set* conj x))
                              (apply-neg-diff [_ x] (swap! *actual-set* disj x)))
                            {:period 100})]
    ;; Sets should be synced.
    (Thread/sleep 200)
    (is (= #{1} @*ideal-set* @*actual-set*))

    ;; Additions should be synced.
    (reset! *ideal-set* #{1 2})
    (Thread/sleep 200)
    (is (= #{1 2} @*ideal-set* @*actual-set*))

    ;; Removals should be synced.
    (reset! *ideal-set* #{})
    (Thread/sleep 200)
    (is (= #{} @*ideal-set* @*actual-set*))

    ;; Revert `actual-set` if it changes.
    (reset! *actual-set* #{1 2 3})
    (Thread/sleep 200)
    (is (= #{} @*ideal-set* @*actual-set*))

    ;; Sync can be stopped.
    (sync/stop! syncer)
    (reset! *ideal-set* #{1})
    (Thread/sleep 200)
    (is (not= @*ideal-set* @*actual-set*))))
