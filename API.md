# Table of contents
-  [`dompa.coordinates`](#dompa.coordinates) 
    -  [`->nodes`](#dompa.coordinates/->nodes) - Transform given <code>html</code> according to given <code>coordinates</code> into a tree of nodes, each representing one HTML node and its children.
    -  [`compose`](#dompa.coordinates/compose) - Composes a given <code>html</code> string into a vector of coordinates.
    -  [`unify`](#dompa.coordinates/unify) - Joins together given <code>coordinates</code> that represent one HTML node in <code>html</code>, using a stack-based approach to correctly handle nested and void tags.
-  [`dompa.html`](#dompa.html) 
    -  [`->coordinates`](#dompa.html/->coordinates) - Transform a <code>html</code> string into a vector of coordinates indicating where an HTML node ends and begins.
    -  [`->nodes`](#dompa.html/->nodes) - Transform a <code>html</code> string into a tree of nodes, each representing one HTML node and its children.
-  [`dompa.nodes`](#dompa.nodes) 
    -  [`$`](#dompa.nodes/$) - Creates a new node Usage: <code></code><code>clojure ($ :div ($ &quot;hello world&quot; )) </code><code></code>.
    -  [`->html`](#dompa.nodes/->html) - Transform a vector of <code>nodes</code> into an HTML string.
    -  [`defhtml`](#dompa.nodes/defhtml) - Creates a new function with <code>name</code> that outputs HTML.
    -  [`traverse`](#dompa.nodes/traverse) - Recursively traverses given tree of <code>nodes</code> with a <code>traverser-fn</code> that gets a single node passed to it and returns a new updated tree.

-----
# <a name="dompa.coordinates">dompa.coordinates</a>






## <a name="dompa.coordinates/->nodes">`->nodes`</a>
``` clojure

(->nodes {:keys [html coordinates]})
```
Function.

Transform given `html` according to given `coordinates` into
  a tree of nodes, each representing one HTML node and its children.

  Direct output of both [`compose`](#dompa.coordinates/compose) and [`unify`](#dompa.coordinates/unify) can be given to this
  function, allowing chaining such as:

  ```clojure
  (-> "some html ..."
      coordinates/compose
      coordinates/unify
      coordinates/->nodes)
  ```
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/coordinates.cljc#L330-L353">Source</a></sub></p>

## <a name="dompa.coordinates/compose">`compose`</a>
``` clojure

(compose html)
```
Function.

Composes a given `html` string into a vector of coordinates.
  These are single-pass coordinates without awareness of context,
  thus HTML such as:

  ```html
  <div>hello</div>
  ```

  will return 3 coordinates (div, text, div) instead of 2 (div, text).
  To unify the coordinates in a context-aware way, you pass the result
  of this function to the [`unify`](#dompa.coordinates/unify) function.
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/coordinates.cljc#L65-L85">Source</a></sub></p>

## <a name="dompa.coordinates/unify">`unify`</a>
``` clojure

(unify {:keys [html coordinates]})
```
Function.

Joins together given `coordinates` that represent
  one HTML node in `html`, using a stack-based approach to correctly
  handle nested and void tags.
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/coordinates.cljc#L159-L167">Source</a></sub></p>

-----
# <a name="dompa.html">dompa.html</a>






## <a name="dompa.html/->coordinates">`->coordinates`</a>
``` clojure

(->coordinates html)
```
Function.

Transform a `html` string into a vector of coordinates
  indicating where an HTML node ends and begins.
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/html.cljc#L5-L11">Source</a></sub></p>

## <a name="dompa.html/->nodes">`->nodes`</a>
``` clojure

(->nodes html)
```
Function.

Transform a `html` string into a tree of nodes,
  each representing one HTML node and its children.
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/html.cljc#L13-L19">Source</a></sub></p>

-----
# <a name="dompa.nodes">dompa.nodes</a>






## <a name="dompa.nodes/$">`$`</a>
``` clojure

($ name & opts)
```
Function.

Creates a new node
  
  Usage:

  ```clojure
  ($ :div
    ($ "hello world" ))
  ```
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/nodes.cljc#L97-L117">Source</a></sub></p>

## <a name="dompa.nodes/->html">`->html`</a>
``` clojure

(->html nodes)
(->html nodes {:keys [void-nodes]})
```
Function.

Transform a vector of `nodes` into an HTML string.

  Options:
  - `void-nodes` - A set of node names that are self-closing, defaults to:
    - `:!doctype`
    - `:area`
    - `:base`
    - `:br`
    - `:col`
    - `:embed`
    - `:hr`
    - `:img`
    - `:input`
    - `:link`
    - `:meta`
    - `:source`
    - `:track`
    - `:wbr`
  
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/nodes.cljc#L53-L77">Source</a></sub></p>

## <a name="dompa.nodes/defhtml">`defhtml`</a>
``` clojure

(defhtml name & args-and-elements)
```
Macro.

Creates a new function with `name` that outputs HTML.

  Example usage:

  ```clojure
  (defhtml about-page [who]
    ($ :div
      ($ "hello " who)))

  (about-page "world")
  ```
  
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/nodes.cljc#L79-L95">Source</a></sub></p>

## <a name="dompa.nodes/traverse">`traverse`</a>
``` clojure

(traverse nodes traverser-fn)
```
Function.

Recursively traverses given tree of `nodes` with a `traverser-fn`
  that gets a single node passed to it and returns a new updated tree.
  If the traverses function returns `nil`, the node will be removed.
  In any other case the node will be replaced. If you wish to keep
  a node unchanged, just return it as-is.
<p><sub><a href="https://github.com/askonomm/dompa/blob/main/src/dompa/nodes.cljc#L39-L51">Source</a></sub></p>
