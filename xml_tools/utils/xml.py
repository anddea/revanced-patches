from lxml import etree as ET
from typing import Dict, Tuple
from pathlib import Path
import logging
from core.exceptions import XMLProcessingError

logger = logging.getLogger("xml_tools")


class XMLProcessor:
    """
    Utilities for processing XML files.

    This class provides static methods for parsing and writing XML files,
    with special handling for elements containing 'name' attributes.
    """

    @staticmethod
    def parse_file(path: Path) -> Tuple[ET.ElementTree, ET.Element, Dict[str, Dict[str, str]]]:
        """
        Parse an XML file and extract data from elements with 'name' attributes.

        Args:
            path (Path): Path to the XML file to parse

        Returns:
            Tuple[ET.ElementTree, ET.Element, Dict[str, Dict[str, str]]]: A tuple containing:
                - The parsed XML tree
                - Root element
                - Dictionary mapping element names to their properties:
                  {
                    "element_name": {
                      "text": "element text content",
                      "attributes": {"attr1": "value1", ...}
                    }
                  }

        Raises:
            XMLProcessingError: If the file cannot be parsed or read

        Note:
            Only elements with 'name' attributes are included in the returned dictionary.
        """
        if not path.exists() or path.stat().st_size < 2:
            return None, None, {}

        try:
            tree = ET.parse(path)
            root = tree.getroot()

            # Capture all elements with a 'name' attribute
            strings = {}
            for elem in root.findall(".//*[@name]"):
                name = elem.get("name")
                if name:
                    strings[name] = {
                        "text": elem.text or "",
                        "attributes": dict(elem.attrib)
                    }

            return tree, root, strings
        except (ET.ParseError, IOError) as e:
            logger.error(f"Failed to parse {path}: {e}")
            raise XMLProcessingError(f"Failed to parse {path}: {e}")

    @staticmethod
    def write_file(path: Path, root: ET.Element, pretty_print: bool = True) -> None:
        """
        Write an XML element tree to a file.

        Args:
            path (Path): Output file path
            root (ET.Element): Root element to write
            pretty_print (bool): Whether to format the output with proper indentation

        Raises:
            XMLProcessingError: If the file cannot be written

        Note:
            - Creates parent directories if they don't exist
            - Uses 4-space indentation when pretty_print is True
            - Writes in UTF-8 encoding with XML declaration
        """
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            tree = ET.ElementTree(root)
            ET.indent(tree, space="    ")  # Set indentation to 4 spaces
            tree.write(
                path,
                encoding="utf-8",
                xml_declaration=True,
                pretty_print=pretty_print
            )
        except IOError as e:
            logger.error(f"Failed to write {path}: {e}")
            raise XMLProcessingError(f"Failed to write {path}: {e}")
