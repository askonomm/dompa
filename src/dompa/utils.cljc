(ns dompa.utils
  (:require [dompa.nodes :as nodes]))

(defmacro $ [name & opts]
  (if (string? name)
    {:name :dompa/text
     :value name}
    (let [node {:name name}
          attrs? (map? (first opts))
          attrs (if attrs? (first opts) {})
          children (if attrs? (rest opts) opts)]
      (merge
        node
        (when attrs?
          {:attrs attrs})
        (when-not (empty? children)
          {:children (into [] children)})))))

(defn- page []
  (list
    ($ :!doctype {:html true})
    ($ :html {:lang "en"}
      ($ :head
        ($ :meta {:charset "utf-8"})
        ($ :link {:rel "stylesheet" :href "style.css"}))
      ($ :body
        ($ :span {:class "test"}
        ($ :span {:class "test2"}
          ($ "hello, world.")))))))

(comment
  ($ "asdasd")
  (page)
  (nodes/->html (page)))