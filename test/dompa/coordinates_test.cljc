(ns dompa.coordinates-test
  #?(:clj
     (:require
      [clojure.test :refer [deftest testing is]]
      [dompa.coordinates :as coordinates]))
  #?(:cljs
     (:require
      [cljs.test :refer-macros [deftest testing is]]
      [dompa.coordinates :as coordinates])))

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
              :coordinates [[5 9]]}
             (-> (coordinates/compose html)
                 coordinates/unify))))

    (let [html "<div>hello</span>"]
      (is (= {:html        html
              :coordinates [[5 9]]}
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
    (is (= [{:node/name     :div
             :node/attrs    {}
             :node/children [{:node/name  :dompa/text
                              :node/value "hello"}]}]
           (-> "<div>hello</div>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes))))

  (testing "Create nodes from self-closing tags"
    (is (= [{:node/name  :hr
             :node/attrs {}}]
           (-> "<hr />"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes))))

  (testing "Parse attributes with forward slashes in them"
    (is (=  [{:node/name :meta,
              :node/attrs {:name "route-pattern",
                           :content "/:user_id/:repository",
                           :data-turbo-transient true}}]
            (-> "<meta name=\"route-pattern\" content=\"/:user_id/:repository\" data-turbo-transient>"
                coordinates/compose
                coordinates/unify
                coordinates/->nodes))))

  (testing "Create nodes with attributes"
    (is (= [{:node/name     :div
             :node/attrs    {:class     "some test classes"
                             :data-attr "something"
                             :checked   true}
             :node/children [{:node/name  :dompa/text
                              :node/value "hello"}]}]
           (-> "<div class=\"some test classes\" data-attr=\"something\" checked>hello</div>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes))))

  (testing "Create nested nodes"
    (is (= [{:node/name     :b
             :node/attrs    {}
             :node/children [{:node/name  :dompa/text
                              :node/value "bold"}]}
            {:node/name  :img
             :node/attrs {:src "img.png"}}
            {:node/name  :dompa/text
             :node/value "Hello, "}
            {:node/name     :span
             :node/attrs    {}
             :node/children [{:node/name  :dompa/text
                              :node/value "wor"}
                             {:node/name     :i
                              :node/attrs    {}
                              :node/children [{:node/name  :dompa/text
                                               :node/value "l"}
                                              {:node/name     :b
                                               :node/attrs    {}
                                               :node/children [{:node/name  :dompa/text
                                                                :node/value "d"}]}]}]}]
           (-> "<b>bold</b><img src=\"img.png\" />Hello, <span>wor<i>l<b>d</b></i></span>"
               coordinates/compose
               coordinates/unify
               coordinates/->nodes)))))
