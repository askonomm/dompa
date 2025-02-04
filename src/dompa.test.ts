import Dompa from "./dompa.ts";

test("abc", () => {
  const nodes = Dompa.nodes("<div>a<span>b<span>c</span></span></div>");
  const html = Dompa.serialize(nodes, Dompa.Serializer.Html);

  expect(html).toBe("<div>a<span>b<span>c</span></span></div>");
});
