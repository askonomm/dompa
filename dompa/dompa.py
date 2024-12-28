from __future__ import annotations
from typing import Dict, Any, Tuple, Callable, Optional, Union


class Node:
    name: str
    attributes: Dict[str, str]
    children: list[Node]

    def __init__(self, name: str, attributes: Dict[str, str], children: list[Node]):
        self.name = name
        self.attributes = attributes
        self.children = children


class IRBlockNode:
    name: str
    coords: Tuple[int, int]
    children: list[Union[IRBlockNode, str]]

    def __init__(self, name: str, coords: Tuple[int, int]):
        self.name = name
        self.coords = coords


class Dompa:
    __template: str
    __ir_block_nodes: list[IRBlockNode]
    __block_elements = ["html", "head", "body", "div", "span", "a"]
    __inline_elements = ["!doctype", "img"]

    def __init__(self, template: str):
        self.__template = template
        self.__ir_block_nodes = []
        self.__create_ir_block_pos_nodes()
        self.__join_ir_block_nodes()
        self.__create_nodes()

    def __create_ir_block_pos_nodes(self):
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
                    self.__maybe_close_ir_block_node(tag, tag_end)
                    tag_start = None
                    tag_end = None
                    continue

                name = tag[1:-1].split(" ")[0].strip()

                if name.lower() in self.__block_elements:
                    self.__ir_block_nodes.append(
                        IRBlockNode(name=name, coords=(tag_start, 0))
                    )

                if name.lower() in self.__inline_elements:
                    self.__ir_block_nodes.append(
                        IRBlockNode(name=name, coords=(tag_start, tag_end))
                    )

                tag_start = None
                tag_end = None
                continue

            if tag_start is None and tag_end is None and text_start is None:
                text_start = idx

            if text_start is not None and text_end is not None:
                self.__ir_block_nodes.append(
                    IRBlockNode(name="text", coords=(text_start, text_end))
                )

                text_start = None
                text_end = None

    def __maybe_close_ir_block_node(self, tag: str, coord: int):
        el_name = tag[2:-1].split(" ")[0].strip()
        match = self.__find_last_ir_block_pos_match(lambda node: node.name == el_name)

        if match is not None:
            [idx, last_ir_pos_node] = match
            last_ir_pos_node.coords = (last_ir_pos_node.coords[0], coord)
            self.__ir_block_nodes[idx] = last_ir_pos_node

    def __join_ir_block_nodes(self):
        set_coords = set()

        for node in self.__ir_block_nodes:
            if node.coords in set_coords:
                continue

            nodes_within = self.__find_ir_block_nodes_in_coords(node.coords)
            node.children = self.__recur_ir_block_node_children(
                nodes_within, set_coords
            )

        self.__ir_block_nodes = [
            node for node in self.__ir_block_nodes if node.coords not in set_coords
        ]

    def __recur_ir_block_node_children(
        self, nodes: list[Tuple[int, IRBlockNode]], set_coords: set
    ):
        children = []

        for idx, child_node in nodes:
            if child_node.coords in set_coords:
                continue

            set_coords.add(child_node.coords)
            child_node_children = self.__find_ir_block_nodes_in_coords(
                child_node.coords
            )
            child_node.children = self.__recur_ir_block_node_children(
                child_node_children, set_coords
            )
            children.append(child_node)

        return children

    def __find_ir_block_nodes_in_coords(
        self, coords: Tuple[int, int]
    ) -> list[Tuple[int, IRBlockNode]]:
        ir_block_nodes = []
        [start, end] = coords

        for idx, node in enumerate(self.__ir_block_nodes):
            [iter_start, iter_end] = node.coords

            if iter_start > start and iter_end < end:
                ir_block_nodes.append((idx, node))

        return ir_block_nodes

    def __create_nodes(self):
        pass

    def __run_attribute_parsers(self):
        pass

    def __find_last_ir_block_pos_match(
        self, condition: Callable[[Any], bool]
    ) -> Optional[Tuple[int, Any]]:
        idx = len(self.__ir_block_nodes)

        for item in reversed(self.__ir_block_nodes):
            idx -= 1

            if condition(item):
                return idx, item

        return None

    def toHtml(self):
        pass
