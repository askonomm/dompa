from typing import Union, Optional
from .nodes import TextNode, Node


class QueryEngine:
    __query_nodes: list[Union[TextNode, Node]]

    def __init__(self, nodes: list[Union[TextNode, Node]]):
        self.__query_nodes = nodes

    def query_selector(self, selectors: str) -> Optional[Union[TextNode, Node]]:
        if len(self.__query_nodes) == 0:
            return None

        return self.__query_nodes[0]

    def query_selectors(self, selectors: str) -> list[Union[TextNode, Node]]:
        return self.__query_nodes