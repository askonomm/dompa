use std::collections::{HashMap, HashSet};

#[derive(Debug, Clone)]
struct IrNode {
    name: String,
    coords: (usize, usize),
    children: Vec<IrNode>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum NodeAttributeValue {
    /// Represents a string HTML attribute, i.e:
    ///
    /// ```html
    /// <element attribute="value">
    /// ```
    String(String),

    /// Represents a truthy HTML attribute, i.e:
    ///
    /// ```html
    /// <element checked>
    /// ```
    True,
}

impl NodeAttributeValue {
    /// Simplifies the creation of a `NodeAttributeValue::String` variant by providing
    /// a shorthand function:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// NodeAttributeValue::string("value");
    /// ```
    ///
    /// The verbose variant looks like this:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// NodeAttributeValue::String(String::from("value"));
    /// ```
    pub fn string(value: impl Into<String>) -> Self {
        NodeAttributeValue::String(value.into())
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum Node {
    Block(BlockNode),
    Text(TextNode),
    Void(VoidNode),
    Fragment(FragmentNode),
}

#[derive(Debug, Clone, PartialEq)]
pub struct BlockNode {
    pub name: String,
    pub attributes: HashMap<String, NodeAttributeValue>,
    pub children: Vec<Node>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct TextNode {
    pub value: String,
}

#[derive(Debug, Clone, PartialEq)]
pub struct VoidNode {
    pub name: String,
    pub attributes: HashMap<String, NodeAttributeValue>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct FragmentNode {
    pub children: Vec<Node>,
}

// Adding some helper methods to the Node struct to make the usage a little
// less verbose.
impl Node {
    /// Simplifies the creation of a `BlockNode` by providing a shorthand
    /// function:
    ///
    /// ```rust
    /// use dompa::*;
    /// use std::collections::HashMap;
    ///
    /// Node::block("div", HashMap::new(), vec![]);
    /// ```
    ///
    /// The verbose variant looks like this:
    ///
    /// ```rust
    /// use dompa::*;
    /// use std::collections::HashMap;
    ///
    /// Node::Block(BlockNode {
    ///   name: String::from("div"),
    ///   attributes: HashMap::new(),
    ///   children: vec![]
    /// });
    /// ```
    pub fn block(
        name: impl Into<String>,
        attributes: HashMap<String, NodeAttributeValue>,
        children: Vec<Node>,
    ) -> Self {
        Node::Block(BlockNode {
            name: name.into(),
            attributes,
            children,
        })
    }

    /// Simplifies the simplified shorthand function (`Node::block`) even more for when
    /// you don't want to set attributes, in which case you can simply call this:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::simple_block("div", vec![]);
    /// ```
    pub fn simple_block(name: impl Into<String>, children: Vec<Node>) -> Self {
        Node::block(name, HashMap::new(), children)
    }

    /// Simplifies the creation of a `TextNode` by providing a shorthand
    /// function:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::text("Hello, World!");
    /// ```
    ///
    /// The verbose variant looks like this:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::Text(TextNode {
    ///   value: String::from("Hello, World!")
    /// });
    /// ```
    pub fn text(value: impl Into<String>) -> Self {
        Node::Text(TextNode {
            value: value.into(),
        })
    }

    /// Simplifies the creation of a `VoidNode` by providing a shorthand
    /// function:
    ///
    /// ```rust
    /// use dompa::*;
    /// use std::collections::HashMap;
    ///
    /// Node::void("img", HashMap::new());
    /// ```
    ///
    /// The verbose variant looks like this:
    ///
    /// ```rust
    /// use dompa::*;
    /// use std::collections::HashMap;
    ///
    /// Node::Void(VoidNode {
    ///   name: String::from("img"),
    ///   attributes: HashMap::new()
    /// });
    /// ```
    pub fn void(name: impl Into<String>, attributes: HashMap<String, NodeAttributeValue>) -> Self {
        Node::Void(VoidNode {
            name: name.into(),
            attributes,
        })
    }

    /// Simplifies the simplifier shrothand function (`Node::void`) even more for when
    /// you don't want to set attributes, in which case you can simply call this:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::simple_void("img");
    /// ```
    pub fn simple_void(name: impl Into<String>) -> Self {
        Node::void(name, HashMap::new())
    }

    /// Simplifies the creation of a `FragmentNode` by providing a shrothand
    /// function:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::fragment(vec![]);
    /// ```
    ///
    /// The verbose variant looks like this:
    ///
    /// ```rust
    /// use dompa::*;
    ///
    /// Node::Fragment(FragmentNode {
    ///   children: vec![]
    /// });
    /// ```
    pub fn fragment(children: Vec<Node>) -> Self {
        Node::Fragment(FragmentNode { children })
    }
}

// Nodes which are automatically parsed as self-closing.
// TODO: should this be configurable?
static VOID_NODES: [&'static str; 14] = [
    "!doctype", "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
    "source", "track", "wbr",
];

fn set_end_coord_of_last_ir_node(nodes: &mut [IrNode], name: &str, end_coord: usize) {
    if let Some(node) = nodes
        .iter_mut()
        .rev()
        .find(|node| node.name == name && node.coords.1 == 0)
    {
        node.coords = (node.coords.0, end_coord);
    }
}

fn create_ir_nodes(html: &str) -> Vec<IrNode> {
    let mut ir_nodes: Vec<IrNode> = Vec::from([]);
    let mut tag_start: Option<usize> = None;
    let mut tag_end: Option<usize> = None;
    let mut text_start: Option<usize> = None;
    let mut text_end: Option<usize> = None;

    for (i, char) in html.chars().enumerate() {
        // start of a tag or end of a text node
        if char == '<' {
            if text_start.is_some() {
                text_end = Some(i);
            }

            tag_start = Some(i);
        }
        // if we're at  the last char and it's not the end of a tag,
        // means w'ere in a text node
        else if i == html.len() - 1 && char != '>' {
            text_end = Some(i + 1);
        }
        // end of a tag
        else if char == '>' {
            tag_end = Some(i + 1);
        }

        // when we have both tag_start and tag_end, collect the tag
        if let (Some(start), Some(end)) = (tag_start, tag_end) {
            let tag = &html[start..end];
            let contents = &tag[1..tag.len() - 1];
            let contents_split: Vec<&str> = contents.split(" ").collect();
            let name = contents_split[0].to_string();
            let is_void_node = VOID_NODES.contains(&name.as_str());

            // if this is a closing tag, close the last node
            if tag.starts_with("</") {
                set_end_coord_of_last_ir_node(&mut ir_nodes, &name[1..], end);
            }
            // otherwise, business as usual, create the IrNode
            else {
                ir_nodes.push(IrNode {
                    name,
                    coords: (start, if is_void_node { end } else { 0 }),
                    children: Vec::new(),
                });
            }

            tag_start = None;
            tag_end = None;
            continue;
        }

        // no collection data, and no text_start, which means we start
        // collecting text data.
        if tag_start.is_none() && tag_end.is_none() && text_start.is_none() {
            text_start = Some(i);
        }

        // if we have all the text data, collect it
        if let (Some(start), Some(end)) = (text_start, text_end) {
            ir_nodes.push(IrNode {
                name: "_text_node".to_string(),
                coords: (start, end),
                children: Vec::new(),
            });

            text_start = None;
            text_end = None;
        }
    }

    ir_nodes
}

fn find_ir_nodes_in_coords(ir_nodes: &[IrNode], coords: (usize, usize)) -> Vec<IrNode> {
    ir_nodes
        .iter()
        .filter(|node| node.coords.0 > coords.0 && node.coords.1 < coords.1)
        .cloned()
        .collect()
}

fn join_ir_nodes_inner(nodes: &[IrNode], seen: &mut HashSet<(usize, usize)>) -> Vec<IrNode> {
    nodes
        .iter()
        .filter_map(|node| {
            // skip if we've seen it
            if !seen.insert(node.coords) {
                return None;
            }

            let children = find_ir_nodes_in_coords(nodes, node.coords);
            let processed_children = join_ir_nodes_inner(&children, seen);

            Some(IrNode {
                coords: node.coords,
                children: processed_children,
                ..node.clone()
            })
        })
        .collect()
}

fn join_ir_nodes(ir_nodes: Vec<IrNode>) -> Vec<IrNode> {
    join_ir_nodes_inner(&ir_nodes, &mut HashSet::new())
}

fn attrs_str_from_coords(html: &str, coords: (usize, usize)) -> Option<String> {
    let raw_tag = &html[coords.0..coords.1];
    let end = raw_tag.find('>')?;
    let start = raw_tag.find(|c: char| c.is_whitespace())? + 1;

    // start has to be before end, and we should actually have
    // attributes i.e start should not be equal to the tag length.
    if start >= end || start == raw_tag.len() {
        return None;
    }

    Some(raw_tag[start..end].to_string())
}

fn attrs_from_coords(html: &str, coords: (usize, usize)) -> HashMap<String, NodeAttributeValue> {
    let mut attrs = HashMap::new();

    let Some(attrs_str) = attrs_str_from_coords(&html, coords) else {
        return attrs;
    };

    let mut chars = attrs_str.chars().peekable();
    let mut current_name = String::new();

    while let Some(c) = chars.next() {
        match c {
            // skip leading whitespace
            c if c.is_whitespace() && current_name.is_empty() => continue,

            // handle whitespace after attribute name or value
            c if c.is_whitespace() => {
                if !current_name.is_empty() {
                    attrs.insert(current_name.clone(), NodeAttributeValue::True);
                    current_name.clear();
                }
            }

            // parse attribute value
            '=' => {
                if let Some('"') = chars.next() {
                    let value: String = chars.by_ref().take_while(|&c| c != '"').collect();

                    attrs.insert(current_name.clone(), NodeAttributeValue::String(value));
                    current_name.clear();

                    // skip the space after the closing quote, if present
                    if let Some(' ') = chars.peek() {
                        chars.next();
                    }
                }
            }

            // collect attribute name
            _ => current_name.push(c),
        }
    }

    // handle last attribute, if it's a boolean one
    if !current_name.is_empty() {
        attrs.insert(current_name, NodeAttributeValue::True);
    }

    attrs
}

fn create_nodes(html: &str, ir_nodes: Vec<IrNode>) -> Vec<Node> {
    ir_nodes
        .into_iter()
        .map(|ir_node| match ir_node.name.as_str() {
            "_text_node" => Node::Text(TextNode {
                value: html[ir_node.coords.0..ir_node.coords.1].to_string(),
            }),

            name if VOID_NODES.contains(&name) => Node::Void(VoidNode {
                name: ir_node.name,
                attributes: attrs_from_coords(html, ir_node.coords),
            }),

            _ => Node::Block(BlockNode {
                name: ir_node.name,
                attributes: attrs_from_coords(html, ir_node.coords),
                children: create_nodes(html, ir_node.children),
            }),
        })
        .collect()
}

/// Transforms a given `html` string into a node tree.
pub fn nodes(html: String) -> Vec<Node> {
    let ir_nodes = create_ir_nodes(&html);
    let joined_ir_nodes = join_ir_nodes(ir_nodes);

    return create_nodes(&html, joined_ir_nodes);
}

/// Traverses the given `nodes` node tree and returns an updated tree based
/// on `callable`. The `callable` must return a `Node` if you wish to either
/// replace the node or update it, and return `None` if you wish to remove it.
pub fn traverse(nodes: Vec<Node>, callable: fn(node: &Node) -> Option<Node>) -> Vec<Node> {
    let mut result = Vec::new();

    for node in nodes {
        if let Some(transformed_node) = callable(&node) {
            match transformed_node {
                Node::Block(mut block_node) => {
                    block_node.children = traverse(block_node.children, callable);
                    result.push(Node::Block(block_node));
                }

                Node::Fragment(fragment_node) => {
                    result.extend(traverse(fragment_node.children, callable));
                }

                Node::Text(_) | Node::Void(_) => {
                    result.push(transformed_node);
                }
            }
        }
    }

    result
}

fn attrs_to_html_str(attrs: HashMap<String, NodeAttributeValue>) -> String {
    let mut sorted_attrs: Vec<_> = attrs.into_iter().collect();
    sorted_attrs.sort_by(|(a, _), (b, _)| a.cmp(b));

    sorted_attrs.into_iter().fold(String::new(), |acc, attr| {
        let html = match attr {
            (key, NodeAttributeValue::True) => key,

            (key, NodeAttributeValue::String(val)) => format!("{}=\"{}\"", key, val),
        };

        format!("{} {}", acc, html).to_string()
    })
}

/// Transform the given `nodes` node tree into a HTML string.
pub fn to_html(nodes: Vec<Node>) -> String {
    nodes.into_iter().fold(String::new(), |acc, node| {
        let html = match node {
            Node::Block(block_node) => {
                format!(
                    "<{name}{attrs}>{content}</{name}>",
                    name = block_node.name,
                    attrs = attrs_to_html_str(block_node.attributes),
                    content = to_html(block_node.children),
                )
            }

            Node::Text(text_node) => text_node.value,

            Node::Void(void_node) => {
                format!(
                    "<{name}{attrs}>",
                    name = void_node.name,
                    attrs = attrs_to_html_str(void_node.attributes)
                )
            }

            Node::Fragment(fragment_node) => to_html(fragment_node.children),
        };

        format!("{}{}", acc, html)
    })
}
