import { Effect, Ref, Context, pipe } from "effect";

/**
 *
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
 *
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
 *
 */
export class Node {
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
 *
 */
export class TextNode extends Node {
  value: string;

  constructor(value: string) {
    super({ name: "_text_node", attributes: {}, children: [] });
    this.value = value;
  }
}

/**
 *
 */
export class VoidNode extends Node {}

/**
 *
 */
export class FragmentNode extends Node {}

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

const setCoordOfLastIrNodeEffect = (name: string, coord: number) =>
  Effect.gen(function* () {
    const irNodesState = yield* IrNodesState;
    const irNodesValue = yield* Ref.get(irNodesState);

    if (irNodesValue.length === 0) {
      return yield* Effect.fail(Error("No IrNode's found."));
    }

    const index = irNodesValue.findLastIndex((n) => n.name === name);

    if (index === -1) {
      return yield* Effect.fail(Error("Could not find matching IrNode."));
    }

    yield* Ref.update(irNodesState, (irNodes) => {
      irNodes[index].coords = [irNodes[index].coords[0], coord];

      return irNodes;
    });

    return yield* Effect.succeed(true);
  });

/**
 *
 * @param tag
 * @param coord
 * @returns
 */
const closeIrNodeEffect = (tag: string, coord: number) =>
  Effect.gen(function* () {
    const name = tag
      .substring(2, tag.length - 1)
      .split(" ")[0]
      .trim();

    yield* setCoordOfLastIrNodeEffect(name, coord);
  });

/**
 *
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

const joinIrNodesEffect = Effect.gen(function* () {
  const irNodesState = yield* IrNodesState;

  const joinedIrNodes = (
    irNodes: IrNode[],
    addedNodes: Set<string>,
  ): IrNode[] => {
    return irNodes.reduce((iterNodes: IrNode[], node: IrNode) => {
      const nodeCoordsKey = JSON.stringify(node.coords);

      if (addedNodes.has(nodeCoordsKey)) return iterNodes;

      addedNodes.add(nodeCoordsKey);

      const iterNodeChildrenList = findIrNodesInCoords(irNodes, node.coords);
      const iterNodeChildrenNodes = iterNodeChildrenList.map((n) => n[1]);

      node.children = joinedIrNodes(iterNodeChildrenNodes, addedNodes);
      iterNodes.push(node);

      return iterNodes;
    }, []);
  };

  yield* Ref.update(irNodesState, (irNodes) => {
    return joinedIrNodes(irNodes, new Set<string>());
  });
});

const nodeAttrsStrFromCoords = (
  html: string,
  coords: [number, number],
): string | null => {
  const nodeStr = html.substring(coords[0], coords[1]);
  let attributeStrStart: number | null = null;
  let attributeStrEnd: number | null = null;

  for (let i = 0; i < nodeStr.length; i++) {
    const char = nodeStr.at(i);

    if (char === ">") {
      attributeStrEnd = i;
      break;
    }

    if (attributeStrStart === null && char === " ") {
      attributeStrStart = i + 1;
    }
  }

  if (attributeStrStart === null || attributeStrEnd === null) {
    return null;
  }

  return nodeStr.substring(attributeStrStart, attributeStrEnd);
};

const nodeAttrsFromCoords = (html: string, coords: [number, number]): Attrs => {
  let attributes: Attrs = {};
  const attrsStr = nodeAttrsStrFromCoords(html, coords);

  if (!attrsStr) {
    return attributes;
  }

  let iterAttrName = "";
  let iterAttrValue: string | null = null;

  for (let i = 0; i < attrsStr.length; i++) {
    const char = attrsStr.at(i);

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
    if (i === attrsStr.length - 1 && iterAttrValue !== null) {
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
    if ((char === " " || i === attrsStr.length - 1) && iterAttrValue === null) {
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
 *
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
 *
 * @param html
 * @returns
 */
export const dompa = (html: string) => {
  const program = Effect.gen(function* () {
    yield* createIrNodesEffect;
    yield* joinIrNodesEffect;
    yield* createNodesEffect;

    return yield* Ref.get(yield* NodesState);
  });

  return Effect.runSync(
    program.pipe(
      Effect.provideServiceEffect(HtmlState, Ref.make(html)),
      Effect.provideServiceEffect(IrNodesState, Ref.make<IrNode[]>([])),
      Effect.provideServiceEffect(NodesState, Ref.make<Node[]>([])),
    ),
  );
};

/**
 *
 * @param nodes
 * @param cb
 * @returns
 */
export const traverse = (nodes: Node[], cb: (node: Node) => Node | null) => {
  return nodes.reduce((updatedNodes: Node[], node: Node) => {
    const updatedNode = cb(node);

    // If the callback returns `null`, it means we want to remove the node.
    if (updatedNode === null) {
      return updatedNodes;
    }

    // If the callback returns a `TextNode`, we don't need to traverse it's children.
    if (updatedNode instanceof TextNode) {
      updatedNodes.push(updatedNode);

      return updatedNodes;
    }

    // If the callback returns a `FragmentNode`, we need to traverse it's children,
    // as it's children will replace the `FragmentNode` itself.
    if (updatedNode instanceof FragmentNode) {
      updatedNodes.push(...traverse(updatedNode.children, cb));

      return updatedNodes;
    }

    // If the callback returns a `VoidNode`, we don't need to traverse it's children.
    if (updatedNode instanceof VoidNode) {
      updatedNodes.push(updatedNode);

      return updatedNodes;
    }

    // For all other nodes, we need to traverse it's children.
    updatedNode.children = traverse(updatedNode.children, cb);
    updatedNodes.push(updatedNode);

    return updatedNodes;
  }, []);
};

/**
 *
 * @param nodes
 * @param cb
 * @returns
 */
export const find = (nodes: Node[], cb: (node: Node) => boolean) => {
  return nodes.reduce((foundNodes: Node[], node: Node) => {
    if (cb(node)) {
      foundNodes.push(node);
    }

    if (node.children.length) {
      foundNodes.push(...find(node.children, cb));
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
 *
 * @param nodes
 * @param serializer
 * @returns
 */
export const serialize = <T>(nodes: Node[], serializer: Serializer<T>): T => {
  return serializer(nodes);
};
