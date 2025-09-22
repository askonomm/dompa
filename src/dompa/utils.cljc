(ns dompa.utils
  (:require [dompa.nodes :as nodes]))

(defmacro defhtml [name & args-and-elements]
  (let [[args & elements] args-and-elements]
    `(defn ~name ~args
       (nodes/->html (vector ~@elements)))))

(defn $ [name & opts]
  (if (string? name)
    {:node/name  :dompa/text
     :node/value (str name (apply str opts))}
    (let [node {:node/name name}
          attrs? (not (get (first opts) :node/name))
          attrs (if attrs? (first opts) {})
          children (if attrs? (rest opts) opts)]
      (merge
        node
        (when attrs?
          {:node/attrs attrs})
        (when-not (empty? children)
          {:node/children children})))))

(defhtml page
  [test]
  ($ :!doctype {:html true})
  ($ :html {:lang "en"}
    ($ :head
      ($ :meta {:charset "utf-8"})
      ($ :link {:rel "stylesheet" :href "style.css"}))
    ($ :body
      ($ :span {:class "test"})
      ($ :span {:class "test2"}
        ($ "hello" test)))))

(prn (page "world"))

(comment
  ($ :div {:class "test"}
    ($ "asdasd" "asd")))