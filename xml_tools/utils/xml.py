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
        """Parse an XML file and extract data from elements with 'name' attributes.

        Args:
            path (Path): Path to the XML file to parse

        Returns:
            tuple[ET.ElementTree, ET.Element, dict[str, dict[str, str]]]: A tuple containing:
                - The parsed XML tree
                - Root element
                - Dictionary mapping element names to their properties:
                {
                    "element_name": {
                        "text": "element text content",
                        "attributes": {"attr1": "value1", ...}
                    }
                }

        Note:
            Only elements with 'name' attributes are included in the returned dictionary.

        """
        if not path.exists() or path.stat().st_size < BYTES:
            return None, None, {}

        try:
            # Parse XML using defusedxml for security
            tree = ElementTree.parse(str(path))  # defusedxml requires string path
            root = tree.getroot()

            # Capture all elements with a 'name' attribute
            strings = {}
            for elem in root.findall(".//*[@name]"):
                name = elem.get("name")
                if name:
                    strings[name] = {"text": elem.text or "", "attributes": dict(elem.attrib)}

        except (OSError, ET.ParseError):
            logger.exception("Failed to parse %s: ", path)
        else:
            return tree, root, strings

    @staticmethod
    def write_file(path: Path, root: ET.Element) -> None:
        """Write an XML element tree to a file.

        Args:
            path (Path): Output file path
            root (ET.Element): Root element to write
            pretty_print (bool): Whether to format the output with proper indentation

        Note:
            - Creates parent directories if they don't exist
            - Uses 4-space indentation when pretty_print is True
            - Writes in UTF-8 encoding with XML declaration

        """
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            tree = ET.ElementTree(root)
            ET.indent(tree, space="    ")  # Set indentation to 4 spaces
            tree.write(path, encoding="utf-8", xml_declaration=True)
        except OSError:
            logger.exception("Failed to write %s: ", path)
