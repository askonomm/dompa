(ns dompa.templates-test
  (:require [clojure.test :refer [deftest is testing]]
            [dompa.templates :refer [defhtml $]]))

(deftest node-composition-test
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
