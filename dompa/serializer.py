from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any

import dompa.nodes


class Serializer(ABC):
    @abstractmethod
    def __init__(self, nodes: list[dompa.nodes.Node]) -> None:
        self.nodes = nodes

    @abstractmethod
    def serialize(self) -> Any:
        pass
