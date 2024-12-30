# Dompa

![Coverage](https://raw.githubusercontent.com/askonomm/dompa/refs/heads/master/coverage-badge.svg)

A _work-in-progress_ HTML5 document parser. It takes an input of an HTML string, parses it into a node tree, 
and provides an API for querying and manipulating the node tree.

## Install

```shell
pip install dompa
```

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

### `find`

You can find nodes with the `find` method which takes a `Callable` that gets `Node` passed to it and that has to return 
a boolean `true` or `false`, like so:

```python
from dompa import Dompa

dom = Dompa("<h1>Site Title</h1><ul><li>...</li><li>...</li></ul>")
list_items = dom.find(lambda n: n.name == "li")
```

All nodes returned with `find` are deep copies, so mutating them has no effect on Dompa's state.

### `update`

You can update nodes with the `update` method which takes a `Callable` that gets a `Node` passed to it, and has to 
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

dom.update(update_title)
```

If you wish to remove a node then return `None` instead of the node.
