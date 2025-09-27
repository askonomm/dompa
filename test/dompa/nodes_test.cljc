(ns dompa.nodes-test
  (:require [clojure.test :refer [deftest is testing]]
            [dompa.nodes :refer [$]]))

(deftest node-composition-macro-test
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
