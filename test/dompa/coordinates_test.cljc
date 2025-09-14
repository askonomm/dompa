(ns dompa.coordinates-test
  (:require [clojure.test :refer [deftest testing is]]
            [dompa.coordinates :as coordinates]))

(deftest compose-test
  (testing "Create coordinates"
    (is (= [[0 4] [5 9] [10 15]]
           (coordinates/compose "<div>hello</div>"))))

  (testing "Create coordinates with invalid HTML"
    (is (= [[0 4] [5 9]]
           (coordinates/compose "<div>hello")))

    (is (= []
           (coordinates/compose "<div")))

    (is (= [[0 3]]
           (coordinates/compose "div>")))

    (is (= [[0 1]]
           (coordinates/compose "<>"))))

  (testing "Create coordinates with just text"
    (is (= [[0 4]]
           (coordinates/compose "hello"))))

  (testing "Create coordinates with text starting"
    (is (= [[0 4] [5 9] [10 15]]
           (coordinates/compose "hello<div></div>")))))

(deftest unify-test)