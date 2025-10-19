(ns dompa.round-trip-test
  (:require [clojure.test :refer [deftest is testing]]
            [dompa.nodes :as nodes]
            [dompa.html :as html]))

(deftest round-trip-test
  (testing "michiel borkent website"
    (let [input-html (slurp "test/dompa/html/michiel_borkent.html")
          nodes      (html/->nodes input-html)
          output-html (nodes/->html nodes)]
      (is (= input-html output-html)))))