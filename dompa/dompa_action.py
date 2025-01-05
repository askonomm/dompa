from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any

import dompa


class DompaAction(ABC):
    @abstractmethod
    def __init__(self, instance: dompa.Dompa) -> None:
        self.instance = instance

    @abstractmethod
    def make(self) -> Any:
        pass