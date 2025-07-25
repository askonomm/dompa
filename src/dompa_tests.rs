use crate::dompa::*;
use std::collections::{BTreeMap};

#[test]
fn test_nodes_simple_text() {
    let html = "Hello, world!".to_string();
    let result = nodes(html);

    assert_eq!(result.len(), 1);
    assert!(matches!(result[0], Node::Text(_)));

    if let Node::Text(text_node) = &result[0] {
        assert_eq!(text_node.value, "Hello, world!");
    }
}

#[test]
fn test_nodes_simple_tag() {
    let html = "<div>Hello</div>".to_string();
    let result = nodes(html);

    assert_eq!(result.len(), 1);
    assert!(matches!(result[0], Node::Block(_)));

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.name, "div");
        assert_eq!(block_node.attributes.len(), 0);
        assert_eq!(block_node.children.len(), 1);

        if let Node::Text(text_node) = &block_node.children[0] {
            assert_eq!(text_node.value, "Hello");
        } else {
            panic!("Expected child to be a text node");
        }
    }
}

#[test]
fn test_nodes_void_tag() {
    let html = "<img src=\"test.jpg\" alt=\"Test\">".to_string();
    let result = nodes(html);

    assert_eq!(result.len(), 1);
    assert!(matches!(result[0], Node::Void(_)));

    if let Node::Void(void_node) = &result[0] {
        assert_eq!(void_node.name, "img");
        assert_eq!(void_node.attributes.len(), 2);

        assert_eq!(
            void_node.attributes.get("src"),
            Some(&NodeAttrVal::String("test.jpg".to_string()))
        );

        assert_eq!(
            void_node.attributes.get("alt"),
            Some(&NodeAttrVal::String("Test".to_string()))
        );
    }
}

#[test]
fn test_nodes_with_attributes() {
    let html = "<div class=\"container\" id=\"main\">Content</div>".to_string();
    let result = nodes(html);

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.attributes.len(), 2);

        assert_eq!(
            block_node.attributes.get("class"),
            Some(&NodeAttrVal::String("container".to_string()))
        );

        assert_eq!(
            block_node.attributes.get("id"),
            Some(&NodeAttrVal::String("main".to_string()))
        );
    } else {
        panic!("Expected a block node");
    }
}

#[test]
fn test_nodes_boolean_attribute() {
    let html = "<button disabled>Click me</button>".to_string();
    let result = nodes(html);

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.attributes.len(), 1);

        assert_eq!(
            block_node.attributes.get("disabled"),
            Some(&NodeAttrVal::True)
        );
    } else {
        panic!("Expected a block node");
    }
}

#[test]
fn test_nodes_nested_tags() {
    let html = "<div><p>Paragraph</p><span>Span</span></div>".to_string();
    let result = nodes(html);

    assert_eq!(result.len(), 1);

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.name, "div");
        assert_eq!(block_node.children.len(), 2);

        if let Node::Block(p_node) = &block_node.children[0] {
            assert_eq!(p_node.name, "p");
            assert_eq!(p_node.children.len(), 1);

            if let Node::Text(text_node) = &p_node.children[0] {
                assert_eq!(text_node.value, "Paragraph");
            }
        } else {
            panic!("Expected first child to be a p block node");
        }

        if let Node::Block(span_node) = &block_node.children[1] {
            assert_eq!(span_node.name, "span");
            assert_eq!(span_node.children.len(), 1);

            if let Node::Text(text_node) = &span_node.children[0] {
                assert_eq!(text_node.value, "Span");
            }
        } else {
            panic!("Expected second child to be a span block node");
        }
    }
}

#[test]
fn test_nodes_mixed_content() {
    let html = "<div>Text before <span>inside</span> text after</div>".to_string();
    let result = nodes(html);

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.children.len(), 3);

        if let Node::Text(text_node) = &block_node.children[0] {
            assert_eq!(text_node.value, "Text before ");
        } else {
            panic!("Expected first child to be a text node");
        }

        assert!(matches!(block_node.children[1], Node::Block(_)));

        if let Node::Text(text_node) = &block_node.children[2] {
            assert_eq!(text_node.value, " text after");
        } else {
            panic!("Expected third child to be a text node");
        }
    }
}

#[test]
fn test_nodes_multiple_top_level() {
    let html = "<div>First</div><p>Second</p>".to_string();
    let result = nodes(html);

    assert_eq!(result.len(), 2);
    assert!(matches!(result[0], Node::Block(_)));
    assert!(matches!(result[1], Node::Block(_)));

    if let Node::Block(block_node) = &result[0] {
        assert_eq!(block_node.name, "div");
    }

    if let Node::Block(block_node) = &result[1] {
        assert_eq!(block_node.name, "p");
    }
}

#[test]
fn test_traverse_modify_all_nodes() {
    let html = "<div><p>Text</p><img src=\"test.jpg\"></div>".to_string();
    let parsed_nodes = nodes(html);

    // wrap all nodes in a span
    let transform = |node: &Node| -> Option<Node> {
        match node {
            Node::Block(block) => {
                let mut attrs = BTreeMap::new();

                attrs.insert("data-transformed".to_string(), NodeAttrVal::True);

                Some(Node::block(
                    block.name.clone(),
                    attrs,
                    block.children.clone(),
                ))
            }
            Node::Text(text) => Some(Node::text(format!("TRANSFORMED: {}", text.value))),
            Node::Void(void) => {
                let mut attrs = BTreeMap::new();

                attrs.insert("data-transformed".to_string(), NodeAttrVal::True);

                for (k, v) in &void.attributes {
                    attrs.insert(k.clone(), v.clone());
                }

                Some(Node::void(void.name.clone(), attrs))
            }
            Node::Fragment(_) => Some(node.clone()),
        }
    };

    let result = traverse(parsed_nodes, transform);

    // Check that all nodes have been transformed
    fn check_transformed(nodes: &[Node]) {
        for node in nodes {
            match node {
                Node::Block(block) => {
                    assert!(block.attributes.contains_key("data-transformed"));
                    check_transformed(&block.children);
                }
                Node::Text(text) => {
                    assert!(text.value.starts_with("TRANSFORMED: "));
                }
                Node::Void(void) => {
                    assert!(void.attributes.contains_key("data-transformed"));
                }
                Node::Fragment(fragment) => {
                    check_transformed(&fragment.children);
                }
            }
        }
    }

    check_transformed(&result);
}

#[test]
fn test_traverse_filter_nodes() {
    let html = "<div><p>Keep</p><span>Remove</span><img src=\"keep.jpg\"><br></div>".to_string();
    let parsed_nodes = nodes(html);

    // remove span and br nodes
    let filter = |node: &Node| -> Option<Node> {
        match node {
            Node::Block(block) if block.name == "span" => None,
            Node::Void(void) if void.name == "br" => None,
            _ => Some(node.clone()),
        }
    };

    let result = traverse(parsed_nodes, filter);

    // verify that spans and br nodes are removed
    fn check_filtered(nodes: &[Node]) -> bool {
        for node in nodes {
            match node {
                Node::Block(block) => {
                    if block.name == "span" {
                        return false;
                    }
                    if !check_filtered(&block.children) {
                        return false;
                    }
                }

                Node::Void(void) => {
                    if void.name == "br" {
                        return false;
                    }
                }

                Node::Fragment(fragment) => {
                    if !check_filtered(&fragment.children) {
                        return false;
                    }
                }
                _ => {}
            }
        }
        true
    }

    assert!(check_filtered(&result));
}

#[test]
fn test_traverse_fragment_replacement() {
    let html = "<div><p>Original</p></div>".to_string();
    let parsed_nodes = nodes(html);

    // Replace p with two spans in a fragment
    let replace_with_fragment = |node: &Node| -> Option<Node> {
        match node {
            Node::Block(block) if block.name == "p" => {
                let span1 = Node::simple_block("span", vec![Node::text("First")]);
                let span2 = Node::simple_block("span", vec![Node::text("Second")]);

                Some(Node::fragment(vec![span1, span2]))
            }
            _ => Some(node.clone()),
        }
    };

    let result = traverse(parsed_nodes, replace_with_fragment);

    // Check that p was replaced with two spans
    if let Node::Block(div) = &result[0] {
        assert_eq!(div.children.len(), 2);

        if let Node::Block(span1) = &div.children[0] {
            assert_eq!(span1.name, "span");

            if let Node::Text(text) = &span1.children[0] {
                assert_eq!(text.value, "First");
            }
        } else {
            panic!("Expected first child to be a span block");
        }

        if let Node::Block(span2) = &div.children[1] {
            assert_eq!(span2.name, "span");

            if let Node::Text(text) = &span2.children[0] {
                assert_eq!(text.value, "Second");
            }
        } else {
            panic!("Expected second child to be a span block");
        }
    }
}

#[test]
fn test_constructor_methods() {
    let text_node = Node::text("Hello");
    let simple_block = Node::simple_block("div", vec![text_node.clone()]);
    let simple_void = Node::simple_void("img");

    // Test with attributes
    let mut attrs = BTreeMap::new();

    attrs.insert(
        "class".to_string(),
        NodeAttrVal::String("container".to_string()),
    );

    let block_with_attrs = Node::block("div", attrs.clone(), vec![]);
    let void_with_attrs = Node::void("img", attrs);

    // Validate the nodes
    if let Node::Text(text) = text_node {
        assert_eq!(text.value, "Hello");
    } else {
        panic!("Expected a text node");
    }

    if let Node::Block(block) = simple_block {
        assert_eq!(block.name, "div");
        assert_eq!(block.attributes.len(), 0);
        assert_eq!(block.children.len(), 1);
    } else {
        panic!("Expected a block node");
    }

    if let Node::Void(void) = simple_void {
        assert_eq!(void.name, "img");
        assert_eq!(void.attributes.len(), 0);
    } else {
        panic!("Expected a void node");
    }

    if let Node::Block(block) = block_with_attrs {
        assert_eq!(block.name, "div");
        assert_eq!(block.attributes.len(), 1);

        assert_eq!(
            block.attributes.get("class"),
            Some(&NodeAttrVal::String("container".to_string()))
        );
    } else {
        panic!("Expected a block node");
    }

    if let Node::Void(void) = void_with_attrs {
        assert_eq!(void.name, "img");
        assert_eq!(void.attributes.len(), 1);

        assert_eq!(
            void.attributes.get("class"),
            Some(&NodeAttrVal::String("container".to_string()))
        );
    } else {
        panic!("Expected a void node");
    }
}

#[test]
fn test_to_html_simple_text_node() {
    let nodes = vec![Node::text("Hello, world!")];

    assert_eq!(to_html(nodes), "Hello, world!");
}

#[test]
fn test_to_html_simple_void_node() {
    let nodes = vec![Node::simple_void("br")];

    assert_eq!(to_html(nodes), "<br>");
}

#[test]
fn test_to_html_void_node_with_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert(
        "src".to_string(),
        NodeAttrVal::String("image.jpg".to_string()),
    );

    attrs.insert(
        "alt".to_string(),
        NodeAttrVal::String("An image".to_string()),
    );

    let nodes = vec![Node::void("img", attrs)];

    assert_eq!(to_html(nodes), "<img alt=\"An image\" src=\"image.jpg\">");
}

#[test]
fn test_to_html_simple_block_node() {
    let nodes = vec![Node::simple_block("div", vec![Node::text("Content")])];

    assert_eq!(to_html(nodes), "<div>Content</div>");
}

#[test]
fn test_to_html_block_node_with_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert(
        "class".to_string(),
        NodeAttrVal::String("container".to_string()),
    );
    attrs.insert("id".to_string(), NodeAttrVal::String("main".to_string()));

    let nodes = vec![Node::block("div", attrs, vec![Node::text("Content")])];

    assert_eq!(
        to_html(nodes),
        "<div class=\"container\" id=\"main\">Content</div>"
    );
}

#[test]
fn test_to_html_nested_block_nodes() {
    let nodes = vec![Node::simple_block(
        "section",
        vec![
            Node::simple_block("h1", vec![Node::text("Title")]),
            Node::simple_block("p", vec![Node::text("Paragraph")]),
        ],
    )];

    assert_eq!(
        to_html(nodes),
        "<section><h1>Title</h1><p>Paragraph</p></section>"
    );
}

#[test]
fn test_to_html_fragment_node() {
    let nodes = vec![Node::fragment(vec![
        Node::text("Text before"),
        Node::simple_void("br"),
        Node::text("Text after"),
    ])];

    assert_eq!(to_html(nodes), "Text before<br>Text after");
}

#[test]
fn test_to_html_mixed_node_types() {
    let mut attrs = BTreeMap::new();

    attrs.insert("class".to_string(), NodeAttrVal::String("btn".to_string()));

    attrs.insert("disabled".to_string(), NodeAttrVal::True);

    let nodes = vec![Node::simple_block(
        "div",
        vec![
            Node::text("Hello "),
            Node::block("span", attrs.clone(), vec![Node::text("World")]),
            Node::text("!"),
            Node::simple_void("br"),
            Node::simple_block("p", vec![Node::text("Paragraph")]),
        ],
    )];

    assert_eq!(
        to_html(nodes),
        "<div>Hello <span class=\"btn\" disabled>World</span>!<br><p>Paragraph</p></div>"
    );
}

#[test]
fn test_to_html_empty_nodes() {
    let nodes = vec![];

    assert_eq!(to_html(nodes), "");
}

#[test]
fn test_to_html_multiple_root_nodes() {
    let nodes = vec![
        Node::simple_block("header", vec![Node::text("Header")]),
        Node::simple_block("main", vec![Node::text("Content")]),
        Node::simple_block("footer", vec![Node::text("Footer")]),
    ];

    assert_eq!(
        to_html(nodes),
        "<header>Header</header><main>Content</main><footer>Footer</footer>"
    );
}

#[test]
fn test_to_html_boolean_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert("required".to_string(), NodeAttrVal::True);
    attrs.insert("readonly".to_string(), NodeAttrVal::True);

    let nodes = vec![Node::void("input", attrs)];

    assert_eq!(to_html(nodes), "<input readonly required>");
}

#[test]
fn test_to_html_complex_html_structure() {
    let mut form_attrs = BTreeMap::new();

    form_attrs.insert(
        "action".to_string(),
        NodeAttrVal::String("/submit".to_string()),
    );

    form_attrs.insert(
        "method".to_string(),
        NodeAttrVal::String("post".to_string()),
    );

    let mut input_attrs = BTreeMap::new();

    input_attrs.insert("type".to_string(), NodeAttrVal::String("text".to_string()));

    input_attrs.insert(
        "name".to_string(),
        NodeAttrVal::String("username".to_string()),
    );

    input_attrs.insert("required".to_string(), NodeAttrVal::True);

    let mut button_attrs = BTreeMap::new();

    button_attrs.insert(
        "type".to_string(),
        NodeAttrVal::String("submit".to_string()),
    );

    button_attrs.insert(
        "class".to_string(),
        NodeAttrVal::String("btn primary".to_string()),
    );

    let nodes = vec![Node::block(
        "form",
        form_attrs,
        vec![
            Node::simple_block("label", vec![Node::text("Username:")]),
            Node::void("input", input_attrs),
            Node::simple_void("br"),
            Node::block("button", button_attrs, vec![Node::text("Submit")]),
        ],
    )];

    assert_eq!(
        to_html(nodes),
        "<form action=\"/submit\" method=\"post\"><label>Username:</label><input name=\"username\" required type=\"text\"><br><button class=\"btn primary\" type=\"submit\">Submit</button></form>"
    );
}

#[test]
fn test_to_json_simple_text_node() {
    let nodes = vec![Node::text("Hello, world!")];

    assert_eq!(to_json(nodes), "[{\"value\":\"Hello, world!\"}]");
}


#[test]
fn test_to_json_simple_void_node() {
    let nodes = vec![Node::simple_void("br")];

    assert_eq!(to_json(nodes), "[{\"name\":\"br\",\"attributes\":{}}]");
}

#[test]
fn test_to_json_void_node_with_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert(
        "src".to_string(),
        NodeAttrVal::String("image.jpg".to_string()),
    );

    attrs.insert(
        "alt".to_string(),
        NodeAttrVal::String("An image".to_string()),
    );

    let nodes = vec![Node::void("img", attrs)];

    assert_eq!(to_json(nodes), "[{\"name\":\"img\",\"attributes\":{\"alt\":\"An image\",\"src\":\"image.jpg\"}}]");
}

#[test]
fn test_to_json_simple_block_node() {
    let nodes = vec![Node::simple_block("div", vec![Node::text("Content")])];

    assert_eq!(to_json(nodes), "[{\"name\":\"div\",\"attributes\":{},\"children\":[{\"value\":\"Content\"}]}]");
}

#[test]
fn test_to_json_block_node_with_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert(
        "class".to_string(),
        NodeAttrVal::String("container".to_string()),
    );
    attrs.insert("id".to_string(), NodeAttrVal::String("main".to_string()));

    let nodes = vec![Node::block("div", attrs, vec![Node::text("Content")])];

    assert_eq!(
        to_json(nodes),
        "[{\"name\":\"div\",\"attributes\":{\"class\":\"container\",\"id\":\"main\"},\"children\":[{\"value\":\"Content\"}]}]"
    );
}

#[test]
fn test_to_json_nested_block_nodes() {
    let nodes = vec![Node::simple_block(
        "section",
        vec![
            Node::simple_block("h1", vec![Node::text("Title")]),
            Node::simple_block("p", vec![Node::text("Paragraph")]),
        ],
    )];

    assert_eq!(
        to_json(nodes),
        "[{\"name\":\"section\",\"attributes\":{},\"children\":[{\"name\":\"h1\",\"attributes\":{},\"children\":[{\"value\":\"Title\"}]},{\"name\":\"p\",\"attributes\":{},\"children\":[{\"value\":\"Paragraph\"}]}]}]"
    );
}

#[test]
fn test_to_json_fragment_node() {
    let nodes = vec![Node::fragment(vec![
        Node::text("Text before"),
        Node::simple_void("br"),
        Node::text("Text after"),
    ])];

    assert_eq!(to_json(nodes), "[{\"children\":[{\"value\":\"Text before\"},{\"name\":\"br\",\"attributes\":{}},{\"value\":\"Text after\"}]}]");
}

#[test]
fn test_to_json_mixed_node_types() {
    let mut attrs = BTreeMap::new();

    attrs.insert("class".to_string(), NodeAttrVal::String("btn".to_string()));

    attrs.insert("disabled".to_string(), NodeAttrVal::True);

    let nodes = vec![Node::simple_block(
        "div",
        vec![
            Node::text("Hello "),
            Node::block("span", attrs.clone(), vec![Node::text("World")]),
            Node::text("!"),
            Node::simple_void("br"),
            Node::simple_block("p", vec![Node::text("Paragraph")]),
        ],
    )];

    assert_eq!(
        to_json(nodes),
        "[{\"name\":\"div\",\"attributes\":{},\"children\":[{\"value\":\"Hello \"},{\"name\":\"span\",\"attributes\":{\"class\":\"btn\",\"disabled\":true},\"children\":[{\"value\":\"World\"}]},{\"value\":\"!\"},{\"name\":\"br\",\"attributes\":{}},{\"name\":\"p\",\"attributes\":{},\"children\":[{\"value\":\"Paragraph\"}]}]}]"
    );
}

#[test]
fn test_to_json_empty_nodes() {
    let nodes = vec![];

    assert_eq!(to_json(nodes), "[]");
}

#[test]
fn test_to_json_multiple_root_nodes() {
    let nodes = vec![
        Node::simple_block("header", vec![Node::text("Header")]),
        Node::simple_block("main", vec![Node::text("Content")]),
        Node::simple_block("footer", vec![Node::text("Footer")]),
    ];

    assert_eq!(
        to_json(nodes),
        "[{\"name\":\"header\",\"attributes\":{},\"children\":[{\"value\":\"Header\"}]},{\"name\":\"main\",\"attributes\":{},\"children\":[{\"value\":\"Content\"}]},{\"name\":\"footer\",\"attributes\":{},\"children\":[{\"value\":\"Footer\"}]}]"
    );
}

#[test]
fn test_to_json_boolean_attributes() {
    let mut attrs = BTreeMap::new();

    attrs.insert("required".to_string(), NodeAttrVal::True);
    attrs.insert("readonly".to_string(), NodeAttrVal::True);

    let nodes = vec![Node::void("input", attrs)];

    assert_eq!(to_json(nodes), "[{\"name\":\"input\",\"attributes\":{\"readonly\":true,\"required\":true}}]");
}

#[test]
fn test_to_json_complex_html_structure() {
    let mut form_attrs = BTreeMap::new();

    form_attrs.insert(
        "action".to_string(),
        NodeAttrVal::String("/submit".to_string()),
    );

    form_attrs.insert(
        "method".to_string(),
        NodeAttrVal::String("post".to_string()),
    );

    let mut input_attrs = BTreeMap::new();

    input_attrs.insert("type".to_string(), NodeAttrVal::String("text".to_string()));

    input_attrs.insert(
        "name".to_string(),
        NodeAttrVal::String("username".to_string()),
    );

    input_attrs.insert("required".to_string(), NodeAttrVal::True);

    let mut button_attrs = BTreeMap::new();

    button_attrs.insert(
        "type".to_string(),
        NodeAttrVal::String("submit".to_string()),
    );

    button_attrs.insert(
        "class".to_string(),
        NodeAttrVal::String("btn primary".to_string()),
    );

    let nodes = vec![Node::block(
        "form",
        form_attrs,
        vec![
            Node::simple_block("label", vec![Node::text("Username:")]),
            Node::void("input", input_attrs),
            Node::simple_void("br"),
            Node::block("button", button_attrs, vec![Node::text("Submit")]),
        ],
    )];

    assert_eq!(
        to_json(nodes),
        "[{\"name\":\"form\",\"attributes\":{\"action\":\"/submit\",\"method\":\"post\"},\"children\":[{\"name\":\"label\",\"attributes\":{},\"children\":[{\"value\":\"Username:\"}]},{\"name\":\"input\",\"attributes\":{\"name\":\"username\",\"required\":true,\"type\":\"text\"}},{\"name\":\"br\",\"attributes\":{}},{\"name\":\"button\",\"attributes\":{\"class\":\"btn primary\",\"type\":\"submit\"},\"children\":[{\"value\":\"Submit\"}]}]}]"
    );
}