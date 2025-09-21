(ns dompa.utils)

(defmacro $ [name & opts]
  (let [node# {:name (keyword name)}
        attrs? (map? (first opts))
        attrs (if attrs? (first opts) {})
        children (if attrs? (rest opts) opts)]
    (merge
      node#
      (when attrs?
        {:attrs attrs})
      (when-not (empty? children)
        {:children (into [] children)}))))

(comment
  (list
    ($ doctype {:html true})
    ($ head)
    ($ body
      ($ span {:class "test"}
        ($ span {:class "test2"})))))