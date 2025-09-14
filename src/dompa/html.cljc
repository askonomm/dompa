(ns dompa.html
  (:require
    [dompa.coordinates :as coordinates]))

(defn ->coordinates
  "Transform a `html` string into a vector of coordinates
  indicating where an HTML node ends and begins."
  [html]
  (->> coordinates/compose
       (coordinates/unify html)))

(defn ->nodes
  "Transform a `html` string into a tree of nodes,
  each representing one HTML node and its children."
  [html]
  (->> (->coordinates html)
       (coordinates/->nodes html)))