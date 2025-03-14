"""XML Processor."""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING
from xml.etree import ElementTree as ET

from defusedxml import ElementTree

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")
BYTES: int = 2


class XMLProcessor:
    """Utilities for processing XML files.

    This class provides static methods for parsing and writing XML files,
    with special handling for elements containing 'name' attributes.
    Uses defusedxml for secure XML processing.
    """

    @staticmethod
    def parse_file(
        path: Path,
    ) -> tuple[ET.ElementTree | None, ET.Element | None, dict[str, dict[str, str]]]:
        """Parse an XML file and extract data from elements with 'name' attributes."""
        if not path.exists() or path.stat().st_size < BYTES:
            return None, None, {}

        try:
            # Parse XML using defusedxml for security
            tree = ElementTree.parse(str(path))  # type: ignore[reportUnknownMemberType]
            root = tree.getroot()

            strings: dict[str, dict[str, str]] = {}
            for elem in root.findall(".//*[@name]"):
                name = elem.get("name")
                if name:
                    attributes = dict(elem.attrib)
                    attributes["text"] = XMLProcessor.element_to_string(elem)
                    strings[name] = attributes

        except (OSError, ElementTree.ParseError):
            logger.exception("Failed to parse %s: ", path)
            return None, None, {}
        else:
            return tree, root, strings

    @staticmethod
    def element_to_string(element: ET.Element) -> str:
        """Convert an element to its string representation, preserving inner tags."""
        return ET.tostring(element, encoding="unicode", method="xml")

    @staticmethod
    def write_file(path: Path, root: ET.Element) -> None:
        """Write an XML element tree to a file."""
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            tree = ET.ElementTree(root)
            ET.indent(tree, space="    ")
            with path.open("wb") as f:
                tree.write(f, encoding="utf-8", xml_declaration=True)
                f.write(b"\n")
        except OSError:
            logger.exception("Failed to write file: %s", path)
