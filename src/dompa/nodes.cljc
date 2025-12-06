(ns dompa.nodes
  (:require
   [clojure.zip :as zip]))

(def ^:private default-void-nodes
  #{:!doctype :!DOCTYPE :area :base :br :col :embed :hr :img :input
    :link :meta :source :track :wbr})

(defn- node-attrs-reducer [attrs k v]
  (let [attr-name (-> k name)]
    (if (true? v)
      (str attrs " " attr-name)
      (str attrs " " attr-name "=\"" v "\""))))

(defn- node->html-reducer-fn
  "Returns a reducer function with the initial state of `void-nodes`
  set and `nodes->html-fn` function for recursive HTML creation."
  [void-nodes nodes->html-fn]
  (fn [html node]
    (cond
      ; fragment nodes expand their children to replace themselves
      (and (not (nil? node))
           (= (:node/name node) :<>))
      (str html (nodes->html-fn (:node/children node)))

      ; otherwise business as usual
      (not (nil? node))
      (let [node-name (-> node :node/name name)
            node-attrs (reduce-kv node-attrs-reducer "" (-> node :node/attrs))]
        (cond
          (= (-> node :node/name) :dompa/text)
          (str html (-> node :node/value))

          (contains? void-nodes (-> node :node/name))
          (str html "<" node-name node-attrs ">")

          :else
          (let [value (nodes->html-fn (-> node :node/children))]
            (str html "<" node-name node-attrs ">" value "</" node-name ">"))))

      :else "")))

(defn traverse
  "Recursively traverses given tree of `nodes` with a `traverser-fn`
  that gets a single node passed to it and returns a new updated tree.
  If the traverses function returns `nil`, the node will be removed.
  In any other case the node will be replaced. If you wish to keep
  a node unchanged, just return it as-is."
  [nodes traverser-fn]
  (-> (fn [updated-nodes node]
        (if-let [updated-node (traverser-fn node)]
          (let [children (traverse (-> updated-node :node/children) traverser-fn)]
            (conj updated-nodes (assoc updated-node :node/children children)))
          updated-nodes))
      (reduce [] nodes)))

(defn zip
  "Creates a zipper for given a given `node`."
  [node]
  (zip/zipper
   (fn branch? [node]
     (boolean (seq (:node/children node))))
   (fn children [node]
     (:node/children node))
   (fn make-node [node children]
     (assoc node :node/children children))
   node))

(defn ->html
  "Transform a vector of `nodes` into an HTML string.

  Options:
  - `void-nodes` - A set of node names that are self-closing, defaults to:
    - `:!doctype`
    - `:area`
    - `:base`
    - `:br`
    - `:col`
    - `:embed`
    - `:hr`
    - `:img`
    - `:input`
    - `:link`
    - `:meta`
    - `:source`
    - `:track`
    - `:wbr`
  "
  ([nodes]
   (->html nodes {:void-nodes default-void-nodes}))
  ([nodes {:keys [void-nodes]}]
   (-> (node->html-reducer-fn void-nodes ->html)
       (reduce "" nodes))))

(defmacro defhtml
  "Creates a new function with `name` that outputs HTML.

  Example usage:

  ```clojure
  (defhtml about-page [who]
    ($ :div
      ($ \"hello \" who)))

  (about-page \"world\")
  ```
  "
  [name & args-and-elements]
  (let [[args & elements] args-and-elements]
    `(defn ~name ~args
       (->html (vector ~@elements)))))

(defn $$
  "Creates a new node
  
  Usage:

  ```clojure
  ($ :div
    ($ \"hello world\" ))
  ```"
  [name & opts]
  (if (string? name)
    {:node/name  :dompa/text
     :node/value (apply str name opts)}
    (let [first-opt (first opts)
          attrs? (and (map? first-opt)
                      (not (contains? first-opt :node/name)))
          attrs (if attrs? first-opt {})
          children (if attrs? (rest opts) opts)]
      (cond-> {:node/name name}
        attrs? (assoc :node/attrs attrs)
        (seq children) (assoc :node/children (flatten children))))))

(defn- list-of-one?
  [coll]
  (and (sequential? coll)
       (= (count coll) 1)))

(defn- list-of-many?
  [coll]
  (and (sequential? coll)
       (> (count coll) 1)))

(defn- nodes-from-opt
  [opt]
  (cond (map? opt)
        opt

        (list-of-many? opt)
        {:node/name :<>
         :node/children opt}

        (list-of-one? opt)
        (first opt)

        :else
        {:node/name :dompa/text
         :node/value (str opt)}))

(defn- node-from-opts
  [opts]
  (let [first-opt (first opts)
        second-op (second opts)
        attrs? (and (map? second-op)
                    (not (contains? second-op :node/name)))
        attrs (if attrs? second-op {})
        children-opts (if attrs? (drop 2 opts) (rest opts))
        children-nodes (->> children-opts
                            (map nodes-from-opt)
                            flatten)]
    (cond-> {:node/name first-opt}
      attrs? (assoc :node/attrs attrs)
      (seq children-nodes) (assoc :node/children children-nodes))))

(defn- nodes-from-opts
  [opts]
  (let [nodes (->> (remove nil? opts)
                   (map nodes-from-opt))]
    (if (> (count nodes) 1)
      {:node/name :<>
       :node/children nodes}
      (first nodes))))

(defn $
  "Creates a new node
  
  Usage:

  ```clojure
  ($ :div
    ($ \"hello world\" ))
  ```"
  [& opts]
  (if (keyword? (first opts))
    (node-from-opts opts)
    (nodes-from-opts opts)))
