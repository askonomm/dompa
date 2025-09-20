(ns dompa.coordinates
  (:require [clojure.string :as str]))

(defn- compose-reducer-fn
  [total-char-count]
  (fn [{:keys [char-type start-idx coordinates] :as state} [idx c]]
    (cond
      ; we're undecided what to do next,
      ; so we figure it out here
      (nil? char-type)
      {:char-type (if (some #{c} "<>") :tag :text)
       :start-idx idx
       :coordinates coordinates}

      ; text ended, tag begins, which means we can
      ; record text node coordinates
      (and (= :text char-type)
           (= \< c))
      {:char-type :tag
       :start-idx idx
       :coordinates (conj coordinates [start-idx (dec idx)])}

      ; text ended by HTML ending, record text node
      ; coordinates
      (and (= :text char-type)
           (= (dec total-char-count) idx))
      {:char-type nil
       :start-idx idx
       :coordinates (conj coordinates [start-idx idx])}

      ; otherwise don't record anything, just note
      ; the start of a tag
      (and (not= :text char-type)
           (= \< c))
      {:char-type :tag
       :start-idx idx
       :coordinates coordinates}

      ; tag ended, record tag node coordinates
      (= \> c)
      {:char-type nil
       :start-idx idx
       :coordinates (conj coordinates [start-idx idx])}

      :else state)))

(defn compose
  "Composes a given `html` string into a vector of coordinates.
  These are single-pass coordinates without awareness of context,
  thus HTML such as:

  ```html
  <div>hello</div>
  ```

  will return 3 coordinates (div, text, div) instead of 2 (div, text).
  To unify the coordinates in a context-aware way, you pass the result
  of this function to the `unify` function."
  [html]
  (let [default-state {:char-type nil
                       :start-idx 0
                       :coordinates []}
        indexed-html (map-indexed vector html)]
    {:html html
     :coordinates (-> (compose-reducer-fn (count indexed-html))
                      (reduce default-state indexed-html)
                      :coordinates)}))

(defn- coordinates->tag-name
  [html [start end]]
  (let [value (subs html start end)]
    (if (str/starts-with? value "<")
      (->> (subs html start end)
           (take-while #(not (contains? #{\space \>} %)))
           (remove #(contains? #{\< \/} %))
           (apply str))
      value)))

(defn- name-coordinates-fn
  [html]
  (fn [idx coordinate]
    [idx (coordinates->tag-name html coordinate)]))

(defn- last-by-tag-name-idx
  [html coordinates name start]
  (let [filter-fn (fn [[_ end]] (< end start))
        filtered-coordinates (filter filter-fn coordinates)
        index-fn (name-coordinates-fn html)
        named-coordinates (map-indexed index-fn filtered-coordinates)]
    (->> named-coordinates
         (filter #(= name (-> % last)))
         last
         first)))

(defn- unify-one
  [html coordinates [start end]]
  (let [name (coordinates->tag-name html [start end])
        matching-idx (last-by-tag-name-idx html coordinates name start)]
    (if matching-idx
      (let [[matching-start] (nth coordinates matching-idx)]
        (assoc coordinates matching-idx [matching-start end]))
      coordinates)))

(defn- unify-reducer-fn
  [html]
  (fn [coordinates [start end]]
    (if (and (= \< (nth html start))
             (= \/ (nth html (inc start) nil)))
      (unify-one html coordinates [start end])
      (conj coordinates [start end]))))

(defn unify
  "Joins together given `coordinates` that represent
  one HTML node in `html`, without which `html` such as:

  ```html
  <div>hello</div>
  ```

  would result in 3 nodes (div, text, div), instead of 2 (div, text),
  because non-unified coordinates are blind to the context
  in which they live, having only had one pass over the
  raw HTML string which composes the initial coordinates."
  [{:keys [html coordinates]}]
  {:html html
   :coordinates (-> (unify-reducer-fn html)
                    (reduce [] coordinates))})

(defn- children
  [coordinates [from to]]
  (->> coordinates
       (filter (fn [[iter-from iter-to]]
                 (and (< from iter-from)
                      (> to iter-to))))
       (sort-by first)))

(defn- without-children
  [coordinates [parent-from parent-to]]
  (->> coordinates
       (remove (fn [[from to]]
                 (or (= from parent-from)
                     (and (> from parent-from)
                          (< to parent-to)))))))

(defn- html-str->node-name
  "Parses a given HTML string of a node to get its name as
  a keyword. A text node will return `:dompa/text`."
  [html]
  (if (str/starts-with? html "<")
    (->> (subs html 1)
         (take-while #(not (contains? #{\space \>} %)))
         (apply str)
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

(defn- html->str->node-attrs-reducer
  [attrs-html]
  (fn [{:keys [start-idx has-attrs? attrs] :as state} [idx c]]
    (cond
      ; end of attrs-html, so lets collect whatever there is left
      (= (count attrs-html) (inc idx))
      {:start-idx 0
       :has-attrs? has-attrs?
       :attrs (conj attrs (subs attrs-html start-idx (inc idx)))}

      ; encountered a space, but there's no attrs, we're good
      ; to collect whatever there is.
      (and (= \space c)
           (not has-attrs?))
      {:start-idx (inc idx)
       :has-attrs? false
       :attrs (conj attrs (subs attrs-html start-idx idx))}

      ; if we discover a = with quote after it,
      ; it means we have attrs
      (and (= \= c)
           (= \" (get attrs-html (inc idx))))
      {:start-idx start-idx
       :has-attrs? true
       :attrs attrs}

      ; quote with either space next or nothing next,
      ; and no = before, and we have attrs
      (and (= \" c)
           (not (= \= (get attrs-html (dec idx))))
           (or (nil? (get attrs-html (inc idx)))
               (= \space (get attrs-html (inc idx))))
           has-attrs?)
      {:start-idx (inc idx)
       :has-attrs? false
       :attrs (conj attrs (subs attrs-html start-idx (inc idx)))}

      :else state)))

(defn- html->str->attrs-html-str [html]
  (->> (subs html 1)
       (take-while #(not (contains? #{\> \/} %)))
       (partition-by #(= % \space))
       (drop 1)
       flatten
       (reduce str)
       str/trim))

(defn- html-str->node-attrs
  "Turns a given `html` string into an attribute map, e.g:

  ```html
  <input type=\"checkbox\" checked />
  ```

  Would become:

  ```clojure
  {:type \"checkbox\"
   :checked true}
  ```"
  [html]
  (when (str/starts-with? html "<")
    (let [attrs-html (html->str->attrs-html-str html)
          indexed-attrs-html (map-indexed vector attrs-html)
          default-reducer-state {:start-idx 0
                                 :has-attrs? false
                                 :attrs []}]
      (as-> (html->str->node-attrs-reducer attrs-html) $
            (reduce $ default-reducer-state indexed-attrs-html)
            (remove str/blank? (:attrs $))
            (map parse-html-attr-str $)
            (into {} $)))))

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

(defn ->nodes
  "Transform given `html` according to given `coordinates` into
  a tree of nodes, each representing one HTML node and its children.

  Direct output of both `compose` and `unify` can be given to this
  function, allowing chaining such as:

  ```clojure
  (-> \"some html ...\"
      coordinates/compose
      coordinates/unify
      coordinates/->nodes)
  ```"
  [{:keys [html coordinates]}]
  (when (seq coordinates)
    (let [sorted-coordinates (sort-by first coordinates)
          [parent-from parent-to] (first sorted-coordinates)
          children (children sorted-coordinates [parent-from parent-to])
          remaining (without-children sorted-coordinates [parent-from parent-to])
          node-html (subs html parent-from (inc parent-to))
          node-children (->nodes {:html html :coordinates children})]
      (-> (cons (construct-node node-html node-children)
                (->nodes {:html html :coordinates remaining}))
          vec))))