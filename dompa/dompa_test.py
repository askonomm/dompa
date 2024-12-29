from dompa import Dompa


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