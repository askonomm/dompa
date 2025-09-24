(ns dompa.nodes)

(def ^:private default-void-nodes
  #{:!doctype :area :base :br :col :embed :hr :img :input
    :link :meta :source :track :wbr})

(defn- node-attrs-reducer [attrs k v]
  (let [attr-name (-> k name)]
    (if (true? v)
      (str attrs " " attr-name)
      (str attrs " " attr-name "=\"" v "\""))))

(defn- node->html-reducer-fn
  [void-nodes nodes->html-fn]
  (fn [html node]
    (when-not (nil? node)
      (let [node-name (-> node :node/name name)
            node-attrs (reduce-kv node-attrs-reducer "" (-> node :node/attrs))]
        (cond
          (= (-> node :node/name) :dompa/text)
          (str html (-> node :node/value))

          (contains? void-nodes (-> node :node/name))
          (str html "<" node-name node-attrs">")

          :else
          (let [value (nodes->html-fn (-> node :node/children))]
            (str html "<" node-name node-attrs ">" value "</" node-name ">")))))))

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