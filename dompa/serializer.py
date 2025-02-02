from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Generic

import dompa.nodes
from dompa.types import T


class Serializer(Generic[T], ABC):
    @abstractmethod
    def __init__(self, nodes: list[dompa.nodes.Node]) -> None:
        self.nodes = nodes

    @abstractmethod
    def serialize(self) -> T:
        raise NotImplementedError()
