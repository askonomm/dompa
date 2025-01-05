from ..node_action import NodeAction
from .. import Node
from ...utils import recur_to_html


class ToHtml(NodeAction):
    __node: Node

    def __init__(self, instance: Node):
        self.__node = instance

    def make(self) -> str:
        """
        Transform the node tree into a HTML string.
        """
        return recur_to_html([self.__node])
