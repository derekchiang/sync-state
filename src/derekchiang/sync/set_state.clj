(ns derekchiang.sync.set-state)

(defprotocol SetStateManager
  "SetStateManager is a `StateManager` for states (both ideal and actual) that
  can be modeled as a set.

  The ideal state set and the actual state set will be used to compute a set of
  positive differences (i.e. things to add) and a set of negative differences
  (i.e. things to remove).  Each diff will then be called with `apply-pos-diff`
  and `apply-neg-diff` respectively."
  (ideal-state [sm]
    "Return the current ideal state.")
  (actual-state [sm]
    "Return the current actual state.")
  (apply-pos-diff [sm diff]
    "Apply a positive diff.")
  (apply-neg-diff [sm diff]
    "Apply a negative diff."))

