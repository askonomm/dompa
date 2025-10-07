(ns dompa.html-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [dompa.html :as html]))
  #?(:cljs (:require [cljs.test :refer-macros [deftest testing is]]
                     [dompa.html :as html])))

(deftest coordinates-test
  (testing "HTML to coordinates"
    (is (= {:coordinates [[0 15]
                          [5 9]]
            :html        "<div>hello</div>"}
           (html/->coordinates "<div>hello</div>")))))

(deftest nodes-test
  (testing "HTML to nodes"
    (is (= [{:node/name     :div
             :node/attrs    {}
             :node/children [{:node/name  :dompa/text
                              :node/value "hello"}]}]
           (html/->nodes "<div>hello</div>")))))
