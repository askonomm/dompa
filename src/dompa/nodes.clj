(ns dompa.nodes
  (:require [clojure.string :as str]
            [dompa.coordinates :as coordinates]))

(defn- html->node-name [html]
  (if (str/starts-with? html "<")
    (-> html
        (str/split #"[\s\>]")
        first
        (str/replace #"[\<\>\/]" "")
        keyword)
    :text-node))

(defn- html->node-attrs [html])



(defn coordinates->nodes
  [html coordinates]
  (when (seq coordinates)
    (let [sorted-coordinates (sort-by first coordinates)
          [parent-from parent-to] (first sorted-coordinates)
          children (coordinates/children sorted-coordinates [parent-from parent-to])
          remaining (coordinates/without-children sorted-coordinates [parent-from parent-to])
          node-html (subs html parent-from (inc parent-to))]
      (cons {:value (subs html parent-from (inc parent-to))
             :name (html->node-name node-html)
             :attrs (html->node-attrs node-html)
             :children (coordinates->nodes html children)}
            (coordinates->nodes html remaining)))))
