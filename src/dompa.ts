import ElementNode from "./nodes/element.ts";
import IrNode from "./nodes/ir.ts";
import TextNode from "./nodes/text.ts";
import VoidNode from "./nodes/void.ts";

export type Attributes = Record<string, string | true>;

export interface Node {
  name: string;
  attributes: Attributes;
  children: Node[];
  getName(defaultValue: string | null): string;
  getAttributes(defaultAttributes: Attributes | null): Attributes;
  getChildren(defaultChildren: Node[] | null): Node[];
}

export type Serializer<T> = (nodes: Node[]) => T;

export default class Dompa {
  private nodes: Node[] = [];
  private irNodes: IrNode[] = [];
  private html: string;
  private voidNames: string[] = [
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

  constructor(html: string) {
    this.html = html;

    // Create the IR nodes
    this.irNodes = this.createIrNodes();

    // Join the IR nodes so we'd get a nested tree structure
    const addedNodes = new Set<string>();
    this.irNodes = this.joinIrNodes(this.irNodes, addedNodes);

    // Create the final nodes
    this.nodes = this.createNodes(this.irNodes);

    console.log(this.nodes);
  }

  private createIrNodes(): IrNode[] {
    let irNodes = [];
    let tagStart: number | null = null;
    let tagEnd: number | null = null;
    let textStart: number | null = null;
    let textEnd: number | null = null;

    for (let i = 0; i < this.html.length; i++) {
      const char = this.html.at(i);

      // Start of a tag, or end of a text node
      if (char === "<") {
        if (textStart !== null) {
          textEnd = i;
        }

        tagStart = i;
      }

      // If we're at the last char and it's not the end of a tag,
      // means we're in a text node
      if (i === this.html.length - 1 && char !== ">") {
        textEnd = i + 1;
      }

      // End of a tag
      if (char === ">") {
        tagEnd = i + 1;
      }

      // When we have both tagStart and tagEnd, collect
      if (tagStart !== null && tagEnd !== null) {
        const tag = this.html.substring(tagStart, tagEnd);

        if (tag.startsWith("</")) {
          this.closeIrNode(irNodes, tag, tagEnd);
          tagStart = null;
          tagEnd = null;
          continue;
        }

        const name = tag
          .substring(1, tag.length - 1)
          .split(" ")[0]
          .trim();

        if (this.voidNames.includes(name.toLowerCase())) {
          irNodes.push(
            new IrNode({
              name,
              coords: [tagStart, tagEnd],
              children: [],
            })
          );
        } else {
          irNodes.push(
            new IrNode({
              name,
              coords: [tagStart, 0],
              children: [],
            })
          );
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
        irNodes.push(
          new IrNode({
            name: "_text_node",
            coords: [textStart, textEnd],
            children: [],
          })
        );

        textStart = null;
        textEnd = null;
      }
    }

    return irNodes;
  }

  private closeIrNode(irNodes: IrNode[], tag: string, coord: number): void {
    const elName = tag
      .substring(2, tag.length - 1)
      .split(" ")[0]
      .trim();

    const match = this.findLastIrNode(
      (node: IrNode) => node.name === elName,
      irNodes
    );

    if (match !== null && match[1].coords[1] === 0) {
      const [i, lastIrPosNode] = match;
      lastIrPosNode.coords = [lastIrPosNode.coords[0], coord];
      irNodes[i] = lastIrPosNode;
    }
  }

  private findLastIrNode(
    cb: (n: IrNode) => boolean,
    nodes: IrNode[]
  ): [number, IrNode] | null {
    const index = nodes.findLastIndex((n) => cb(n));

    if (index !== -1) {
      return [index, nodes[index]];
    }

    return null;
  }

  /**
   * Join the IR nodes so we'd get a nested tree structure of nodes based on
   * the coordinates of the nodes.
   */
  private joinIrNodes(irNodes: IrNode[], addedNodes: Set<string>): IrNode[] {
    return irNodes.reduce((iterNodes: IrNode[], node: IrNode) => {
      const nodeCoordsKey = JSON.stringify(node.coords);

      if (!addedNodes.has(nodeCoordsKey)) {
        addedNodes.add(nodeCoordsKey);

        const iterNodeChildrenList = this.findIrNodesInCoords(node.coords);
        const iterNodeChildrenNodes = iterNodeChildrenList.map((n) => n[1]);
        node.children = this.joinIrNodes(iterNodeChildrenNodes, addedNodes);

        iterNodes.push(node);
      }

      return iterNodes;
    }, []);
  }

  private findIrNodesInCoords(coords: [number, number]): [number, IrNode][] {
    return this.irNodes.reduce(
      (nodes: [number, IrNode][], node: IrNode, i: number) => {
        if (node.coords[0] > coords[0] && node.coords[1] < coords[1]) {
          nodes.push([i, node]);
        }

        return nodes;
      },
      []
    );
  }

  private createNodes(irNodes: IrNode[]): Node[] {
    return irNodes.reduce((nodes: Node[], irNode: IrNode) => {
      if (!irNode.children.length) {
        nodes.push(this.irNodeToNode(irNode));
      } else {
        const node = this.irNodeToNode(irNode);
        node.children = this.createNodes(irNode.children);
        nodes.push(node);
      }

      return nodes;
    }, []);
  }

  private irNodeToNode(irNode: IrNode): Node {
    if (irNode.name === "_text_node") {
      return new TextNode(
        this.html.substring(irNode.coords[0], irNode.coords[1])
      );
    }

    if (this.voidNames.includes(irNode.name.toLowerCase())) {
      return new VoidNode({
        name: irNode.name,
        attributes: this.nodeAttributesFromCoords(irNode.coords),
        children: [],
      });
    }

    return new ElementNode({
      name: irNode.name,
      attributes: this.nodeAttributesFromCoords(irNode.coords),
      children: [],
    });
  }

  private nodeAttributesFromCoords(coords: [number, number]): Attributes {
    let attributes: Attributes = {};
    const attrStr = this.nodeAttributesStrFromCoords(coords);

    if (!attrStr) {
      return attributes;
    }

    let iterAttrName = "";
    let iterAttrValue: string | null = null;

    for (let i = 0; i < attrStr.length; i++) {
      const char = attrStr.at(i);

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
          iterAttrValue = iterAttrValue?.substring(
            1,
            iterAttrValue?.length - 1
          );
        }

        attributes[`${iterAttrName}${char}`.trim()] = iterAttrValue;
        iterAttrName = "";
        iterAttrValue = null;
        continue;
      }

      // same as above is true when we are the last char of the entire `attrStr`,
      // in which case we are ending an attribute declaration.
      if (i === attrStr.length - 1 && iterAttrValue !== null) {
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
        (char === " " || i === attrStr.length - 1) &&
        iterAttrValue === null
      ) {
        attributes[`${iterAttrName}${char}`.trim()] = true;
        iterAttrName = "";
        iterAttrValue = null;
        continue;
      }

      // If we encounter the `=` char, it means we are done with `iter_attr_name`,
      // and can move on to start creating the `iter_attr_value`.
      if (iterAttrValue === null && char === "=") {
        iterAttrValue = "";
        continue;
      }

      // in all other cases if we have already set `iter_attr_value`, keep on
      // collecting it.
      if (iterAttrValue !== null) {
        iterAttrValue += char;
        continue;
      }

      // or if we have not set `iter_attr_value`, keep on collecting `iter_attr_name`.
      if (iterAttrValue === null) {
        iterAttrName += char;
      }
    }

    return attributes;
  }

  private nodeAttributesStrFromCoords(coords: [number, number]): string | null {
    const nodeStr = this.html.substring(coords[0], coords[1]);
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
  }

  public serialize<T>(serializer: Serializer<T>): T {
    return serializer(this.nodes);
  }
}
