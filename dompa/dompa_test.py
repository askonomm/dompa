from typing import Optional

from dompa import Dompa
from dompa.nodes import TextNode, Node


def test_html_equality():
    html = '<html><body>Hello</body></html>'
    dom = Dompa(html)

    assert dom.html() == html


def test_html_equality2():
    html = '<!DOCTYPE html><html><body>Hello</body></html>'
    dom = Dompa(html)

    assert dom.html() == html


def test_html_equality3():
    html = '<div class=\"test test2 test3\">Hello</div>'
    dom = Dompa(html)

    assert dom.html() == html


def test_html_equality4():
    html = '<input type=\"radio\" checked>'
    dom = Dompa(html)

    assert dom.html() == html


def test_invalid_html():
    html = '<div><p>Hello</p>'
    dom = Dompa(html)

    assert dom.html() == '<div></div><p>Hello</p>'


def test_invalid_html2():
    html = '<div><p>Hello</div></p>'
    dom = Dompa(html)

    assert dom.html() == '<div>Hello</div><p></p>'


def test_invalid_html3():
    html = '<div><p>Hello</div></span>'
    dom = Dompa(html)

    assert dom.html() == '<div><p></p>Hello</div>'


def test_nodes():
    html = '<div>Hello, World</div>'
    dom = Dompa(html)

    assert len(dom.nodes()) == 1
    assert len(dom.nodes()[0].children) == 1
    assert isinstance(dom.nodes()[0], Node)
    assert isinstance(dom.nodes()[0].children[0], TextNode)


def test_query():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)
    result = dom.query(lambda x: x.name == "h1")

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

    assert dom.html() == "<div><h1>Hello, World!</h1><p>Content</p></div>"


def test_traverse_replace_node():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)

    def update_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return Node(name="p", children=[TextNode(value="Some Paragraph")])

        return node

    dom.traverse(update_title)

    assert dom.html() == "<div><p>Some Paragraph</p><p>Content</p></div>"


def test_traverse_remove_node():
    html = '<div><h1>Title</h1><p>Content</p></div>'
    dom = Dompa(html)

    def update_title(node: Node) -> Optional[Node]:
        if node.name == "h1":
            return None

        return node

    dom.traverse(update_title)

    assert dom.html() == "<div><p>Content</p></div>"
