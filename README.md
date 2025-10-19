# Dompa

[![Tests](https://github.com/askonomm/dompa/actions/workflows/tests.yml/badge.svg)](https://github.com/askonomm/dompa/actions/workflows/tests.yml)
[![CodeScene Average Code Health](https://codescene.io/projects/72504/status-badges/average-code-health)](https://codescene.io/projects/72504)

A zero-dependency, runtime-agnostic HTML parser and builder for Clojure.

Dompa aims to be a universal Clojure library, tested across:

  * Clojure
  * ClojureScript
  * Babashka

-----

## 🚀 Installation

Add Dompa to your `deps.edn` directly from GitHub:

```clojure
{:deps {askonomm/dompa {:git/url "https://github.com/askonomm/dompa"
                        :git/tag "v1.0.3"
                        :git/sha "3ef74e6a0cfd3869aaa6859750024ee5c9b5ea8a"}}}
```

-----

## ✨ Usage

### Parsing and Creating HTML

Dompa makes it simple to convert HTML strings into Clojure data structures and back.

**1. Parse HTML to Nodes**

Use `dompa.html/->nodes` to parse an HTML string into a vector of nodes.

```clojure
(ns my.app
  (:require [dompa.html :as html]))

(html/->nodes "<div>hello <strong>world</strong></div>")
```

This produces a nested data structure:

```clojure
[{:node/name :div
  :node/attrs {}
  :node/children [{:node/name :dompa/text
                   :node/value "hello "}
                  {:node/name :strong
                   :node/attrs {}
                   :node/children [{:node/name :dompa/text
                                    :node/value "world"}]}]}]
```

**2. Create HTML from Nodes**

Use `dompa.nodes/->html` to convert the node structure back into an HTML string.

```clojure
(ns my.app
  (:require [dompa.nodes :as nodes]))

;; ...using the nodes from the previous example
(nodes/->html [...])
;;=> "<div>hello <strong>world</strong></div>"
```

-----

### Traversing and Modifying Nodes

Easily walk and transform the node tree with the `dompa.nodes/traverse` helper.

```clojure
(ns my.app
  (:require [dompa.nodes :refer [traverse]])

(def nodes-data [...]) ; Your node structure

(defn update-text-value [node]
  (if (= :dompa/text (:node/name node))
    (assoc node :node/value "updated text")
    node))

(traverse nodes-data update-text-value)
```

> The function you provide to `traverse` dictates the outcome for each node:
>
>   * To **update a node**, return the modified node.
>   * To **keep a node unchanged**, return the original node.
>   * To **remove a node**, return `nil`.

-----

### 🛠️ Building Nodes with the `$` Helper

For a more idiomatic and concise way to build node structures, use the `$` helper from `dompa.nodes`.

```clojure
(ns my.app
  (:require [dompa.nodes :refer [$]]))

;; A simple node
($ :button)

;; A node with attributes
($ :button {:class "some-btn"})

;; A text node
($ "hello world")

;; Put it all together
($ :button {:class "some-btn"}
  ($ "hello world"))
```

Nodes can be nested. Children are passed as the second argument (if no attributes) or the third argument (if attributes are present).

-----

### ⚡️ Compile-Time HTML with `defhtml`

The `defhtml` macro creates functions that build and render HTML at compile time for maximum performance.

```clojure
(ns my.app
  (:require [dompa.nodes :refer [defhtml $]]))

(defhtml hello-page [who]
  ($ :div
    ($ "hello " who)))

(hello-page "world")
;;=> "<div>hello world</div>"
```

It works seamlessly with standard Clojure functions like `map`:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [defhtml $]]))

(def names ["john" "mike" "jenna"])

(defhtml name-list []
  ($ :ul
    (map #($ :li %) names)))

(name-list)
;;=> "<ul><li>john</li><li>mike</li><li>jenna</li></ul>"
```

> **Note for ClojureScript:** Remember to use `:refer-macros` instead of `:refer` when requiring `defhtml`.

-----

### ⚙️ Advanced: Lower-Level API

Dompa also exposes the lower-level functions that power the parsing process. You can use these for more granular control:

  * `dompa.html/->coordinates`: Transforms an HTML string into coordinate data.
  * `dompa.coordinates/compose`: Creates range positions of nodes from an HTML string.
  * `dompa.coordinates/unify`: Merges coordinates for the same block nodes.
  * `dompa.coordinates/->nodes`: Transforms coordinate data into a final node tree.
