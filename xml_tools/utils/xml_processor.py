"""XML Processor."""

import logging
from pathlib import Path
from xml.etree import ElementTree as ET

from defusedxml import ElementTree

logger = logging.getLogger("xml_tools")
BYTES: int = 2


class XMLProcessor:
    """Utilities for processing XML files.

    This class provides static methods for parsing and writing XML files,
    with special handling for elements containing 'name' attributes.
    Uses defusedxml for secure XML processing.
    """

    @staticmethod
    def parse_file(path: Path) -> tuple[ET.ElementTree, ET.Element, dict[str, dict[str, str]]]:
        """Parse an XML file and extract data from elements with 'name' attributes."""
        if not path.exists() or path.stat().st_size < BYTES:
            return None, None, {}

        try:
            # Parse XML using defusedxml for security
            tree = ElementTree.parse(str(path))  # defusedxml requires string path
            root = tree.getroot()

            strings = {}
            for elem in root.findall(".//*[@name]"):
                name = elem.get("name")
                if name:
                    strings[name] = {
                        "text": XMLProcessor.element_to_string(elem),
                        "attributes": dict(elem.attrib),
                    }

        except (OSError, ET.ParseError):
            logger.exception("Failed to parse %s: ", path)
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
            with path.open(mode="wb") as f:
                tree.write(f, encoding="utf-8", xml_declaration=True)
                f.write(b"\n")
        except OSError:
            logger.exception("Failed to write %s: ", path)
