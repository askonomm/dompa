from dompa.nodes.serializers import ToHtml
from dompa.nodes import Node, TextNode, VoidNode, FragmentNode


def test_node_html_equality():
    assert Node(name="div").serialize(ToHtml) == "<div></div>"


def test_text_node_html_equality():
    assert TextNode(value="Hello, World").serialize(ToHtml) == "Hello, World"


def test_void_node_html_equality():
    assert VoidNode(name="img").serialize(ToHtml) == "<img>"


def test_fragment_node_html_equality():
    node = FragmentNode(children=[Node(name="div"), TextNode(value="Test"), VoidNode(name="img")])

    assert node.serialize(ToHtml) == "<div></div>Test<img>"
