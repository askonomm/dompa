(ns dompa.core
  (:require
    [dompa.coordinates :refer [html->coordinates]]
    [dompa.nodes :refer [coordinates->nodes]]))

(defn html->nodes [html]
  (let [coordinates (html->coordinates html)
        nodes (coordinates->nodes html coordinates)]
    nodes))

(comment
  (html->coordinates "<div>hello<span>asd</span><strong>asdasdadad<img></strong></div>hello some text<div>another root element</div>")
  (html->nodes "<div>hello<span>asd</span><strong>asdasdadad</strong></div>"))