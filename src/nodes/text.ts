import ElementNode from "./element.ts";

export default class TextNode extends ElementNode {
  public value: string;

  constructor(value: string) {
    super({
      name: "",
      attributes: {},
      children: [],
    });

    this.value = value;
  }

  public getValue(defaultValue: string | null): string {
    if (!this.value.length && defaultValue) {
      return defaultValue;
    }

    return this.value;
  }
}
