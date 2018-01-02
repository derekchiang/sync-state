(ns derekchiang.sync.core
  (:require [derekchiang.sync.state :as st]
            [derekchiang.sync.set-state :as sst]
            [clojure.set :as set]))

(defn start!
  "Sync states using the given `StateManager`.  Return a reference that can be
  passed to `stop!` to stop syncing and release resources.

  `opts` is a map that may contain the following options:

  * `:period`: the period (in milliseconds) between successive syncs.  Defaults
  to 1s.

  * `:ex-handler`: an exception handler that will be run with any exception that
  occurred during the sync loop.  By default, the exception handler prints the
  exception to stdout.

  * `:ex-continue?`: a boolean value that specifies whether the sync loop should
  continue, should an exception occur.  Defaults to false.
  "
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
     (let [*shutdown (promise)
           {:keys [period ex-handler ex-continue?]
            :or {period 1000
                 ex-handler #(println %)
                 ex-continue? false}} opts]
       (->
        (fn thread []
          (try
            (while (not (realized? *shutdown))
              (let [ideal (st/ideal-state sm)
                    actual (st/actual-state sm)]
                (st/converge! sm actual ideal)
                (Thread/sleep period)))
            (catch Exception e
              (ex-handler e)
              (when ex-continue?
                (thread)))))
        Thread. .start)
       *shutdown))))

(defn stop!
  "Shutdown"
  [s]
  (deliver s nil))
