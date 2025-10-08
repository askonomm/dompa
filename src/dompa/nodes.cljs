(ns dompa.nodes)

(defn $
  "Creates a new node
  
  Usage:

  ```clojure
  ($ :div
    ($ \"hello world\" ))
  ```"
  [name & opts]
  (if (string? name)
    {:node/name  :dompa/text
     :node/value (apply str name opts)}
    (let [first-opt (first opts)
          attrs? (and (map? first-opt)
                      (not (contains? first-opt :node/name)))
          attrs (if attrs? first-opt {})
          children (if attrs? (rest opts) opts)]
      (cond-> {:node/name name}
              attrs? (assoc :node/attrs attrs)
              (seq children) (assoc :node/children children)))))

