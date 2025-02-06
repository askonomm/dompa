import Dompa from "./dompa.ts";
import { expect } from "vitest";

describe("Dompa Serialization Tests", () => {
  test("html equality", () => {
    const html = "<html><body>Hello</body></html>";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(html);
  });

  test("html equality2", () => {
    const html = "<!DOCTYPE html><html><body>Hello</body></html>";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(html);
  });

  test("html equality3", () => {
    const html = '<div class="test test2 test3">Hello</div>';
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(html);
  });

  test("html equality4", () => {
    const html = '<input type="radio" checked>';
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(html);
  });

  test("html equality5", () => {
    const html = "Hello, World!";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe("Hello, World!");
  });

  test("html equality6", () => {
    const html = "<div>some elem</div>some text";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(
      "<div>some elem</div>some text",
    );
  });

  test("invalid to_html", () => {
    const html = "<div><p>Hello</p>";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(
      "<div></div><p>Hello</p>",
    );
  });

  test("invalid html2", () => {
    const html = "<div><p>Hello</div></p>";
    const nodes = Dompa.nodes(html);

    expect(Dompa.serialize(nodes, Dompa.Serializer.Html)).toBe(
      "<div>Hello</div><p></p>",
    );
  });

  test("invalid html3", () => {
    const html = "<div><p>Hello</div></span>";

    expect(() => Dompa.nodes(html)).toThrowError(
      "Error: Could not find matching node for <span>.",
    );
  });
});

describe("Dompa Nodes Tests", () => {
  test("nodes length and children", () => {
    const html = "<div>Hello, World</div>";
    const nodes = Dompa.nodes(html);

    expect(nodes.length).toBe(1);
    expect(nodes[0].children.length).toBe(1);
    expect(nodes[0] instanceof Dompa.Node).toBeTruthy();
    expect(nodes[0].children[0] instanceof Dompa.TextNode).toBeTruthy();
  });

  // Add more tests for the remaining functions here...
});

describe("Dompa Query Tests", () => {
  test("query by name", () => {
    const html = "<div><h1>Title</h1><p>Content</p></div>";
    const nodes = Dompa.nodes(html);

    const result = Dompa.find(nodes, (n) => n.name === "h1");
    expect(result.length).toBe(1);
    expect(result[0] instanceof Dompa.Node).toBeTruthy();
    expect(result[0].children[0] instanceof Dompa.TextNode).toBeTruthy();
  });

  // Add more tests for the remaining functions here...
});

describe("Dompa Traverse Tests", () => {
  test("traverse update node", () => {
    const html = "<div><h1>Title</h1><p>Content</p></div>";
    const nodes = Dompa.nodes(html);
    const updatedNodes = Dompa.traverse(nodes, (node) => {
      if (node.name === "h1") {
        node.children = [new Dompa.TextNode("Hello, World!")];
      }
      return node;
    });

    expect(Dompa.serialize(updatedNodes, Dompa.Serializer.Html)).toBe(
      "<div><h1>Hello, World!</h1><p>Content</p></div>",
    );
  });

  // Add more tests for the remaining functions here...
});
