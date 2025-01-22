"""Check strings in destination."""

import logging
from pathlib import Path

from defusedxml import lxml
from lxml import etree

from config.settings import Settings
from utils.xml_processor import XMLProcessor

logger = logging.getLogger("xml_tools")


def sort_file(path: Path) -> None:
    """Sort strings in XML file alphabetically.

    Args:
        path: Path to XML file to sort

    """
    try:
        # Use defusedxml for parsing
        _, _, strings = XMLProcessor.parse_file(path)

        # Create new root with sorted strings using lxml
        new_root = etree.Element("resources")  # Use etree.Element from lxml
        for name in sorted(strings.keys()):
            data = strings[name]
            # Parse the string representation into an element using defusedxml
            # Use lxml.fromstring (which is patched by defusedxml)
            string_elem = lxml.fromstring(data["text"].encode())  # encode to bytes
            new_root.append(string_elem)

        # Use lxml-based write_file for pretty-printing
        XMLProcessor.write_file(path, new_root)
        logger.info("Sorted strings in %s", path)

    except Exception:
        logger.exception("Failed to sort %s: ", path)


def process(app: str) -> None:
    """Process all files for an app.

    Args:
        app: Application name (youtube/music)

    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")
    translations = settings.get_resource_path(app, "translations")

    # Sort main strings file
    sort_file(base_path / "host/values/strings.xml")

    # Sort translation files
    for lang_dir in translations.iterdir():
        if lang_dir.is_dir():
            sort_file(lang_dir / "strings.xml")
