(ns dompa.core
  (:require
    [dompa.coordinates :refer [html->coordinates]]
    [dompa.nodes :refer [coordinates->nodes]]))

(defn html->nodes [html]
  (->> (html->coordinates html)
       (coordinates->nodes html)))

(defn nodes->html 
  ([nodes]
   (nodes->html nodes {:void-nodes #{:img}}))
  ([nodes {:keys [void-nodes]}]
   (reduce
     (fn [html node]
       (cond
         (= (-> node :name) :dompa/text)
         (str html (-> node :value))

         :else
         (let [node-name (-> node :name name)
               node-child-html (nodes->html (-> node :children))]
           (str html "<" node-name ">" node-child-html "</" node-name ">"))))
     ""
     nodes)))

(defn traverse-nodes [nodes pred]
  (reduce
    (fn [updated-nodes node]
      (if-let [updated-node (pred node)]
        (let [children (traverse-nodes (-> updated-node :children) pred)]
          (conj updated-nodes (assoc updated-node :children children)))
        updated-nodes))
    []
    nodes))

(defn traverse-html [html pred]
  (-> (html->nodes html)
      (traverse-nodes pred)
      nodes->html))

(comment
  (traverse-html "<div>asdasd<span>hello</span></div>" #(when-not (= (-> % :name) :span)
                                                          %))
  (html->coordinates "<div>hello<span>asd</span><strong>asdasdadad<img></strong></div>hello some text<div>another root element</div>")
  (html->nodes "<div>hello<span><img src=\"test.jpg\" ckcche/>asd</span><strong>asdasdadad</strong>"))