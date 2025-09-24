(ns dompa.utils
  (:require [dompa.nodes :as nodes]))

(defmacro defhtml
  {:clj-kondo/lint-as 'clojure.core/defn}
  [name & args-and-elements]
  (let [[args & elements] args-and-elements]
    `(defn ~name ~args
       (nodes/->html (vector ~@elements)))))

(defn ->flat-xf []
  (fn [rf]
    (letfn [(step [result input]
              (if (sequential? input)
                (reduce step result input)                  ;; recursively reduce over nested seq
                (rf result input)))]                        ;; apply rf with accumulator + value
      (fn
        ([] (rf))                                           ;; init
        ([result] (rf result))                              ;; completion
        ([result input] (step result input))))))            ;; step

(defn ->flat [children]
  (into [] (->flat-xf) children))

(defmacro $
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
               (seq children#) (assoc :node/children (->flat children#))))))

(defhtml test-page []
  (let [n 123]
    ($ :div
      ($ "hello world" n))
    ($ "hello")
    ($ :div "test")))

(test-page)
