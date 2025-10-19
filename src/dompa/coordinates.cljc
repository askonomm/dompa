(ns dompa.coordinates
  (:require [clojure.string :as str]))

(defn- text-ended-and-tag-begins? [char char-type]
  (and (= :text char-type)
       (= \< char)))

(defn- text-ended-by-html-ending? [char-type total-char-count idx]
  (and (= :text char-type)
       (= (dec total-char-count) idx)))

(defn- tag-starts? [char char-type]
  (and (not= :text char-type)
       (= \< char)))

(defn- compose-reducer-fn
  "Returns a reducer function with initial state of
  `total-char-count` integer."
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
      (text-ended-and-tag-begins? c char-type)
      {:char-type :tag
       :start-idx idx
       :coordinates (conj coordinates [start-idx (dec idx)])}

      ; text ended by HTML ending, record text node
      ; coordinates
      (text-ended-by-html-ending? char-type total-char-count idx)
      {:char-type nil
       :start-idx idx
       :coordinates (conj coordinates [start-idx idx])}

      ; new tag starts while we were parsing another tag, 
      ; handles void elements
      (and (= :tag char-type) (= \< c))
      {:char-type :tag
       :start-idx idx
       :coordinates (conj coordinates [start-idx (dec idx)])}

      ; otherwise don't record anything, just note
      ; the start of a tag
      (tag-starts? c char-type)
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
  "Parses the given `html` string between the indexes of `start`
  and `end` for an HTML tag name.

  ```html
  <div>hello</div>
  ```

  Would become: `div`."
  [html [start end]]
  (let [value (subs html start end)]
    (if (str/starts-with? value "<")
      (->> (subs html start end)
           (take-while #(not (contains? #{\space \>} %)))
           (remove #(contains? #{\< \/} %))
           (apply str))
      value)))

(defn- coordinate-info
  "Determines if a coordinate is an opening tag, closing tag, or text."
  [html [start end]]
  (let [value (subs html start (inc end))]
    (cond
      (str/starts-with? value "</")
      {:coord-type :closing, :coord-name (coordinates->tag-name html [start end])}

      (str/starts-with? value "<")
      {:coord-type :opening, :coord-name (coordinates->tag-name html [start end])}

      :else
      {:coord-type :text, :coord-name :dompa/text})))

(def ^:private void-elements
  #{"!DOCTYPE" "!doctype" "area" "base" "br" "col" "embed" "hr" "img" 
    "input" "link" "meta" "param" "source" "track" "wbr"})

(defn- handle-opening-tag [{:keys [stack unified coord coord-name start]}]
  (if (void-elements coord-name)
    {:stack stack
     :unified (conj unified coord)}
    {:stack (conj stack {:name coord-name :start start})
     :unified unified}))

(defn- handle-closing-tag [{:keys [stack unified coord-name end]}]
  (if-let [last-open (peek stack)]
    (if (= coord-name (:name last-open))
      {:stack (pop stack)
       :unified (conj unified [(:start last-open) end])}
      {:stack stack :unified unified})
    {:stack stack :unified unified}))

(defn- unify-reducer-fn [html]
  (fn [{:keys [stack unified]} [start end :as coord]]
    (let [{:keys [coord-type coord-name]} (coordinate-info html coord)]
      (cond
        (= coord-type :opening)
        (handle-opening-tag {:stack stack
                             :unified unified
                             :coord coord
                             :coord-name coord-name
                             :start start})

        (= coord-type :closing)
        (handle-closing-tag {:stack stack
                             :unified unified
                             :coord-name coord-name
                             :end end})

        :else
        {:stack stack
         :unified (conj unified coord)}))))

(defn unify
  "Joins together given `coordinates` that represent
  one HTML node in `html`, using a stack-based approach to correctly
  handle nested and void tags."
  [{:keys [html coordinates]}]
  (let [initial-state {:stack [], :unified []}
        result (reduce (unify-reducer-fn html) initial-state coordinates)]
    {:html html
     :coordinates (sort-by first (:unified result))}))

(defn- children
  "Returns all the coordinates that belong between the given
  `from` and `to` indexes."
  [coordinates [from to]]
  (->> coordinates
       (filter (fn [[iter-from iter-to]]
                 (and (< from iter-from)
                      (> to iter-to))))
       (sort-by first)))

(defn- without-children
  "Returns all the coordinates that do not belong between
  the given `parent-from` and `parent-to` indexes."
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
  (str/split attr #"=" 2))

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
        k (keyword k)]
    {k (if (nil? v) true (normalize-html-attr-str v))}))

(defn- html->str->node-attrs-reducer-fn
  "Returns a reducer function with initial state of `attrs-html`."
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

(defn- html->attrs-html
  "Transforms a given `html` string into a string portion of
  just the attributes.

  ```html
  <div class=\"test\"></div>
  ```

  would become

  ```
  class=\"test\"
  ```"
  [html]
  (let [attrs-html (->> (subs html 1)
                        (take-while #(not (contains? #{\>} %)))
                        (partition-by #(= % \space))
                        (drop 1)
                        flatten
                        (apply str)
                        str/trim)]
    (if (= \/ (last attrs-html))
      (subs attrs-html 0 (dec (count attrs-html)))
      attrs-html)))

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
    (let [attrs-html (html->attrs-html html)
          indexed-attrs-html (map-indexed vector attrs-html)
          default-reducer-state {:start-idx 0
                                 :has-attrs? false
                                 :attrs []}]
      (as-> (html->str->node-attrs-reducer-fn attrs-html) $
        (reduce $ default-reducer-state indexed-attrs-html)
        (remove str/blank? (:attrs $))
        (map parse-html-attr-str $)
        (into {} $)))))

(defn- construct-node
  "Constructs a node map from `node-html` string and
  its children nodes."
  [node-html node-children]
  (let [node-name (html-str->node-name node-html)
        node-attrs (html-str->node-attrs node-html)]
    (cond-> {:node/name node-name}
      (= node-name :dompa/text) (assoc :node/value node-html)
      (not (nil? node-attrs)) (assoc :node/attrs node-attrs)
      (not (nil? node-children)) (assoc :node/children node-children))))

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