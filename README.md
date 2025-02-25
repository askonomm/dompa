# Dompa

A lightweight, zero-dependency HTML5 document parser for Rust. Dompa takes an HTML string as input, parses it into a node tree, and provides an API for querying, manipulating, and serializing the node tree back to HTML.

## Installation

Add Dompa to your `Cargo.toml`:

```toml
[dependencies]
dompa = "1.0.2"
```

## Usage

Basic usage looks like this:

```rust
use dompa;

fn main() {
    let html = String::from("<div>Hello, World</div>");
    let nodes = dompa::nodes(html);
    
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
    let html_output = dompa::to_html(nodes);
}
```

## Node Types

Dompa defines four types of nodes that represent HTML elements:

### `BlockNode`

Represents standard HTML elements that can contain children:

A `BlockNode` can be created either by using the full, verbose way, such as:

```rust
use dompa::{Node, BlockNode};
use std::collections::HashMap;

let block_node = Node::Block(BlockNode {
  name: String::from("div"),
  attributes: HashMap::new(),
  children: Vec::new()
});
```

Or you can use a shorthand helper, like so:

```rust
let block_node = Node::block("div", HashMap::new(), vec![]);
```

But if you don't care about manually adding attributes then you can use an even shorter shorthand helper, like so:

```rust
let block_node = Node::simple_block("div", vec![]);
```

Example with content:

```rust
use dompa::Node;
use std::collections::HashMap;

// Create a div with text content
let div = Node::simple_block("div", vec![
    Node::text("Hello, World!")
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

A `VoidNode` can be created either by using the full, verbose way, such as:

```rust
use dompa::{Node, VoidNode};
use std::collections::HashMap;

let void_node = Node::Void(VoidNode {
    name: String::from("img"),
    attributes: HashMap::new()
});
```

Or you can use a shorthand helper, like so:

```rust
let void_node = Node::void("img", HashMap::new());
```

But if you don't care about manually adding attributes then you can use an even shorter shorthand helper, like so:

```rust
let void_node = Node::simple_void("img");
```

Example with attributes:

```rust
use dompa::{Node, NodeAttributeValue};
use std::collections::HashMap;

// Create an img element with attributes
let mut attrs = HashMap::new();
attrs.insert(String::from("src"), NodeAttributeValue::string("/images/logo.png"));
attrs.insert(String::from("alt"), NodeAttributeValue::string("Logo"));

let img = Node::void("img", attrs);
```

### `TextNode`

Represents plain text content inside HTML elements:

A `TextNode` can be created either by using the full, verbose way, such as:

```rust
use dompa::{Node, TextNode};

let text_node = Node::Text(TextNode {
    value: String::from("Hello, World!")
});
```

Or you can use a shorthand helper, like so:

```rust
use dompa::Node;

let text_node = Node::text("Hello, World!");
```

### `FragmentNode`

A special node type that allows grouping multiple nodes without creating a parent element:

A `FragmentNode` can be created either by using the full, verbose way, such as:

```rust
use dompa::{Node, FragmentNode};

let fragment_node = Node::Fragment(FragmentNode {
    children: vec![
        Node::text("First part "),
        Node::simple_void("br"),
        Node::text("Second part")
    ]
});
```

Or you can use a shorthand helper, like so:

```rust
use dompa::Node;

let fragment_node = Node::fragment(vec![
    Node::text("First part "),
    Node::simple_void("br"),
    Node::text("Second part")
]);
```

Essentially, a `FragmentNode` is a node which children replace itself.

## HTML Parsing and Manipulation

### `nodes` function

The `nodes` function parses an HTML string into a node tree:

```rust
let html = String::from("<h1>Title</h1><p>Content</p>");
let nodes = dompa::nodes(html);
```

### `traverse` function

The `traverse` function allows you to manipulate the node tree by applying a callback function to each node:

```rust
let html = String::from("<h1>Old Title</h1><p>Content</p>");
let nodes = dompa::nodes(html);

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
- For nodes you don't want to modify, you must return the original node wrapped in `Some()` (typically `Some(node.clone())`)
- For nodes you want to modify, return a new or updated node wrapped in `Some()`

Note that the callback function is called for every node in the tree, so you need to handle all cases. In most cases, you'll have specific patterns you want to match and transform, and then a catch-all case that returns the original node.

### `to_html` function

The `to_html` function serializes the node tree back to an HTML string:

```rust
let html = dompa::to_html(nodes);
```

Note that since the attributes are stored in a `HashMap`, their order is not guaranteed to be the same as in your HTML. However, to not have
unpredictable results, Dompa sorts the attributes alphabetically in the output.

## Working with Attributes

Attributes are stored in a `HashMap` with string keys. Attribute values can be either `String` values or a boolean `true`:

```rust
use dompa::{Node, NodeAttributeValue};
use std::collections::HashMap;

let mut attrs = HashMap::new();
// String attribute
attrs.insert(String::from("href"), NodeAttributeValue::string("#top"));
// Boolean attribute (present without value)
attrs.insert(String::from("required"), NodeAttributeValue::True);

let anchor = Node::block("a", attrs, vec![Node::text("Go to top")]);
```

Dompa provides a convenient helper method for string attributes:

```rust
// Instead of:
NodeAttributeValue::String(String::from("value"))

// You can use:
NodeAttributeValue::string("value")
```
