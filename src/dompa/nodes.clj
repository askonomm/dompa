(ns dompa.nodes
  (:require
    [clojure.string :as str]
    [dompa.coordinates :as coordinates]))

(defn- html->node-name
  "Parses a given HTML string of a node to get its name as
  a keyword. A text node will return `:dompa/text`."
  [html]
  (if (str/starts-with? html "<")
    (->> (subs html 1)
         (take-while #(not (contains? #{\space \>} %)))
         (reduce str)
         keyword)
    :dompa/text))

(defn- attr->k-v
  "Parses a given HTML node attribute string into a
  key-value pair."
  [attr]
  (->> (partition-by #(= % \=) attr)
       (filter #(not= (-> % first) \=))
       (map #(reduce str %))))

(defn- normalize-attr-str
  "Normalizes a given HTML attribute string. If it
  has surrounding quotes, removes them."
  [attr-str]
  (if (str/starts-with? attr-str "\"")
    (->> (subs attr-str 1)
         (take-while #(not= % \"))
         (reduce str))
    attr-str))

(defn- parse-attr
  "Parses a given HTML attribute into a normalized
  key-value map. Attributes with no value part are
  treated as boolean attributes, and are always `true`."
  [attr]
  (let [[k v] (attr->k-v attr)
        k (keyword k)
        v (if (nil? v) true (normalize-attr-str v))]
    {k v}))

(defn- html->node-attrs [html]
  (when (str/starts-with? html "<")
    (->> (subs html 1)
         (take-while #(not (contains? #{\> \/} %)))
         (partition-by #(= % \space))
         (drop 1)
         (filter #(not= (-> % first) \space))
         (map parse-attr)
         (into {}))))

(comment
  (html->node-attrs "<img src=\"test.jpg\" checked />"))

(defn- construct-node
  [node-html node-children]
  (let [node-name (html->node-name node-html)]
    (merge
      {:name node-name}
      (when (= node-name :dompa/text)
        {:value node-html})
      (when-let [attrs (html->node-attrs node-html)]
        {:attrs attrs})
      (when node-children
        {:children node-children}))))

(defn coordinates->nodes
  [html coordinates]
  (when (seq coordinates)
    (let [sorted-coordinates (sort-by first coordinates)
          [parent-from parent-to] (first sorted-coordinates)
          children (coordinates/children sorted-coordinates [parent-from parent-to])
          remaining (coordinates/without-children sorted-coordinates [parent-from parent-to])
          node-html (subs html parent-from (inc parent-to))
          node-children (coordinates->nodes html children)]
      (cons (construct-node node-html node-children)
            (coordinates->nodes html remaining)))))
