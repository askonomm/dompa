from typing import Optional

from dompa import Dompa
from dompa.serializers import ToHtml
from dompa.nodes import TextNode, Node, FragmentNode


def test_html_equality():
    html = '<html><body>Hello</body></html>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == html


def test_html_equality2():
    html = '<!DOCTYPE html><html><body>Hello</body></html>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == html


def test_html_equality3():
    html = '<div class=\"test test2 test3\">Hello</div>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == html


def test_html_equality4():
    html = '<input type=\"radio\" checked>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == html


def test_html_equality5():
    html = 'Hello, World!'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == "Hello, World!"


def test_invalid_to_html():
    html = '<div><p>Hello</p>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == '<div></div><p>Hello</p>'


def test_invalid_html2():
    html = '<div><p>Hello</div></p>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == '<div>Hello</div><p></p>'


def test_invalid_html3():
    html = '<div><p>Hello</div></span>'
    dom = Dompa(html)

    assert dom.serialize(ToHtml) == '<div><p></p>Hello</div>'


def test_nodes():
    html = '<div>Hello, World</div>'
    dom = Dompa(html)

    assert len(dom.get_nodes()) == 1
    assert len(dom.get_nodes()[0].children) == 1
    assert isinstance(dom.get_nodes()[0], Node)
    assert isinstance(dom.get_nodes()[0].children[0], TextNode)


def test_query():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)
    result = dom.query(lambda x: x.name == "h1")

    assert len(result) == 1
    assert isinstance(result[0], Node)
    assert isinstance(result[0].children[0], TextNode)


def test_query_fragment_node():
    html = '<div><h1>Title</h1></div>'
    dom = Dompa(html)

    def replace_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return FragmentNode(children=[
                Node(name="h2", children=[TextNode(value="Hello, World!")]),
                Node(name="p", children=[TextNode(value="Some content ...")])
            ])

        return node

    dom.traverse(replace_title)

    result = dom.query(lambda x: x.name == "h2")

    assert len(result) == 1
    assert isinstance(result[0], Node)
    assert isinstance(result[0].children[0], TextNode)


def test_traverse_update_node():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)

    def update_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            node.children = [TextNode(value="Hello, World!")]

        return node

    dom.traverse(update_title)

    assert dom.serialize(ToHtml) == "<div><h1>Hello, World!</h1><p>Content</p></div>"


def test_traverse_replace_node():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)

    def update_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return Node(name="p", children=[TextNode(value="Some Paragraph")])

        return node

    dom.traverse(update_title)

    assert dom.serialize(ToHtml) == "<div><p>Some Paragraph</p><p>Content</p></div>"


def test_traverse_remove_node():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)

    def update_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return None

        return node

    dom.traverse(update_title)

    assert dom.serialize(ToHtml) == "<div><p>Content</p></div>"


def test_traverse_fragment_node():
    html = '<div><h1>Title</h1></div>'
    dom = Dompa(html)

    def replace_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return FragmentNode(children=[
                Node(name="h2", children=[TextNode(value="Hello, World!")]),
                Node(name="p", children=[TextNode(value="Some content ...")])
            ])

        return node

    dom.traverse(replace_title)

    assert dom.serialize(ToHtml) == "<div><h2>Hello, World!</h2><p>Some content ...</p></div>"
