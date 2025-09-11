(ns dompa.nodes
  (:require
    [clojure.string :as str]
    [dompa.coordinates :as coordinates]))

(defn- html-str->node-name
  "Parses a given HTML string of a node to get its name as
  a keyword. A text node will return `:dompa/text`."
  [html]
  (if (str/starts-with? html "<")
    (->> (subs html 1)
         (take-while #(not (contains? #{\space \>} %)))
         (reduce str)
         keyword)
    :dompa/text))

(defn- html-attr-str->k-v
  "Parses a given HTML node attribute string into a
  key-value pair."
  [attr]
  (->> (partition-by #(= % \=) attr)
       (filter #(not= (-> % first) \=))
       (map #(reduce str %))))

(defn- normalize-html-attr-str
  "Normalizes a given HTML attribute string. If it
  has surrounding quotes, removes them."
  [html-attr-str]
  (if (str/starts-with? html-attr-str "\"")
    (->> (subs html-attr-str 1)
         (take-while #(not= % \"))
         (reduce str))
    html-attr-str))

(defn- parse-html-attr-str
  "Parses a given HTML attribute into a normalized
  key-value map. Attributes with no value part are
  treated as boolean attributes, and are always `true`."
  [html-attr-str]
  (let [[k v] (html-attr-str->k-v html-attr-str)
        k (keyword k)
        v (if (nil? v) true (normalize-html-attr-str v))]
    {k v}))

(defn- html-str->node-attrs [html]
  (when (str/starts-with? html "<")
    (->> (subs html 1)
         (take-while #(not (contains? #{\> \/} %)))
         (partition-by #(= % \space))
         (drop 1)
         (filter #(not= (-> % first) \space))
         (map parse-html-attr-str)
         (into {}))))

(defn- construct-node
  [node-html node-children]
  (let [node-name (html-str->node-name node-html)]
    (merge
      {:name node-name}
      (when (= node-name :dompa/text)
        {:value node-html})
      (when-let [attrs (html-str->node-attrs node-html)]
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
