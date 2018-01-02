(ns derekchiang.sync.core-test
  (:require [clojure.test :refer :all]
            [derekchiang.sync.state :as st]
            [derekchiang.sync.set-state :as sst]
            [derekchiang.sync.core :as sync]
            [clojure.set :as set]))

(deftest sync-sets-test
  (let [*ideal-set (atom #{1})
        *actual-set (atom #{})
        syncer (sync/start! (reify sst/SetStateManager
                              (ideal-state [_]
                                @*ideal-set)
                              (actual-state [_] @*actual-set)
                              (apply-pos-diff [_ x] (swap! *actual-set conj x))
                              (apply-neg-diff [_ x] (swap! *actual-set disj x)))
                            {:period 100})]
    ;; Sets should be synced.
    (Thread/sleep 200)
    (is (= #{1} @*ideal-set @*actual-set))

    ;; Additions should be synced.
    (reset! *ideal-set #{1 2})
    (Thread/sleep 200)
    (is (= #{1 2} @*ideal-set @*actual-set))

    ;; Removals should be synced.
    (reset! *ideal-set #{})
    (Thread/sleep 200)
    (is (= #{} @*ideal-set @*actual-set))

    ;; Revert `actual-set` if it changes.
    (reset! *actual-set #{1 2 3})
    (Thread/sleep 200)
    (is (= #{} @*ideal-set @*actual-set))

    ;; Sync can be stopped.
    (sync/stop! syncer)
    (reset! *ideal-set #{1})
    (Thread/sleep 200)
    (is (not= @*ideal-set @*actual-set))))

(deftest ex-handler-test
  (let [msg "error"
        *ex-msg (atom nil)
        syncer (sync/start!
                (reify st/StateManager
                  (ideal-state [_] (throw (Exception. msg)))
                  (actual-state [_] (throw (Exception. msg)))
                  (converge! [_ _ _] (throw (Exception. msg))))
                {:period 100
                 :ex-handler #(reset! *ex-msg (.getMessage %))})]
    (Thread/sleep 200)
    (is (= @*ex-msg msg))
    (sync/stop! syncer)))

(deftest ex-continue-test
  (let [msg "error"
        *ex-count (atom 0)
        syncer (sync/start!
                (reify st/StateManager
                  (ideal-state [_] (throw (Exception. msg)))
                  (actual-state [_] (throw (Exception. msg)))
                  (converge! [_ _ _] (throw (Exception. msg))))
                {:period 100
                 :ex-handler (fn [_] (swap! *ex-count inc))
                 :ex-continue? true})]
    (Thread/sleep 1000)
    (is (> @*ex-count 5))
    (sync/stop! syncer)))
