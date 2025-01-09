from ..serializer import Serializer
from .. import Node
from ...utils import recur_to_html


class ToHtml(Serializer):
    __node: Node

    def __init__(self, node: Node):
        self.__node = node

    def serialize(self) -> str:
        """
        Transform the node tree into a HTML string.
        """
        return recur_to_html([self.__node])
