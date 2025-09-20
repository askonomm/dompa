(ns dompa.coordinates-test
  (:require [clojure.test :refer [deftest testing is]]
            [dompa.coordinates :as coordinates]))

(deftest compose-test
  (testing "Create coordinates"
    (let [html "<div>hello</div>"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9] [10 15]]}
             (coordinates/compose html)))))

  (testing "Create coordinates with invalid HTML"
    (let [html "<div>hello"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9]]}
             (coordinates/compose html))))

    (let [html "<div>hello</span>"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9] [10 16]]}
             (coordinates/compose html))))

    (let [html "<div"]
      (is (= {:html        html
              :coordinates []}
             (coordinates/compose html))))

    (let [html "div>"]
      (is (= {:html        html
              :coordinates [[0 3]]}
             (coordinates/compose html))))

    (let [html "<>"]
      (is (= {:html        html
              :coordinates [[0 1]]}
             (coordinates/compose html)))))

  (testing "Create coordinates with just text"
    (let [html "hello"]
      (is (= {:html        html
              :coordinates [[0 4]]}
             (coordinates/compose html)))))

  (testing "Create coordinates with text starting"
    (let [html "hello<div></div>"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9] [10 15]]}
             (coordinates/compose html)))))

  (testing "Create coordinates with text ending"
    (let [html "<div></div>hello"]
      (is (= {:html        html
              :coordinates [[0 4] [5 10] [11 15]]}
             (coordinates/compose html))))))

(deftest unify-test
  (testing "Unify coordinates"
    (let [html "<div>hello</div>"]
      (is (= {:html        html
              :coordinates [[0 15] [5 9]]}
             (-> (coordinates/compose html)
                 coordinates/unify)))))

  (testing "Unify coordinates with invalid HTML"
    (let [html "<div>hello"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9]]}
             (-> (coordinates/compose html)
                 coordinates/unify))))

    (let [html "<div>hello</span>"]
      (is (= {:html        html
              :coordinates [[0 4] [5 9]]}
             (-> (coordinates/compose html)
                 coordinates/unify)))))

  (testing "Unify coordinates with just text"
    (let [html "hello"]
      (is (= {:html        html
              :coordinates [[0 4]]}
             (-> (coordinates/compose html)
                 coordinates/unify)))))

  (testing "Unify coordinates with text starting"
    (let [html "hello<div></div>"]
      (is (= {:html        html
              :coordinates [[0 4] [5 15]]}
             (-> (coordinates/compose html)
                 coordinates/unify)))))

  (testing "Unify coordinates with text ending"
    (let [html "<div></div>hello"]
      (is (= {:html        html
              :coordinates [[0 10] [11 15]]}
             (-> (coordinates/compose html)
                 coordinates/unify))))))

(deftest nodes-test
  (testing "Create nodes"
    (is (= [{:name     :div
             :attrs    {}
             :children [{:name  :dompa/text
                         :value "hello"}]}]
           (-> "<div>hello</div>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes))))

  (testing "Create nodes with attributes"
    (is (= [{:name     :div
             :attrs    {:class     "some test classes"
                        :data-attr "something"
                        :checked   true}
             :children [{:name  :dompa/text
                         :value "hello"}]}]
           (-> "<div class=\"some test classes\" data-attr=\"something\" checked>hello</div>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes))))

  (testing "Create nested nodes"
    (is (= [{:name     :b
             :attrs    {}
             :children [{:name  :dompa/text
                         :value "bold"}]}
            {:name  :img
             :attrs {:src "img.png"}}
            {:name  :dompa/text
             :value "Hello, "}
            {:name     :span
             :attrs    {}
             :children [{:name  :dompa/text
                         :value "wor"}
                        {:name     :i
                         :attrs    {}
                         :children [{:name  :dompa/text
                                     :value "l"}
                                    {:name     :b
                                     :attrs    {}
                                     :children [{:name  :dompa/text
                                                 :value "d"}]}]}]}]
           (-> "<b>bold</b><img src=\"img.png\" />Hello, <span>wor<i>l<b>d</b></i></span>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes)))))