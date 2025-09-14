(ns dompa.coordinates-test
  (:require [clojure.test :refer [deftest testing is]]
            [dompa.coordinates :as coordinates]))

(deftest compose-test
  (testing "Create first-pass coordinates"
    (is (= [[0 4] [5 9] [10 15]] (coordinates/compose "<div>hello</div>"))))

  (testing "Create first-pass coordinates with invalid HTML"
    (is (= [[0 4]] (coordinates/compose "<div>hello")))
    (is (= [] (coordinates/compose "<div"))))

  (testing "Create first-pass coordinates with just text"
    (is (= [[0 4]] (coordinates/compose "hello")))))