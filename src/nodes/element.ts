import { Attributes, Node } from "../dompa.ts";

type ElementNodeProps = {
  name: string;
  attributes: Attributes;
  children: Node[];
};

export default class ElementNode implements Node {
  name: string;
  attributes: Attributes;
  children: Node[];

  constructor({ name, attributes, children }: ElementNodeProps) {
    this.name = name;
    this.attributes = attributes;
    this.children = children;
  }

  /**
   * Returns the name of the node, or `defaultValue` if the name
   * is empty.
   *
   * @param {string | null} defaultValue
   * @returns {string}
   */
  public getName(defaultValue: string | null = null): string {
    if (!this.name.trim().length && defaultValue) {
      return defaultValue;
    }

    return this.name;
  }

  /**
   * Returns the attributes of the node, or `defaultAttributes` if the
   * attributes are empty.
   *
   * @param {Attributes | null} defaultAttributes
   * @returns {Attributes}
   */
  public getAttributes(defaultAttributes: Attributes | null): Attributes {
    if (Object.keys(this.attributes).length === 0 && defaultAttributes) {
      return defaultAttributes;
    }

    return this.attributes;
  }

  /**
   * Returns the children of the node, or `defaultChildren` if there
   * are no children.
   *
   * @param {Node[] | null} defaultChildren
   * @returns {Node[]}
   */
  public getChildren(defaultChildren: Node[] | null): Node[] {
    if (!this.children.length && defaultChildren) {
      return defaultChildren;
    }

    return this.children;
  }
}
