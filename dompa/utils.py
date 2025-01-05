from typing import Union

from dompa.nodes import Node, TextNode, FragmentNode, VoidNode


def node_attrs_from_dict(attributes: dict[str, Union[str, bool]]) -> str:
    """
    Composes a HTML attribute string for a node from its attributes dictionary.
    """
    attr_str = ""

    for key, value in attributes.items():
        if value is True:
            attr_str += f"{key} "
        else:
            attr_str += f'{key}="{value}" '

    return attr_str.strip()


def recur_to_html(nodes: list[Node]) -> str:
    """
    Recursively iterates over the node tree to compose a HTML string output.
    """
    html = ""

    for node in nodes:
        if isinstance(node, TextNode):
            html += node.value
        elif isinstance(node, FragmentNode):
            html += recur_to_html(node.children)
        else:
            if node.attributes != {}:
                html += f"<{node.name} {node_attrs_from_dict(node.attributes)}>"
            else:
                html += f"<{node.name}>"

            if not isinstance(node, VoidNode):
                html += recur_to_html(node.children)
                html += f"</{node.name}>"

    return html
