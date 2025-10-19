# Dompa

[![Tests](https://github.com/askonomm/dompa/actions/workflows/tests.yml/badge.svg)](https://github.com/askonomm/dompa/actions/workflows/tests.yml)
[![CodeScene Average Code Health](https://codescene.io/projects/72504/status-badges/average-code-health)](https://codescene.io/projects/72504)

A zero-dependency, runtime-agnostic HTML parser (and builder). 

Dompa is continuesly tested to run in the following Clojure runtimes:

- Clojure
- ClojureScript
- Babashka

## Usage

### Parsing HTML

You can use Dompa to parse an HTML string into a vector of nodes like this:

```clojure
(ns my.app
  (:require [dompa.html :as html])

(html/->nodes "<div>hello <strong>world</strong></div>")
```

Which would result in a data structure such as:

```clojure
[{:node/name :div
  :node/attrs {}
  :node/children [{:node/name :dompa/text
                   :value "hello "}
                  {:node/name :strong
                   :node/attrs {}
                   :node/children [{:node/name :dompa/text
                                    :node/value "world"}]}]}]
```

### Creating HTML

You can turn a vector of nodes, such as those above, back into a HTML string as well:

```clojure
(ns my.app
  (:require [dompa.nodes :as nodes])

(nodes/->html [...])
```

Which would then result in a HTML string.


### Traversing and modifying nodes

There's a convenience function `dompa.nodes/traverse` which helps you traverse and modify a node tree, like this:

```clojure
(ns my.app
  (:require [dompa.nodes :as nodes])

(defn update-text-value [node]
  (if (= :dompa/text (:node/name node))
    (assoc node :node/value "updated text")
    node))

(traverse [...] update-text-value)
```

The above would update all text nodes to have the value of "updated text". If you wish to keep a node unchanged, just return the node as-is, and if you wish to remove a node, return `nil`. 

### Create nodes with the `$` helper function

If you want a more convenient way of creating nodes, without having to create the full map manually, you can use the `dompa.nodes/$` helper function.

Creating a node is as simple as:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [$]])

($ :button)
```

You can also add attributes to it:


```clojure
(ns my.app
  (:require [dompa.nodes :refer [$]])

($ :button {:class "some-btn"})
```

And text nodes can be created by simply omitting the first node name keyword, like so:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [$]])

($ "hello world")
```

Which now when you put both together can look like this:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [$]])

($ :button {:class "some-btn"}
  ($ "hello world"))
```

It's important to note that when creating a text node, it cannot have any children nodes, only literal values and/or vars, whereas non-text nodes can have children as either their second argument, if they lack a attribute map, or as their third argument if they have an argument map. 

### Create HTML with the `defhtml` helper macro

If you want a more convenient way of creating functions that convert nodes into HTML on their own during compile-time, you can use the `defhtml` macro:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [defhtml $]])

(defhtml hello-page [who]
  ($ :div
    ($ "hello " who)))

(hello-page "world")
```

And of course making lists and such works the same as you'd imagine:

```clojure
(ns my.app
  (:require [dompa.nodes :refer [defhtml $]])

(def names ["john" "mike" "jenna" "bob"])

(defhtml hello-page []
  ($ :div
    (map #($ (str "hello " %)) names)))

(hello-page)
```

Do note that when using the `defhtml` macro in ClojureScript, you have to use `:refer-macros` instead of `:refer`, due to the differences in how ClojureScript deals with macros.