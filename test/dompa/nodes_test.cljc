(ns dompa.nodes-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [dompa.nodes :refer [$]]
                    [dompa.nodes.shared :refer [defhtml traverse ->html]]
                    [dompa.html :as html]))
  #?(:cljs (:require [cljs.test :refer-macros [deftest testing is]]
                     [dompa.nodes :refer [$]]
                     [dompa.nodes.shared :refer [traverse ->html] :refer-macros [defhtml]]
                     [dompa.html :as html])))

(defhtml hello [who]
  ($ :div
     ($ "hello " who)))

(deftest defhtml-test
  (is (= "<div>hello world</div>"
         (hello "world"))))

(deftest $-test
  (testing "a simple node"
    (is (= {:node/name     :div
            :node/children [{:node/name  :dompa/text
                             :node/value "hello world"}]}
           ($ :div ($ "hello world")))))

  (testing "a fragment node"
    (is (= {:node/name     :<>
            :node/children [{:node/name     :span
                             :node/children [{:node/name  :dompa/text
                                              :node/value "hello"}]}
                            {:node/name     :span
                             :node/children [{:node/name  :dompa/text
                                              :node/value "world"}]}]}
           ($ :<>
              ($ :span
                 ($ "hello"))
              ($ :span
                 ($ "world")))))))

(deftest traverse-test
  (let [traverser-fn (fn [node]
                       (if (= :dompa/text (:node/name node))
                         (assoc node :node/value "world hello")
                         node))]
    (is (= "<div>world hello</div>"
           (-> (html/->nodes "<div>hello world</div>")
               (traverse traverser-fn)
               ->html)))))
