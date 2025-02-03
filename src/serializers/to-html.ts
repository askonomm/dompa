// export const ToHtml: Serializer<string> = (nodes: Node[]): string => {
//   return nodes.reduce((html: string, node: Node) => {
//     if (node instanceof TextNode) {
//       return `${html}${node.value}`;
//     }

//     if (node instanceof FragmentNode) {
//       return `${html}${ToHtml(node.children)}`;
//     }

//     const attributesStr = Object.entries(node.attributes).reduce(
//       (acc, [key, value]) => {
//         if (value === true) {
//           return `${acc} ${key}`;
//         }

//         return `${acc} ${key}="${value}"`;
//       },
//       ""
//     );

//     html += `<${node.name}${attributesStr}>`;

//     if (!(node instanceof VoidNode)) {
//       html += ToHtml(node.children);
//       html += `</${node.name}>`;
//     }

//     return html;
//   }, "");
// };
