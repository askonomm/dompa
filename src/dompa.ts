import { Effect, Ref, Context, Option } from "effect";

/**
 * State for holding the HTML string.
 */
class HtmlState extends Context.Tag("HtmlState")<
  HtmlState,
  Ref.Ref<string>
>() {}

/**
 * -----------------------------------------------------------------
 * IR Node
 * -----------------------------------------------------------------
 */
type IrNodeProps = {
  name: string;
  coords: [number, number];
  children: IrNode[];
};

/**
 * An IR Node (intermediate representation node) is a node that collects
 * crude information such as the node name, and its coords. Based on that
 * information we also give it children, if it has any, based on coords
 */
class IrNode {
  name: string;
  coords: [number, number];
  children: IrNode[];

  constructor({ name, coords, children }: IrNodeProps) {
    this.name = name;
    this.coords = coords;
    this.children = children;
  }
}

class IrNodesState extends Context.Tag("IrNodesState")<
  IrNodesState,
  Ref.Ref<IrNode[]>
>() {}

/**
 * -----------------------------------------------------------------
 * Node
 * -----------------------------------------------------------------
 */
type Attrs = Record<string, string | true>;

type NodeProps = {
  name: string;
  attributes: Attrs;
  children: Node[];
};

/**
 * A Node is a generic HTML element that has a name, attributes
 * and children.
 */
class Node {
  name: string;
  attributes: Attrs;
  children: Node[];

  constructor({ name, attributes, children }: NodeProps) {
    this.name = name;
    this.attributes = attributes;
    this.children = children;
  }
}

/**
 * A TextNode is a Node that only contains plain text.
 */
class TextNode extends Node {
  value: string;

  constructor(value: string) {
    super({ name: "_text_node", attributes: {}, children: [] });
    this.value = value;
  }
}

/**
 * A VoidNode is a Node that is self-closing, and has no children.
 */
class VoidNode extends Node {}

/**
 * A FragmentNode is a Node which children replace itself.
 * This is useful if you want to replace the current node in the
 * traversal iteration with multiple nodes.
 */
class FragmentNode extends Node {}

/**
 * State for holding the nodes.
 */
class NodesState extends Context.Tag("NodesState")<
  NodesState,
  Ref.Ref<Node[]>
>() {}

/**
 * -----------------------------------------------------------------
 * Core logic
 * -----------------------------------------------------------------
 */
const voidNames = [
  "!doctype",
  "area",
  "base",
  "br",
  "col",
  "embed",
  "hr",
  "img",
  "input",
  "link",
  "meta",
  "source",
  "track",
  "wbr",
];

/**
 * An Effect that sets the end coord of the last IR Node with given `name`
 * to `coord`, but only on a last IR node which has its end coord 0, as in
 * unset.
 */
const setEndCoordOfLastIrNodeEffect = (name: string, coord: number) =>
  Effect.gen(function* () {
    const irNodesState = yield* IrNodesState;
    const irNodesValue = yield* Ref.get(irNodesState);

    //yield* Effect.fail(new Error(":)"));

    if (irNodesValue.length === 0) {
      yield* Effect.fail(new Error("No IrNode's found."));
    }

    const index = irNodesValue.findLastIndex(
      (n) => n.name === name && n.coords[1] === 0,
    );

    if (index === -1) {
      yield* Effect.fail(new Error("Could not find matching IrNode."));
    }

    yield* Ref.update(irNodesState, (irNodes) => {
      irNodes[index].coords = [irNodes[index].coords[0], coord];

      return irNodes;
    });

    yield* Effect.succeed(true);
  });

/**
 * An Effect that closes the last IR node matching `tag` with given
 * `coord` by setting it as an end coord.
 */
const closeIrNodeEffect = (tag: string, coord: number) =>
  Effect.gen(function* () {
    const name = tag
      .substring(2, tag.length - 1)
      .split(" ")[0]
      .trim();

    yield* setEndCoordOfLastIrNodeEffect(name, coord);
  });

/**
 * An Effect which creates IR nodes.
 */
const createIrNodesEffect = Effect.gen(function* () {
  const html = yield* Ref.get(yield* HtmlState);
  const irNodesState = yield* IrNodesState;
  let tagStart: number | null = null;
  let tagEnd: number | null = null;
  let textStart: number | null = null;
  let textEnd: number | null = null;

  for (let i = 0; i < html.length; i++) {
    const char = html.at(i);

    // Start of a tag, or end of a text node
    if (char === "<") {
      if (textStart !== null) {
        textEnd = i;
      }

      tagStart = i;
    }

    // If we're at the last char, and it's not the end of a tag,
    // means we're in a text node
    if (i === html.length - 1 && char !== ">") {
      textEnd = i + 1;
    }

    // End of a tag
    if (char === ">") {
      tagEnd = i + 1;
    }

    // When we have both tagStart and tagEnd, collect
    if (tagStart !== null && tagEnd !== null) {
      const tag = html.substring(tagStart, tagEnd);

      // If this is a closing tag, close the last node
      if (tag.startsWith("</")) {
        yield* closeIrNodeEffect(tag, tagEnd);
        tagStart = null;
        tagEnd = null;
        continue;
      }

      const name = tag
        .substring(1, tag.length - 1)
        .split(" ")[0]
        .trim();

      // If this is a void node, we're already at the end.
      if (voidNames.includes(name.toLowerCase())) {
        const newIrNode = new IrNode({
          name,
          coords: [tagStart, tagEnd],
          children: [],
        });

        yield* Ref.update(irNodesState, (irNodes) => {
          return [...irNodes, newIrNode];
        });
      } else {
        const newIrNode = new IrNode({
          name,
          coords: [tagStart, 0],
          children: [],
        });

        yield* Ref.update(irNodesState, (irNodes) => {
          return [...irNodes, newIrNode];
        });
      }

      tagStart = null;
      tagEnd = null;
      continue;
    }

    // No collection data, and no `textStart`, means we start
    // collecting text data
    if (tagStart === null && tagEnd === null && textStart === null) {
      textStart = i;
    }

    // If we have all text data, collect
    if (textStart !== null && textEnd !== null) {
      const newIrNode = new IrNode({
        name: "_text_node",
        coords: [textStart, textEnd],
        children: [],
      });

      yield* Ref.update(irNodesState, (irNodes) => {
        return [...irNodes, newIrNode];
      });

      textStart = null;
      textEnd = null;
    }
  }
});

/**
 * For given `irNodes`, finds all IR nodes within the given `coords`,
 * and returns back its index in state, as well as the object itself.
 */
const findIrNodesInCoords = (
  irNodes: IrNode[],
  coords: [number, number],
): [number, IrNode][] => {
  return irNodes.reduce(
    (nodes: [number, IrNode][], node: IrNode, i: number) => {
      if (node.coords[0] > coords[0] && node.coords[1] < coords[1]) {
        nodes.push([i, node]);
      }

      return nodes;
    },
    [],
  );
};

/**
 * An Effect which joins IR Nodes together - meaning that
 * it turns a flat list of IR Nodes into a tree of IR Nodes,
 * depending on if they have a parent or not, according to their coords.
 */
const joinIrNodesEffect = Effect.gen(function* () {
  const irNodesState = yield* IrNodesState;

  const joinIrNodes = (irNodes: IrNode[], added: Set<string>): IrNode[] => {
    return irNodes.reduce((iterNodes: IrNode[], node: IrNode) => {
      const nodeCoordsKey = JSON.stringify(node.coords);

      if (added.has(nodeCoordsKey)) return iterNodes;
      added.add(nodeCoordsKey);

      const iterNodeChildrenList = findIrNodesInCoords(irNodes, node.coords);
      const iterNodeChildrenNodes = iterNodeChildrenList.map((n) => n[1]);

      node.children = joinIrNodes(iterNodeChildrenNodes, added);
      iterNodes.push(node);

      return iterNodes;
    }, []);
  };

  yield* Ref.update(irNodesState, (irNodes) => {
    return joinIrNodes(irNodes, new Set<string>());
  });
});

/**
 * From given `html` and `coords` tries to return the attribute
 * part of a string. So for example if the coordinates point to a
 * string such as:
 *
 * ```html
 * <div class="some-class"></div>
 * ```
 *
 * Then it will attempt to find and return this:
 *
 * ```html
 * class="some-class"
 * ```
 */
const nodeAttrsStrFromCoords = (
  html: string,
  coords: [number, number],
): Option.Option<string> => {
  const nodeStr = html.substring(coords[0], coords[1]);
  let attrsStrStart: Option.Option<number> = Option.none();
  let attrsStrEnd: Option.Option<number> = Option.none();

  for (let i = 0; i < nodeStr.length; i++) {
    const char = nodeStr.at(i);

    if (char === ">") {
      attrsStrEnd = Option.some(i);
      break;
    }

    if (attrsStrStart === null && char === " ") {
      attrsStrStart = Option.some(i + 1);
    }
  }

  if (Option.isNone(attrsStrStart) || Option.isNone(attrsStrEnd)) {
    return Option.none();
  }

  return Option.some(nodeStr.substring(attrsStrStart.value, attrsStrEnd.value));
};

/**
 * For given `html` and `coords` tries to return the Attrs object. For example
 * if the coordinates point to a string such as:
 *
 * ```html
 * <div class="some-class"></div>
 * ```
 *
 * Then it will attempt to find and return this object:
 *
 * ```typescript
 * {
 *   class: "some-class"
 * }
 * ```
 */
const nodeAttrsFromCoords = (html: string, coords: [number, number]): Attrs => {
  let attributes: Attrs = {};
  const attrsStr = nodeAttrsStrFromCoords(html, coords);

  if (Option.isNone(attrsStr)) {
    return attributes;
  }

  let iterAttrName = "";
  let iterAttrValue: string | null = null;

  for (let i = 0; i < attrsStr.value.length; i++) {
    const char = attrsStr.value.at(i);

    // if we encounter a space, and the last char of `iterAttrValue` is `"`
    // it means we're not in an attr value, in which case a
    // space would be part of the value, but rather ending an attribute
    // declaration and moving onto the next one.
    if (
      char === " " &&
      iterAttrValue !== null &&
      iterAttrValue?.at(-1) !== '"'
    ) {
      if (iterAttrValue?.at(0) === '"' && iterAttrValue?.at(-1) === '"') {
        iterAttrValue = iterAttrValue?.substring(1, iterAttrValue?.length - 1);
      }

      attributes[`${iterAttrName}${char}`.trim()] = iterAttrValue;
      iterAttrName = "";
      iterAttrValue = null;
      continue;
    }

    // same as above is true when we are the last char of the entire `attrStr`,
    // in which case we are ending an attribute declaration.
    if (i === attrsStr.value.length - 1 && iterAttrValue !== null) {
      iterAttrValue += char;

      if (iterAttrValue.at(0) === '"' && iterAttrValue.at(-1) === '"') {
        iterAttrValue = iterAttrValue.substring(1, iterAttrValue.length - 1);
      }

      attributes[`${iterAttrName}`.trim()] = iterAttrValue;
      iterAttrName = "";
      iterAttrValue = null;
      continue;
    }

    // and, same as above is also true when we encounter a space and there is
    // no `iterAttrValue`, meaning it is a Truthy attribute, which needs
    // no explicit value.
    if (
      (char === " " || i === attrsStr.value.length - 1) &&
      iterAttrValue === null
    ) {
      attributes[`${iterAttrName}${char}`.trim()] = true;
      iterAttrName = "";
      iterAttrValue = null;
      continue;
    }

    // If we encounter the `=` char, it means we are done with `iterAttrName`,
    // and can move on to start creating the `iterAttrValue`.
    if (iterAttrValue === null && char === "=") {
      iterAttrValue = "";
      continue;
    }

    // in all other cases if we have already set `iterAttrValue`, keep on
    // collecting it.
    if (iterAttrValue !== null) {
      iterAttrValue += char;
      continue;
    }

    // or if we have not set `iterAttrValue`, keep on collecting `iterAttrName`.
    iterAttrName += char;
  }

  return attributes;
};

/**
 * Transforms an `irNode` to a `Node`.
 */
const irNodeToNode = (html: string, irNode: IrNode): Node => {
  if (irNode.name === "_text_node") {
    return new TextNode(html.substring(irNode.coords[0], irNode.coords[1]));
  }

  if (voidNames.includes(irNode.name.toLowerCase())) {
    return new VoidNode({
      name: irNode.name,
      attributes: nodeAttrsFromCoords(html, irNode.coords),
      children: [],
    });
  }

  return new Node({
    name: irNode.name,
    attributes: nodeAttrsFromCoords(html, irNode.coords),
    children: [],
  });
};

/**
 * An Effect which creates Node's from IrNode's. Essentially
 * turning crude oil into refined oil.
 */
const createNodesEffect = Effect.gen(function* () {
  const irNodesState = yield* IrNodesState;
  const nodesState = yield* NodesState;
  const html = yield* Ref.get(yield* HtmlState);

  const createNodes = (irNodes: IrNode[]): Node[] => {
    return irNodes.reduce((nodes: Node[], irNode: IrNode) => {
      if (!irNode.children.length) {
        nodes.push(irNodeToNode(html, irNode));
      } else {
        const node = irNodeToNode(html, irNode);
        node.children = createNodes(irNode.children);
        nodes.push(node);
      }

      return nodes;
    }, []);
  };

  yield* Ref.set(nodesState, createNodes(yield* Ref.get(irNodesState)));
});

/**
 * An Effect which puts all the various effects for composing nodes
 * together to then return the final result as a Node tree.
 */
const composeNodesEffect = Effect.gen(function* () {
  yield* createIrNodesEffect;
  yield* joinIrNodesEffect;
  yield* createNodesEffect;

  return yield* Ref.get(yield* NodesState);
});

/**
 * Transform the given `html` string into a tree of Node's.
 */
const nodes = (html: string) => {
  return Effect.runSync(
    composeNodesEffect.pipe(
      Effect.provideServiceEffect(HtmlState, Ref.make(html)),
      Effect.provideServiceEffect(IrNodesState, Ref.make<IrNode[]>([])),
      Effect.provideServiceEffect(NodesState, Ref.make<Node[]>([])),
    ),
  );
};

/**
 * Traverses the given tree of `nodes` for the purposes of manipulating
 * said tree of `nodes`. It takes a predicate function `pred` which gets
 * a single Node passed to it during traversal and which must return a
 * `Node` or `null`.
 *
 * If you return the same exact node, it will change nothing, but if you return
 * an updated node, it will update, and if you return a completely new node,
 * it will replace it. Returning `null` will remove the node from the tree.
 *
 * ---
 *
 * ### Types of nodes:
 *
 * - `Node` - generic node
 * - `VoidNode` - a self-closing node (has no children)
 * - `TextNode` - a node containing only plain text
 * - `FragmentNode` - a node which children replace itself
 */
const traverse = (nodes: Node[], pred: (node: Node) => Node | null): Node[] => {
  return nodes.reduce((updatedNodes: Node[], node: Node) => {
    const updatedNode = pred(node);

    // If the callback returns `null`, it means we want to remove the node.
    if (updatedNode === null) {
      return updatedNodes;
    }

    // If the callback returns a `TextNode`, we don't need to traverse its children.
    if (updatedNode instanceof TextNode) {
      updatedNodes.push(updatedNode);

      return updatedNodes;
    }

    // If the callback returns a `FragmentNode`, we need to traverse its children,
    // as it's children will replace the `FragmentNode` itself.
    if (updatedNode instanceof FragmentNode) {
      updatedNodes.push(...traverse(updatedNode.children, pred));

      return updatedNodes;
    }

    // If the callback returns a `VoidNode`, we don't need to traverse its children.
    if (updatedNode instanceof VoidNode) {
      updatedNodes.push(updatedNode);

      return updatedNodes;
    }

    // For all other nodes, we need to traverse its children.
    updatedNode.children = traverse(updatedNode.children, pred);
    updatedNodes.push(updatedNode);

    return updatedNodes;
  }, []);
};

/**
 * Very similar to the `traverse` function, except that instead of manipulating
 * the tree of nodes, it simply finds nodes matching the predicate function
 * passed as `pred` instead. Useful if you want to get only a subset of the
 * tree of nodes.
 */
const find = (nodes: Node[], pred: (node: Node) => boolean): Node[] => {
  return nodes.reduce((foundNodes: Node[], node: Node) => {
    if (pred(node)) {
      foundNodes.push(node);
    }

    if (node.children.length) {
      foundNodes.push(...find(node.children, pred));
    }

    return foundNodes;
  }, []);
};

/**
 * -----------------------------------------------------------------
 * Serializer
 * -----------------------------------------------------------------
 */
export type Serializer<T> = (nodes: Node[]) => T;

/**
 * Serializer the given `nodes` into an HTML string.
 */
const htmlSerializer: Serializer<string> = (nodes: Node[]): string => {
  return nodes.reduce((html: string, node: Node) => {
    if (node instanceof TextNode) {
      return `${html}${node.value}`;
    }

    if (node instanceof FragmentNode) {
      return `${html}${htmlSerializer(node.children)}`;
    }

    const attrsStr = Object.entries(node.attributes).reduce(
      (acc, [key, value]) => {
        if (value === true) {
          return `${acc} ${key}`;
        }

        return `${acc} ${key}="${value}"`;
      },
      "",
    );

    html += `<${node.name}${attrsStr}>`;

    if (!(node instanceof VoidNode)) {
      html += htmlSerializer(node.children);
      html += `</${node.name}>`;
    }

    return html;
  }, "");
};

/**
 * Serializes the given `nodes` into a JSON string.
 */
const jsonSerializer: Serializer<string> = (nodes: Node[]): string => {
  const reducer = (reduceNodes: Node[]) =>
    reduceNodes.reduce((data: Record<string, unknown>[], node: Node) => {
      if (node instanceof TextNode) {
        data.push({
          type: "textNode",
          value: node.value,
        });
      }

      if (node instanceof FragmentNode) {
        data.push({
          type: "fragmentNode",
          children: reducer(node.children),
        });
      }

      if (node instanceof VoidNode) {
        data.push({
          type: "voidNode",
          name: node.name,
          attributes: node.attributes,
          children: reducer(node.children),
        });
      }

      data.push({
        type: "node",
        name: node.name,
        attributes: node.attributes,
        children: reducer(node.children),
      });

      return data;
    }, []);

  return JSON.stringify(reducer(nodes));
};

/**
 * Serializes the given `nodes` with the given `serializer`.
 */
const serialize = <T>(nodes: Node[], serializer: Serializer<T>): T => {
  return serializer(nodes);
};

/**
 * -----------------------------------------------------------------
 * Export
 * -----------------------------------------------------------------
 */
export default {
  nodes,
  traverse,
  find,
  serialize,
  Node,
  TextNode,
  FragmentNode,
  VoidNode,
  Serializer: {
    Html: htmlSerializer,
    Json: jsonSerializer,
  },
};
