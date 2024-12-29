from typing import Optional


class CssRule:
    name: Optional[str]
    id: Optional[str]
    class_list: list[str]

    def __init__(self, **kwargs):
        self.name = kwargs.get("name") or None
        self.id = kwargs.get("id") or None
        self.class_list = kwargs.get("class_list") or []


class CssSelectorParser:
    __rules: list[CssRule]
    __selectors: str
    __individual_selectors: list[str]

    def __init__(self, selectors) -> None:
        self.__selectors = selectors
        self.__parse_to_individual_selectors()
        self.__parse_to_rules()

    def __parse_to_individual_selectors(self) -> None:
        selectors = []

        for idx, char in enumerate(self.__selectors):
            if len(selectors) == 0:
                selectors.append(char)
                continue

            if char == "," and selectors[-1].count('"') % 2 == 0:
                selectors.append("")
                continue

            if char == " " and selectors[-1].count('"') % 2 == 0:
                continue

            selectors[-1] += char

        self.__individual_selectors = selectors

    def __parse_to_rules(self) -> None:
        pass


CssSelectorParser("li, ul")