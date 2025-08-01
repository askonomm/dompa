# Dompa

[![codecov](https://codecov.io/gh/askonomm/dompa/branch/master/graph/badge.svg?token=resg8uSqLE)](https://codecov.io/gh/askonomm/dompa)

A lightweight HTML5 document parser for Rust. Dompa takes an HTML string as input, parses it into a node tree, and provides an API for querying, manipulating, and serializing the node tree back to HTML, or to JSON.

## Installation

Add Dompa to your `Cargo.toml`:

```toml
[dependencies]
dompa = "2.0.0"
```

## Usage

Basic usage looks like this:

```rust
use dompa;

fn main() {
    let nodes = dompa::nodes("<div>Hello, World</div>");

    /* dbg! output of nodes:
    [
        Block(
            BlockNode {
                name: "div",
                attributes: {},
                children: [
                    Text(
                        TextNode {
                            value: "Hello, World",
                        },
                    ),
                ],
            },
        ),
    ]
    */

    // Turn the node tree back into HTML
    dompa::to_html(nodes);
}
```

## Node Types

Dompa defines four types of nodes that represent HTML elements:

### `BlockNode`

Represents standard HTML elements that can contain children:

A `BlockNode` can be created like this:

```rust
use dompa::{Node, NodeAttrVal};

let block_node = Node::block("div")
    .with_attribute("class", NodeAttrVal::string("some-class"))
    .with_children(vec![
        Node::text("Hello, World")
    ]);
```

### `VoidNode`

Represents self-closing HTML elements that cannot have children. Dompa automatically treats the following tags as void nodes:

- `!doctype`
- `area`
- `base`
- `br`
- `col`
- `embed`
- `hr`
- `img`
- `input`
- `link`
- `meta`
- `source`
- `track`
- `wbr`

A `VoidNode` can be created like this:

```rust
use dompa::{Node, NodeAttrVal};

let void_node = Node::void("img")
    .with_attribute("class", NodeAttrVal::string("profile-img"));
```

### `TextNode`

Represents plain text content inside HTML elements:

A `TextNode` can be created like this:

```rust
use dompa::{Node, NodeAttrVal};

let text_node = Node::text("Hello, World!");
```

### `FragmentNode`

A special node type that allows grouping multiple nodes without creating a parent element:

A `FragmentNode` can be created like this:

```rust
use dompa::{Node, NodeAttrVal};

let fragment_node = Node::fragment()
    .with_children(vec![
        Node::text("First part "),
        Node::void("br"),
        Node::text("Second part")
    ]);
```

Essentially, a `FragmentNode` is a node which children replace itself.

## HTML Parsing and Manipulation

### `nodes` function

The `nodes` function parses an HTML string into a node tree:

```rust
let nodes = dompa::nodes("<h1>Title</h1><p>Content</p>");
```

### `traverse` function

The `traverse` function allows you to manipulate the node tree by applying a callback function to each node:

```rust
let nodes = dompa::nodes("<h1>Old Title</h1><p>Content</p>");

// Update the title text
let updated_nodes = dompa::traverse(nodes, |node| {
    match node {
        Node::Block(block) if block.name == "h1" => {
            // Create new h1 with updated text
            let mut new_block = block.clone();
            new_block.children = vec![Node::text("New Title")];
            Some(Node::Block(new_block))
        },
        _ => Some(node.clone())
    }
});
```

The callback function must return an `Option<Node>`:

- If you return `None` for a node, it will be removed from the tree
- If you return `Some(node)`, that node will be kept in the tree, whether it's the original or a replacement

Note that the callback function is called for every node in the tree, so you need to handle all cases. In most cases, you'll have specific patterns you want to match and transform, and then a catch-all case that returns the original node.

### `to_html` function

The `to_html` function serializes the node tree back to an HTML string:

```rust
let html = dompa::to_html(nodes);
```

### `to_json` function

The `to_json` function serializes the node tree into a JSON string:

```rust
let json = dompa::to_json(nodes);
```

## Working with Attributes

Attributes are stored in a `BTreeMap` with string keys. Attribute values can be either a `String` or a `True`.

```rust
use dompa::{Node, NodeAttrVal};
use std::collections::BTreeMap;

let mut attrs = BTreeMap::new();
// String attribute
attrs.insert(String::from("href"), NodeAttrVal::string("#top"));
// Boolean attribute (present without value)
attrs.insert(String::from("required"), NodeAttrVal::True);

let anchor = Node::block("a")
    .with_attributes(attrs)
    .with_children(vec![Node::text("Go to top")]);
```
