# Dompa

A HTML5 document parser. It takes an input of an HTML string, parses it into a node tree, and provides
an API for querying and manipulating said node tree.

## Install

```shell
npm i dompa
```

Requires Node 18.x or later.

## Usage

The most basic usage looks like this:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<div>Hello, World</div>");

// Turn the node tree back into HTML
const html = Dompa.serialize(nodes, Dompa.Serializer.Html);
```

## DOM querying

### `find`

You can find nodes with the `find` function which takes a callback function that gets `Node` passed to it and that has to return
a boolean `true` or `false`, like so:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Site Title</h1><ul><li>...</li><li>...</li></ul>")
const listItems = Dompa.find(nodes, (n) => n.name === "li");
```

All nodes returned with `find` are deep copies, so mutating them does not change the nodes in `nodes`.

## DOM manipulation

### `traverse`

The `traverse` method is very similar to the `find` method, but instead of returning deep copies of data it returns a
direct reference to data instead, meaning it is ideal for manipulating the node tree. It takes a callback function
that gets a `Node` passed to it, and has to return the updated node, like so:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Site Title</h1><ul><li>...</li><li>...</li></ul>");

const updatedNodes = Dompa.traverse(nodes, (node) => {
    if (node.name === "h1") {
        node.children = [new Dompa.TextNode("New Title")];
    }

    return node;
});
```

If you wish to remove a node then return `null` instead of the node. If you wish to replace a single node with multiple
nodes, use [`FragmentNode`](#fragmentnode). Like with `find`, all nodes returned with `traverse` are deep copies.

## Types of nodes

There are four types of nodes that you can use in Dompa to manipulate the node tree.

### `Node`

The most common node is just `Node`. You should use this if you want the node to potentially have any children inside of
it.

```typescript
import Dompa from "dompa";

new Dompa.Node({
  name: "div",
  attributes: {},
  children: [
    new Dompa.TextNode("Hello, World!")
  ]
});
```

Would render:

```html

<div>Hello, World!</div>
```

### `VoidNode`

A void node (or _Void Element_ according
to [the HTML standard](https://html.spec.whatwg.org/multipage/syntax.html#void-elements)) is self-closing, meaning you
would not have any children in it.

```typescript
import Dompa from "dompa";

new Dompa.VoidNode({
  name: "name-goes-here", 
  attributes: {}}
)
```

Would render:

```html
<name-goes-here>
```

You would use this to create things like `img`, `input`, `br` and so forth, but of course you can also create custom
elements. Dompa does not enforce the use of any known names.

### `TextNode`

A text node is just for rendering text. It has no tag of its own, it cannot have any attributes and no children.

```typescript
import Dompa from "dompa";

new Dompa.TextNode("Hello, World!")
```

Would render:

```html
Hello, World!
```

### `FragmentNode`

A fragment node is a node whose children will replace itself. It is sort of a transient node in a sense that it doesn't
really exist. You can use it to replace a single node with multiple nodes on the same level inside of the `traverse`
method.

```typescript
import Dompa from "dompa";

new Dompa.FragmentNode({
  children: [
    new Dompa.Node({
      name: "h2", 
      children: [
        new Dompa.TextNode("Hello, World!")
      ]
    }),
    
    new Dompa.Node({
      name: "p", 
      children: [
        new Dompa.TextNode("Some content ...")
      ]
    })
  ]
});
```

Would render:

```html
<h2>Hello, World!</h2>
<p>Some content ...</p>
```

## Serializers

Dompa has support for serialization - a way to transform data to whatever you want.

### `Html`

Dompa comes with a built-in serializer to transform the node tree into an HTML string.

Example usage:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Hello World</h1>")
const html = Dompa.serialize(nodes, Dompa.Serializer.Html)
```

You can also serialize specific nodes much the same way:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Hello World</h1>")
const h1 = Dompa.find(nodes, (n) => n.name === "h1")[0];
const html = Dompa.serialize([h1], Dompa.Serializer.Html)
```

### `Json`

Dompa also comes with a built-in serializer to transform the node tree into a JSON string.

Example usage:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Hello World</h1>")
const json = Dompa.serialize(nodes, Dompa.Serializer.Json)
```

You can also serialize specific nodes much the same way:

```typescript
import Dompa from "dompa";

const nodes = Dompa.nodes("<h1>Hello World</h1>")
const h1 = Dompa.find(nodes, (n) => n.name === "h1")[0];
const json = Dompa.serialize([h1], Dompa.Serializer.Json)
```

### Custom serializers

You can also create your own serializers by implementing the `Serializer` type:

```typescript
import Dompa, { Serializer } from "dompa";

const customSerializer: Serializer<T> = (nodes: Node[]): T => {
    // Your implementation here
}
```

