from .node import Node

class TextNode(Node):
    value: str

    def __init__(self, value: str) -> None:
        super().__init__()
        self.value = value