import dompa
from ..dompa_action import DompaAction
from ..nodes import Node
from ..utils import recur_to_html


class ToHtml(DompaAction):
    __nodes: list[Node] = []

    def __init__(self, instance: dompa.Dompa):
        self.__nodes = instance.get_nodes()

    def make(self) -> str:
        """
        Transform the node tree into a HTML string.
        """
        return recur_to_html(self.__nodes)
