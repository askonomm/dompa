from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any

import dompa.nodes


class Serializer(ABC):
    @abstractmethod
    def __init__(self, node: dompa.nodes.Node):
        self.node = node

    @abstractmethod
    def serialize(self) -> Any:
        pass
