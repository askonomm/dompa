# Dompa

A _work-in-progress_ HTML5 document parser. It takes an input of an HTML string, parses it into a node tree, 
and provides an API that aims to be [Web APIs](https://developer.mozilla.org/en-US/docs/Web/API) compatible to 
modify said node tree.

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

## Queries

You can run queries on the node tree to get or manipulate node(s), as well as queries on nodes themselves, 
or their children. The Query API is trying to follow the [Web APIs](https://developer.mozilla.org/en-US/docs/Web/API) 
as closely as it can

### `after`

Not implemented yet.

### `append`

Not implemented yet.

### `before`

Not implemented yet.

### `closest`

Not implemented yet.

### `get_attribute`

Not implemented yet.

### `get_attribute_names`

Not implemented yet.

### `get_attribute_node`

Not implemented yet.

### `get_elements_by_class_name`

Not implemented yet.

### `get_elements_by_tag_name`

Not implemented yet.

### `get_html`

Not implemented yet.

### `has_attribute`

Not implemented yet.

### `has_attributes`

Not implemented yet.

### `insert_adjacent_element`

Not implemented yet.

### `insert_adjacent_html`

Not implemented yet.

### `insert_adjacent_text`

Not implemented yet.

### `matches`

Not implemented yet.

### `prepend`

Not implemented yet.

### `querySelector`

Not implemented yet.

### `querySelectorAll`

Not implemented yet.

### `remove`

Not implemented yet.

### `remove_attribute`

Not implemented yet.

### `remove_attribute_node`

Not implemented yet.

### `replace_children`

Not implemented yet.

### `replace_with`

Not implemented yet.

### `set_attribute`

Not implemented yet.

### `set_attribute_node`

Not implemented yet.

### `toggle_attribute`

Not implemented yet.


