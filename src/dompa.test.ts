import { dompa, traverse } from "./dompa.ts";

test("abc", () => {
  const nodes = dompa("<div>test</div>hello world");

  expect(nodes).toBe("updated html");
});
