(ns dompa.utils)

(defn- make-node
  [name & opts]
  (let [node {:name name}]
    (if (get (first opts) :name)
      (merge
        node
        (when-not (empty? opts)
          {:children opts}))
      (let [attrs (first opts)
            [_ & children] opts]
        (merge
          node
          {:attrs attrs}
          (when-not (empty? children)
            {:children children}))))))

(defn doctype []
  (make-node :!DOCTYPE {:html true}))

(defn head [& opts]
  (make-node :head opts))

(defn body [& opts]
  (make-node :body opts))

(defn span [& opts]
  (make-node :span opts))

(comment
  (list
    (doctype)
    (head)
    (body
      (span {:class "test"} (span {:class "test2"})))))