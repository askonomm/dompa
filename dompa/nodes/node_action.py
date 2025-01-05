from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any

import dompa.nodes


class NodeAction(ABC):
    @abstractmethod
    def __init__(self, instance: dompa.nodes.Node):
        self.instance = instance

    @abstractmethod
    def make(self) -> Any:
        pass