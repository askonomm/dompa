import { defineConfig } from "tsup";

export default defineConfig({
  entry: ["src/dompa.ts"],
  clean: true,
  format: ["cjs", "esm"],
  dts: true,
  noExternal: [/(.*)/],
  minify: "terser",
  treeshake: "smallest",
  splitting: false,
});
