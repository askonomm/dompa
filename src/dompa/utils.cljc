(ns dompa.utils
  (:require [dompa.nodes :as nodes]))

(defmacro defhtml
  {:clj-kondo/lint-as 'clojure.core/defn}
  [name & args-and-elements]
  (let [[args & elements] args-and-elements]
    `(defn ~name ~args
       (nodes/->html (vector ~@elements)))))

(defn- flattench [children]
  (mapcat #(if (sequential? %) (flattench %) [%]) children))

(defmacro $
  {:clj-kondo/lint-as 'clojure.core/list}
  [name & opts]
  `(if (string? ~name)
     {:node/name  :dompa/text
      :node/value (apply str ~name ~opts)}
     (let [name# ~name
           opts-list# (list ~@opts)
           first-opt# (first opts-list#)
           attrs?# (and (map? first-opt#)
                        (not (contains? first-opt# :node/name)))
           attrs# (if attrs?# first-opt# {})
           children# (if attrs?# (rest opts-list#) opts-list#)]
       (merge
         {:node/name name#}
         (when attrs?# {:node/attrs attrs#})
         (when (seq children#) {:node/children (flattench children#)})))))

(defhtml page [who]
  ($ :div {:class "test"}
    ($ "hello world")
    (let [n "who"]
      ($ n))
    (for [x ["1" "2" "3"]]
      ($ x))
    ($ who)
    (mapv #($ %) ["a" "b" "c"])))

(page "world")