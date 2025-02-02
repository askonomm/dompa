from __future__ import annotations
from typing import Union, Any

from dompa.types import T

from .serializer import Serializer


class Node:
    name: str
    attributes: dict[str, Union[str, bool]]
    children: list[Node]

    def __init__(self, **kwargs) -> None:
        self.name = kwargs.get("name") or ""
        self.attributes = kwargs.get("attributes") or {}
        self.children = kwargs.get("children") or []

    def serialize(self, serializer: type[Serializer[T]]) -> T:
        """
        Serialize the node into an output created by a given
        serializer.
        """
        return serializer(self).serialize()
