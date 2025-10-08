(ns hooks.dompa
  (:require [clj-kondo.hooks-api :as api]))

(defn str-able? [x]
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
      (let [invalid-args (filter #(not (str-able? %)) rest-args)]
        (doseq [invalid-arg invalid-args]
          (api/reg-finding!
            (assoc (meta invalid-arg)
              :message (str "Invalid argument type. When creating a text node, "
                            "only literal values (strings, numbers and symbols) "
                            "are allowed.")
              :type :dompa.nodes/$-arg-validation))))

      ; if the first arg is a keyword, then the second argument can only be
      ; a sequence or a map.
      (and (api/keyword-node? first-arg)
           (seq rest-args)
           (not (or (api/map-node? (first rest-args))
                    (api/list-node? (first rest-args)))))
      (api/reg-finding!
        (assoc (meta (first rest-args))
          :message "Invalid argument type. Argument must be a sequence or a map."
          :type :dompa.nodes/$-arg-validation))

      ; if the first arg is a keyword, the second arg is a list, then
      ; every arg has to be a list node.
      (and (api/keyword-node? first-arg)
           (api/list-node? (first rest-args))
           (not (every? #(api/list-node? %) (rest rest-args))))
      (doseq [arg (filter #(not (api/list-node? %)) rest-args)]
        (api/reg-finding!
          (assoc (meta arg)
            :message (str "Invalid argument type. Argument must be a $ macro "
                          "or a sequence of $ macros.")
            :type :dompa.nodes/$-arg-validation)))

      ; if the first arg is a keyword, the second arg is a map, then from
      ; the second forwards everything has to be a list node
      (and (api/keyword-node? first-arg)
           (api/map-node? (first rest-args))
           (not (every? #(api/list-node? %) (rest rest-args))))
      (api/reg-finding!
        (assoc (meta (second rest-args))
          :message (str "Invalid argument type. Argument must be a $ macro "
                        "or a sequence of $ macros.")
          :type :dompa.nodes/$-arg-validation)))))

(defn defhtml [{:keys [node]}]
  (let [[_ first-arg second-arg & rest-args] (:children node)]
    (cond
      ; first argument has to be a symbol
      (not (api/token-node? first-arg))
      (api/reg-finding!
        (assoc (meta first-arg)
          :message "Invalid argument type. Binding name must be a symbol."
          :type :dompa.templates/defhtml-arg-validation))

      ; second argument should be a vector
      (not (api/vector-node? second-arg))
      (api/reg-finding!
        (assoc (meta second-arg)
          :message "Invalid argument type. Must be a vector of arguments."
          :type :dompa.templates/defhtml-arg-validation))

      ; rest of the arguments should be a list
      (not (every? #(api/list-node? %) rest-args))
      (doseq [arg rest-args]
        (api/reg-finding!
          (assoc (meta arg)
            :message (str "Invalid argument type. Argument must be a $ macro "
                          "or a sequence of $ macros.")))))))

