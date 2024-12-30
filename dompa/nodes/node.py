from __future__ import annotations
from typing import Dict


class Node:
    name: str
    attributes: Dict[str, str]
    children: list[Node]

    def __init__(self, name: str = None, attributes: Dict[str, str] = None, children: list[Node] = None) -> None:
        self.name = name or ""
        self.attributes = attributes or {}
        self.children = children or []
