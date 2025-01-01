from __future__ import annotations
from typing import Union


class Node:
    name: str
    attributes: dict[str, Union[str, bool]]
    children: list[Node]

    def __init__(self, name: str = None, attributes: dict[str, Union[str, bool]] = None, children: list[Node] = None) -> None:
        self.name = name or ""
        self.attributes = attributes or {}
        self.children = children or []
