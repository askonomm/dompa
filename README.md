# Dompa

![Coverage](https://raw.githubusercontent.com/askonomm/dompa/refs/heads/master/coverage-badge.svg)

A _work-in-progress_ HTML5 document parser. It takes an input of an HTML string, parses it into a node tree,
and provides an API for querying and manipulating the node tree.

## Install

```shell
pip install dompa
```

Requires Python 3.10 or higher.

## Usage

The most basic usage looks like this:

```python
from dompa import Dompa

dom = Dompa("<div>Hello, World</div>")

# Get the tree of nodes
nodes = dom.nodes()

# Get the HTML string
html = dom.html()
```

## DOM manipulation

You can run queries on the node tree to get or manipulate node(s).

### `query`

You can find nodes with the `query` method which takes a `Callable` that gets `Node` passed to it and that has to return
a boolean `true` or `false`, like so:

```python
from dompa import Dompa

dom = Dompa("<h1>Site Title</h1><ul><li>...</li><li>...</li></ul>")
list_items = dom.query(lambda n: n.name == "li")
```

All nodes returned with `query` are deep copies, so mutating them has no effect on Dompa's state.

### `traverse`

The `traverse` method is very similar to the `query` method, but instead of returning deep copies of data it returns a
direct reference to data instead, meaning it is ideal for updating the node tree inside of Dompa. It takes a `Callable`
that gets a `Node` passed to it, and has to
return the updated node, like so:

```python
from typing import Optional
from dompa import Dompa
from dompa.nodes import Node, TextNode

dom = Dompa("<h1>Site Title</h1><ul><li>...</li><li>...</li></ul>")


def update_title(node: Node) -> Optional[Node]:
    if node.name == "h1":
        node.children = [TextNode(value="New Title")]

    return node


dom.traverse(update_title)
```

If you wish to remove a node then return `None` instead of the node.

## Types of nodes

There are three types of nodes that you can use in Dompa to manipulate the node tree.

### `Node`

The most common node is just `Node`. You should use this if you want the node to potentially have any children inside of
it.

```python
from dompa.nodes import Node

Node(name="name-goes-here", attributes={}, children=[])
```

Would render:

```html
<name-goes-here></name-goes-here>
```

### `VoidNode`

A void node (or _Void Element_ according
to [the HTML standard](https://html.spec.whatwg.org/multipage/syntax.html#void-elements)) is self-closing, meaning you
would not have any children in it.

```python
from dompa.nodes import VoidNode

VoidNode(name="name-goes-here", attributes={})
```

Would render:

```html
<name-goes-here>
```

You would use this to create things like `img`, `input`, `br` and so forth, but of course you can also create custom
elements. Dompa does not enforce the use of any known names.

### `TextNode`

A text node is just for rendering text. It has no tag of its own, it cannot have any attributes and no children.

```python
from dompa.nodes import TextNode

TextNode(value="Hello, World!")
```

Would render:

```html
Hello, World!
```