from __future__ import annotations
from typing import Dict, Any, Tuple, Callable, Optional, Union
from .query_engine import QueryEngine
from .nodes import IrNode, TextNode, Node


class Dompa(QueryEngine):
    __template: str
    __ir_nodes: list[IrNode]
    __nodes: list[Union[TextNode, Node]]
    __block_elements = ["html", "head", "body", "div", "span", "a"]
    __inline_elements = ["!doctype", "img", "input"]

    def __init__(self, template: str) -> None:
        self.__template = template
        self.__ir_nodes = []
        self.__nodes = []
        self.__create_ir_nodes()
        self.__join_ir_nodes()
        self.__create_nodes()
        super().__init__(self.__nodes)

    def __create_ir_nodes(self) -> None:
        tag_start = None
        tag_end = None
        text_start = None
        text_end = None

        for idx, part in enumerate(self.__template):
            if part == "<":
                if text_start is not None:
                    text_end = idx

                tag_start = idx

            if part == ">":
                tag_end = idx + 1

            if tag_start is not None and tag_end is not None:
                tag = self.__template[tag_start:tag_end]

                if tag.startswith("</"):
                    self.__maybe_close_ir_node(tag, tag_end)
                    tag_start = None
                    tag_end = None
                    continue

                name = tag[1:-1].split(" ")[0].strip()

                if name.lower() in self.__block_elements:
                    self.__ir_nodes.append(IrNode(name=name, coords=(tag_start, 0)))

                if name.lower() in self.__inline_elements:
                    self.__ir_nodes.append(IrNode(name=name, coords=(tag_start, tag_end)))

                tag_start = None
                tag_end = None
                continue

            if tag_start is None and tag_end is None and text_start is None:
                text_start = idx

            if text_start is not None and text_end is not None:
                self.__ir_nodes.append(IrNode(name="text", coords=(text_start, text_end)))

                text_start = None
                text_end = None

    def __maybe_close_ir_node(self, tag: str, coord: int):
        el_name = tag[2:-1].split(" ")[0].strip()
        match = self.__find_last_ir_node(lambda node: node.name == el_name)

        if match is not None:
            [idx, last_ir_pos_node] = match
            last_ir_pos_node.coords = (last_ir_pos_node.coords[0], coord)
            self.__ir_nodes[idx] = last_ir_pos_node

    def __join_ir_nodes(self):
        set_coords = set()

        for node in self.__ir_nodes:
            if node.coords in set_coords:
                continue

            nodes_within = self.__find_ir_nodes_in_coords(node.coords)
            node.children = self.__recur_ir_node_children(nodes_within, set_coords)

        self.__ir_nodes = [node for node in self.__ir_nodes if node.coords not in set_coords]

    def __recur_ir_node_children(self, nodes: list[Tuple[int, IrNode]], set_coords: set):
        children = []

        for idx, child_node in nodes:
            if child_node.coords in set_coords:
                continue

            set_coords.add(child_node.coords)
            child_node_children = self.__find_ir_nodes_in_coords(child_node.coords)
            child_node.children = self.__recur_ir_node_children(child_node_children, set_coords)
            children.append(child_node)

        return children

    def __find_ir_nodes_in_coords(self, coords: Tuple[int, int]) -> list[Tuple[int, IrNode]]:
        ir_block_nodes = []
        [start, end] = coords

        for idx, node in enumerate(self.__ir_nodes):
            [iter_start, iter_end] = node.coords

            if iter_start > start and iter_end < end:
                ir_block_nodes.append((idx, node))

        return ir_block_nodes

    def __find_last_ir_node(self, condition: Callable[[Any], bool]) -> Optional[Tuple[int, Any]]:
        idx = len(self.__ir_nodes)

        for item in reversed(self.__ir_nodes):
            idx -= 1

            if condition(item):
                return idx, item

        return None

    def __create_nodes(self):
        self.__nodes = self.__recur_create_nodes(self.__ir_nodes)

    def __recur_create_nodes(self, ir_nodes: list[IrNode]) -> list[Union[TextNode, Node]]:
        nodes = []

        for ir_node in ir_nodes:
            if len(ir_node.children) == 0:
                nodes.append(self.__ir_node_to_node(ir_node))
            else:
                node = self.__ir_node_to_node(ir_node)
                node.children = self.__recur_create_nodes(ir_node.children)
                nodes.append(node)

        return nodes

    def __ir_node_to_node(self, ir_node: IrNode) -> Union[TextNode, Node]:
        if ir_node.name == "text":
            return TextNode(
                value=self.__template[ir_node.coords[0]:ir_node.coords[1]],
            )

        return Node(
            name=ir_node.name,
            attributes=self.__node_attributes_from_coords(ir_node.coords),
            children=[]
        )

    def __node_attributes_from_coords(self, coords: Tuple[int, int]) -> Dict[str, str]:
        attributes = {}
        attr_str = self.__node_attr_str_from_coords(coords)

        if attr_str is None:
            return attributes

        iter_attr_name = ""
        iter_attr_value = None

        for idx, char in enumerate(attr_str):
            # if we encounter a space, and the last char of `iter_attr_value` is `"`
            # it means we're not in an attr value, in which case a
            # space would be part of the value, but rather ending an attribute
            # declaration and moving onto the next one.
            if char == " " and iter_attr_value is not None and iter_attr_value[-1] == '"':
                if iter_attr_value[0] == '"' and iter_attr_value[-1] == '"':
                    iter_attr_value = iter_attr_value[1:-1]

                attributes[f"{iter_attr_name}{char}".strip()] = iter_attr_value
                iter_attr_name = ""
                iter_attr_value = None
                continue

            # same as above is true when we are the last char of the entire `attr_str`,
            # in which case we are ending an attribute declaration.
            if idx == len(attr_str) - 1 and iter_attr_value is not None:
                iter_attr_value += char

                if iter_attr_value[0] == '"' and iter_attr_value[-1] == '"':
                    iter_attr_value = iter_attr_value[1:-1]

                attributes[iter_attr_name.strip()] = iter_attr_value
                iter_attr_name = ""
                iter_attr_value = None
                continue

            # and, same as above is also true when we encounter a space and there is
            # no `iter_attr_value`, meaning it is a Truthy attribute, which needs
            # no explicit value.
            if (char == " " or idx == len(attr_str) - 1) and iter_attr_value is None:
                attributes[f"{iter_attr_name}{char}".strip()] = True
                iter_attr_name = ""
                iter_attr_value = None
                continue

            # If we encounter the `=` char, it means we are done with `iter_attr_name`,
            # and can move on to start creating the `iter_attr_value`.
            if iter_attr_value is None and char == "=":
                iter_attr_value = ""
                continue

            # in all other cases if we have already set `iter_attr_value`, keep on
            # collecting it.
            if iter_attr_value is not None:
                iter_attr_value += char
                continue

            # or if we have not set `iter_attr_value`, keep on collecting `iter_attr_name`.
            if iter_attr_value is None:
                iter_attr_name += char

        return attributes

    def __node_attr_str_from_coords(self, coords: Tuple[int, int]) -> Optional[str]:
        node_str = self.__template[coords[0]:coords[1]]
        attr_str_start = None
        attr_str_end = None

        # parse the coords for the attribute str
        for idx, char in enumerate(node_str):
            # stop whenever the tag ends
            if char == ">":
                attr_str_end = idx
                break

            if attr_str_start is None and char == " ":
                attr_str_start = idx + 1

        if attr_str_start is None or attr_str_end is None:
            return None

        return node_str[attr_str_start:attr_str_end]

    def nodes(self) -> list[Union[TextNode, Node]]:
        return self.__nodes

    def html(self) -> str:
        return self.__recur_to_html(self.__nodes)

    def __recur_to_html(self, nodes: list[Union[TextNode, Node]]) -> str:
        html = ""

        for node in nodes:
            if isinstance(node, TextNode):
                html += node.value
            else:
                if node.attributes != {}:
                    html += f"<{node.name} {self.__node_attrs_from_dict(node.attributes)}>"
                else:
                    html += f"<{node.name}>"

                if node.name.lower() in self.__block_elements:
                    html += self.__recur_to_html(node.children)
                    html += f"</{node.name}>"

        return html

    @staticmethod
    def __node_attrs_from_dict(attributes: dict[str, str]) -> str:
        attr_str = ""

        for key, value in attributes.items():
            if value is True:
                attr_str += f"{key} "
            else:
                attr_str += f"{key}=\"{value}\" "

        return attr_str.strip()
