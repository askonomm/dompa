from __future__ import annotations
from typing import Union, Dict
from .text_node import TextNode


class Node:
    name: str
    attributes: Dict[str, str]
    children: list[Union[TextNode, Node]]

    def __init__(self, name: str, attributes: Dict[str, str], children: list[Union[TextNode, Node]]) -> None:
        self.name = name
        self.attributes = attributes
        self.children = children
