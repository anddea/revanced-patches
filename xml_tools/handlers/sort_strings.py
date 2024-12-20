"""Check strings in destination."""

import logging
from pathlib import Path

from lxml import etree as et

from config.settings import Settings
from utils.xml import XMLProcessor

logger = logging.getLogger("xml_tools")


def sort_file(path: Path) -> None:
    """Sort strings in XML file alphabetically.

    Args:
        path: Path to XML file to sort

    """
    try:
        _, root, strings = XMLProcessor.parse_file(path)

        # Create new root with sorted strings
        new_root = et.Element("resources")
        for name in sorted(strings.keys()):
            data = strings[name]
            string_elem = et.Element("string", **data["attributes"])
            string_elem.text = data["text"]
            new_root.append(string_elem)

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
