from __future__ import annotations
from typing import Dict, Union


class Node:
    name: str
    attributes: Dict[str, Union[str, bool]]
    children: list[Node]

    def __init__(self, name: str = None, attributes: Dict[str, Union[str, bool]] = None, children: list[Node] = None) -> None:
        self.name = name or ""
        self.attributes = attributes or {}
        self.children = children or []
