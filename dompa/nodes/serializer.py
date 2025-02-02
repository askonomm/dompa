from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any, Generic

import dompa.nodes
from dompa.types import T


class Serializer(Generic[T], ABC):
    @abstractmethod
    def __init__(self, node: dompa.nodes.Node):
        self.node = node

    @abstractmethod
    def serialize(self) -> T:
        pass
