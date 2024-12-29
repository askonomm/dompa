from __future__ import annotations
from typing import Tuple, Union


class IRNode:
    name: str
    coords: Tuple[int, int]
    children: list[Union[IRNode, str]]

    def __init__(self, name: str, coords: Tuple[int, int]) -> None:
        self.name = name
        self.coords = coords