(ns dompa.coordinates
  (:require [clojure.string :as str]))

(defn- construct-coordinates-reducer
  [{:keys [char-type start-idx coordinates] :as state} [idx c]]
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

    :else state))

(defn- construct-coordinates
  [indexed-html]
  (->> indexed-html
       (reduce construct-coordinates-reducer {:char-type nil :start-idx 0 :coordinates []})
       :coordinates))

(defn coordinates->tag-name [html [from to]]
  (-> (subs html from to)
      (str/split #"[\s\>]")
      first
      (str/replace #"[\<\>\/]" "")))

(defn- name-coordinates-fn [html]
  (fn [idx coordinate]
    [idx (coordinates->tag-name html coordinate)]))

(defn- last-coordinate-by-tag-name-idx [html coordinates name start]
  (let [filter-fn (fn [[_ end]] (< end start))
        filtered-coordinates (filter filter-fn coordinates)
        index-fn (name-coordinates-fn html)
        named-coordinates (map-indexed index-fn filtered-coordinates)]
    (->> named-coordinates
         (filter #(= name (-> % last)))
         last
         first)))

(defn- merge-coordinate [html coordinates [start end]]
  (let [name (coordinates->tag-name html [start end])
        matching-idx (last-coordinate-by-tag-name-idx html coordinates name start)
        [matching-start] (nth coordinates matching-idx)]
    (assoc coordinates matching-idx [matching-start end])))

(defn- merge-coordinates-reducer-fn [html]
  (fn [coordinates [start end]]
    (if (and (= \< (nth html start))
             (= \/ (nth html (inc start) nil)))
      (merge-coordinate html coordinates [start end])
      (conj coordinates [start end]))))

(defn merge-coordinates [html coordinates]
  (-> (merge-coordinates-reducer-fn html)
      (reduce [] coordinates)))

(defn children
  [coordinates [from to]]
  (->> coordinates
       (filter (fn [[iter-from iter-to]]
                 (and (< from iter-from)
                      (> to iter-to))))
       (sort-by first)))

(defn without-children
  [coordinates [parent-from parent-to]]
  (->> coordinates
       (remove (fn [[from to]]
                 (or (= from parent-from)
                     (and (> from parent-from)
                          (< to parent-to)))))))

(defn html->coordinates [html]
  (->> (map-indexed vector html)
       construct-coordinates
       (merge-coordinates html)))
