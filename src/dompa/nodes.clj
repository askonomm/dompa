(ns dompa.nodes)

(defmacro $
  "Creates a new node. Particularly useful
  where you need compile-time composition over run-time, like when
  combined with the `defhtml` macro to create HTML string outputs.

  Usage:

  ```clojure
  ($ :div
    ($ \"hello world\" ))
  ```"
  [name & opts]
  `(if (string? ~name)
     {:node/name  :dompa/text
      :node/value (str ~name ~@opts)}
     (let [opts# (list ~@opts)
           first-opt# (first opts#)
           attrs?# (and (map? first-opt#)
                        (not (contains? first-opt# :node/name)))
           attrs# (if attrs?# first-opt# {})
           children# (if attrs?# (rest opts#) opts#)]
       (cond-> {:node/name ~name}
               attrs?# (assoc :node/attrs attrs#)
               (seq children#) (assoc :node/children children#)))))
