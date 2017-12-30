(ns derekchiang.sync.state)

(defprotocol StateManager
  "StateManager implements the functions required for syncing states."
  (ideal-state [sm]
    "Return the current ideal state.")
  (actual-state [sm]
    "Return the current actual state.")
  (converge! [sm actual ideal]
    "Make the actual state converge towards the ideal state."))
