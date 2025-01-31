import ElementNode from "./element.ts";

type IrNodeProps = {
  name: string;
  coords: [number, number];
  children: IrNode[];
};

export default class IrNode {
  name: string;
  coords: [number, number];
  children: IrNode[];

  constructor({ name, coords, children }: IrNodeProps) {
    this.name = name;
    this.coords = coords;
    this.children = children;
  }
}
