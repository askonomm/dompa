import dompa.nodes
from ..nodes import Node
from ..serializer import Serializer
from ..utils import recur_to_html


class ToHtml(Serializer[str]):
    __nodes: list[Node] = []

    def __init__(self, nodes: list[dompa.nodes.Node]):
        self.__nodes = nodes

    def serialize(self) -> str:
        """
        Transform the node tree into a HTML string.
        """
        return recur_to_html(self.__nodes)
