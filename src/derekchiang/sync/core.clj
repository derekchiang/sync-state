(ns derekchiang.sync.core
  (:require [derekchiang.sync.state :as st]
            [derekchiang.sync.set-state :as sst]
            [clojure.set :as set]))

(defn start!
  "Sync states using the given `StateManager`.  Return a reference that can be
  passed to `stop!` to stop syncing and release resources.

  `opts` is a map that may contain the following options:

  * `:period`: the period (in milliseconds) between successive syncs.  Defaults
  to 1s."
  ([sm]
   (start! sm {}))
  ([sm opts]
   (condp satisfies? sm
     sst/SetStateManager
     (start!
      (reify st/StateManager
        (st/ideal-state [_]
          (sst/ideal-state sm))
        (st/actual-state [_] (sst/actual-state sm))
        (st/converge! [_ actual ideal]
          (let [to-add (set/difference ideal actual)
                to-remove (set/difference actual ideal)]
            (doseq [x to-add]
              (sst/apply-pos-diff sm x))
            (doseq [x to-remove]
              (sst/apply-neg-diff sm x)))))
      opts)

     st/StateManager
     (let [*shutdown* (promise)
           period (or (:period opts) 1000)]
       (future
         (while (not (realized? *shutdown*))
           (let [ideal (st/ideal-state sm)
                 actual (st/actual-state sm)]
             (st/converge! sm actual ideal)
             (Thread/sleep period))))
       *shutdown*))))

(defn stop!
  "Shutdown"
  [s]
  (deliver s nil))
