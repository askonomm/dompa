import Dompa from "./dompa.ts";

test("abc", () => {
  const dom = new Dompa("test<div></div><span></span><img><br><hr><input>");
});
