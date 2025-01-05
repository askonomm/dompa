from __future__ import annotations
from typing import Union, Any

from .node_action import NodeAction


class Node:
    name: str
    attributes: dict[str, Union[str, bool]]
    children: list[Node]

    def __init__(self, **kwargs) -> None:
        self.name = kwargs.get("name") or ""
        self.attributes = kwargs.get("attributes") or {}
        self.children = kwargs.get("children") or []

    def action(self, action: type[NodeAction]) -> Any:
        return action(self).make()