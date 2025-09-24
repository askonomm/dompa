(ns hooks.dompa
  (:require [clj-kondo.hooks-api :as api]))

(defn string-like? [x]
  (or (api/string-node? x)
      (api/token-node? x)))

(defn $ [{:keys [node]}]
  (let [[_ first-arg & rest-args] (:children node)]
    (cond
      ; if the first arg is string-like, then no sequences
      ; are allowed, because you are only allowed to concat
      ; strings, i.e. whatever (str) can do.
      (or (api/string-node? first-arg)
          (api/token-node? first-arg))
      (let [invalid-args (filter #(not (string-like? %)) rest-args)]
        (doall
          (for [invalid-arg invalid-args]
            (api/reg-finding!
              (assoc (meta invalid-arg)
                :message (str "Invalid argument type. When creating a text node, "
                              "only literal values (strings, numbers and symbols) are allowed.")
                :type :dompa.utils/$-arg-validation)))))

      ; if the first arg is a keyword, then the second argument can only be
      ; a sequence or a map.
      (and (api/keyword-node? first-arg)
           (seq rest-args)
           (not (or (api/map-node? (first rest-args))
                    (api/list-node? (first rest-args)))))
      (api/reg-finding!
        (assoc (meta (first rest-args))
          :message (str "Invalid argument type. When creating a non-text node, "
                        "the second argument must be a sequence or a map. "
                        "In other words, the second argument must be an attribute map "
                        "or sequence of other nodes created with the $ macro.")
          :type :dompa.utils/$-arg-validation)))))