(ns dompa.utils
  (:require [dompa.nodes :as nodes]
            [criterium.core :as c]))

(defmacro defhtml
  {:clj-kondo/lint-as 'clojure.core/defn}
  [name & args-and-elements]
  (let [[args & elements] args-and-elements]
    `(defn ~name ~args
       (nodes/->html (vector ~@elements)))))

(defn flattench-xf []
  (fn [rf]
    (letfn [(step [result input]
              (if (sequential? input)
                (reduce step result input)                  ;; recursively reduce over nested seq
                (rf result input)))]                        ;; apply rf with accumulator + value
      (fn
        ([] (rf))                                           ;; init
        ([result] (rf result))                              ;; completion
        ([result input] (step result input))))))            ;; step

(defn flattench [children]
  (into [] (flattench-xf) children))

(defmacro $
  {:clj-kondo/lint-as 'clojure.core/list}
  [name & opts]
  `(if (string? ~name)
     {:node/name  :dompa/text
      :node/value (apply str ~name ~@opts)}
     (let [opts# (list ~@opts)
           first-opt# (first opts#)
           attrs?# (and (map? first-opt#)
                        (not (contains? first-opt# :node/name)))
           attrs# (if attrs?# first-opt# {})
           children# (if attrs?# (rest opts#) opts#)]
       (cond-> {:node/name ~name}
               attrs?# (assoc :node/attrs attrs#)
               (seq children#) (assoc :node/children (flattench children#))))))

(defn bench-n []
  (c/quick-bench
    (dotimes [_ 1500]
      ($ :div {:class "container"}
          (map #($ %) ["a" "b" "c"])
          ($ "hello world")))))

(defhtml test-page []
  ($ :div
      (map #($ %) ["a" "b" "c"])
      ($ "hello world")))

(test-page)
