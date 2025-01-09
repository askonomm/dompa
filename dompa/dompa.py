from __future__ import annotations
import copy
from typing import Any, Tuple, Callable, Optional, Union, Set
from .serializer import Serializer
from .nodes import IrNode, TextNode, VoidNode, Node, FragmentNode


class Dompa:
    __nodes: list[Node]
    __template: str
    __ir_nodes: list[IrNode]
    __void_names = [
        "!doctype",
        "area",
        "base",
        "br",
        "col",
        "embed",
        "hr",
        "img",
        "input",
        "link",
        "meta",
        "source",
        "track",
        "wbr"
    ]

    def __init__(self, template: str) -> None:
        self.__template = template
        self.__ir_nodes = []
        self.__nodes = []
        self.__create_ir_nodes()
        self.__join_ir_nodes()
        self.__create_nodes()

    def serialize(self, serializer: type[Serializer]) -> Any:
        """
        Serialize the node tree into an output created by a given
        serializer.
        """
        return serializer(self.__nodes).serialize()

    def __create_ir_nodes(self) -> None:
        """
        Parses the given template string into a flat list of `IrNode`s,
        a "intermediate representation" nodes that contain the raw info
        required to later on turn it into a nested tree with proper
        data.
        """
        tag_start = None
        tag_end = None
        text_start = None
        text_end = None

        for idx, char in enumerate(self.__template):
            # start of a tag, or perhaps end of a text node
            if char == "<":
                if text_start is not None:
                    text_end = idx

                tag_start = idx

            # last char, but not end of tag, means we're
            # in a text node
            if len(self.__template) - 1 == idx and char != ">":
                text_end = idx + 1

            # end of a tag
            if char == ">":
                tag_end = idx + 1

            # when we have all tag collection data, lets collect it
            if tag_start is not None and tag_end is not None:
                tag = self.__template[tag_start:tag_end]

                if tag.startswith("</"):
                    self.__maybe_close_ir_node(tag, tag_end)
                    tag_start = None
                    tag_end = None
                    continue

                name = tag[1:-1].split(" ")[0].strip()

                if name.lower() in self.__void_names:
                    self.__ir_nodes.append(IrNode(name=name, coords=(tag_start, tag_end)))
                else:
                    self.__ir_nodes.append(IrNode(name=name, coords=(tag_start, 0)))

                tag_start = None
                tag_end = None
                continue

            # no tag collection data, and no `text_start` means we start
            # collecting text data
            if tag_start is None and tag_end is None and text_start is None:
                text_start = idx

            # when we have all text collection data, lets collect it
            if text_start is not None and text_end is not None:
                self.__ir_nodes.append(IrNode(name="_text_node", coords=(text_start, text_end)))

                text_start = None
                text_end = None

    def __maybe_close_ir_node(self, tag: str, coord: int) -> None:
        """
        If there's a `IrNode` with a name matching the previous one where its
        end coord is 0, it means this coord is actually the ending of that one,
        and thus we update its end coord, effectively "closing" it.
        """
        el_name = tag[2:-1].split(" ")[0].strip()
        match = self.__find_last_ir_node(lambda node: node.name == el_name)

        if match is not None and match[1].coords[1] == 0:
            [idx, last_ir_pos_node] = match
            last_ir_pos_node.coords = (last_ir_pos_node.coords[0], coord)
            self.__ir_nodes[idx] = last_ir_pos_node

    def __join_ir_nodes(self) -> None:
        """
        Joins the `IrNode`'s together based on their `coords`.
        """
        set_coords: Set[Tuple[int, int]] = set()
        self.__ir_nodes = self.__recur_join_ir_nodes(self.__ir_nodes, set_coords)

    def __recur_join_ir_nodes(self, nodes: list[IrNode], set_coords: set) -> list[IrNode]:
        """
        Recursively iterates over `IrNode`'s and joins them together based on their
        `coords`. Meaning that nodes within other nodes become their children,
        and so forth.
        """
        children = []

        for child_node in nodes:
            if child_node.coords in set_coords:
                continue

            set_coords.add(child_node.coords)
            child_node_list = self.__find_ir_nodes_in_coords(child_node.coords)
            child_node_nodes: list[IrNode] = list([item[1] for item in child_node_list])
            child_node.children = self.__recur_join_ir_nodes(child_node_nodes, set_coords)
            children.append(child_node)

        return children

    def __find_ir_nodes_in_coords(self, coords: Tuple[int, int]) -> list[Tuple[int, IrNode]]:
        """
        Finds all `IrNode`'s that are within given `coords`.
        """
        ir_block_nodes = []
        [start, end] = coords

        for idx, node in enumerate(self.__ir_nodes):
            [iter_start, iter_end] = node.coords

            if iter_start > start and iter_end < end:
                ir_block_nodes.append((idx, node))

        return ir_block_nodes

    def __find_last_ir_node(self, condition: Callable[[Any], bool]) -> Optional[Tuple[int, IrNode]]:
        """
        Iterates over list of `IrNode`'s in reverse and returns the first one that matches the
        given `condition`, or `None` if no match was found.
        """
        idx = len(self.__ir_nodes)

        for item in reversed(self.__ir_nodes):
            idx -= 1

            if condition(item):
                return idx, item

        return None

    def __create_nodes(self) -> None:
        """
        Transforms `IrNode`'s to `Node`'s.
        """
        self.__nodes = self.__recur_create_nodes(self.__ir_nodes)

    def __recur_create_nodes(self, ir_nodes: list[IrNode]) -> list[Node]:
        """
        Recursively iterates over a list of `IrNode`'s and transforms them
        to a tree of `Node`'s.
        """
        nodes = []

        for ir_node in ir_nodes:
            if len(ir_node.children) == 0:
                nodes.append(self.__ir_node_to_node(ir_node))
            else:
                node = self.__ir_node_to_node(ir_node)
                node.children = self.__recur_create_nodes(ir_node.children)
                nodes.append(node)

        return nodes

    def __ir_node_to_node(self, ir_node: IrNode) -> Node:
        """
        Transform a `IrNode` to a `Node`. If the `IrNode`'s name is `_text_node`,
        it will create a `TextNode` instance. If the `IrNode`'s name is within the
        `__void_names` list, it will create a `VoidNode`. In any other case, a
        generic `Node` will be created.
        """
        if ir_node.name == "_text_node":
            return TextNode(
                value=self.__template[ir_node.coords[0]: ir_node.coords[1]],
            )

        if ir_node.name.lower() in self.__void_names:
            return VoidNode(
                name=ir_node.name,
                attributes=self.__node_attributes_from_coords(ir_node.coords),
            )

        return Node(
            name=ir_node.name,
            attributes=self.__node_attributes_from_coords(ir_node.coords),
            children=[],
        )

    def __node_attributes_from_coords(self, coords: Tuple[int, int]) -> dict[str, Union[str, bool]]:
        """
        Composes a dictionary of node attributes from the tag located at `coords`.
        """
        attributes: dict[str, Union[str, bool]] = {}
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
        """
        Parses the attribute string from given `coords`. The `coords` point to an entire
        tag, which could have child tags, so this only gets the attribute str from the first
        tag, or none at all.
        """
        node_str = self.__template[coords[0]: coords[1]]
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

    def query(self, callback: Callable[[Node], bool]) -> list[Node]:
        """
        Find all nodes that pass the truth check of the `callback` function.
        """
        return copy.deepcopy(self.__recur_query(self.__nodes, callback))

    def __recur_query(self, nodes: list[Node], callback: Callable[[Node], bool]) -> list[Node]:
        """
        Recursively iterates over the node tree and returns nodes based on the `callback`
        function which receives a iteration `Node` as a parameter, and must be return a
        boolean `true` or `false`.
        """
        found_nodes: list[Node] = []

        for node in nodes:
            if callback(node):
                found_nodes.append(node)
                continue

            if len(node.children) != 0:
                found_nodes.extend(self.__recur_query(node.children, callback))

        return found_nodes

    def traverse(self, callback: Callable[[Node], Optional[Node]]) -> None:
        """
        Traverses the node tree, node by node, passing each node to the `callback`
        function. The `callback` function must always return either a `Node` or
        `None`, latter in the case of removal.
        """
        self.__nodes = self.__recur_traverse(self.__nodes, callback)

    def __recur_traverse(self, nodes: list[Node], callback: Callable[[Node], Optional[Node]]) -> list[Node]:
        """
        Recursively iterates over the node tree and updates nodes based on the `callback`
        function. If the `callback` function returns a `Node`, then the `Node` in the
        iteration will be replaced, and if the `callback` function returns `None`, then
        the `Node` in the iteration will be removed.
        """
        updated_nodes: list[Node] = []

        for node in nodes:
            if isinstance(node, TextNode):
                updated_nodes.append(node)
                continue

            updated_node = callback(node)

            if updated_node is None:
                continue

            if isinstance(updated_node, FragmentNode):
                updated_nodes.extend(self.__recur_traverse(updated_node.children, callback))
                continue

            updated_node.children = self.__recur_traverse(updated_node.children, callback)
            updated_nodes.append(updated_node)

        return updated_nodes

    def get_nodes(self) -> list[Node]:
        return self.__nodes
