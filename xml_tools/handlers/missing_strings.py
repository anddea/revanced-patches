"""Find missing strings and create the file with them."""

import logging
from pathlib import Path

from lxml import etree as et

from config.settings import Settings
from utils.xml import XMLProcessor

logger = logging.getLogger("xml_tools")


def compare_and_update(source_path: Path, dest_path: Path, missing_path: Path) -> None:
    """Compare source and destination files and update missing strings.

    Args:
        source_path: Path to source XML file
        dest_path: Path to destination XML file
        missing_path: Path to missing strings file

    """
    try:
        # Parse source and destination files
        _, _, source_strings = XMLProcessor.parse_file(source_path)
        _, _, dest_strings = XMLProcessor.parse_file(dest_path)
        _, _, missing_path_strings = XMLProcessor.parse_file(missing_path)

        # Find missing strings
        missing_strings = {}
        missing_strings = {name: data for name, data in source_strings.items() if name not in dest_strings}

        if missing_strings:
            # Create new root with missing strings
            root = et.Element("resources")
            for _name, data in sorted(missing_strings.items()):
                string_elem = et.Element("string", **data["attributes"])
                string_elem.text = data["text"]
                root.append(string_elem)

            # Write missing strings file
            XMLProcessor.write_file(missing_path, root)
            logger.info("Modified missing strings file: %s", missing_path)
        elif missing_path.exists():
            missing_path.unlink()
            logger.info("Removed empty missing strings file: %s", missing_path)

    except Exception:
        logger.exception("Failed to process missing strings: ")


def process(app: str) -> None:
    """Process all files to find missing strings.

    Args:
        app: Application name (youtube/music)

    """
    settings = Settings()
    source_path = settings.get_resource_path(app, "settings") / "host/values/strings.xml"
    translations = settings.get_resource_path(app, "translations")

    try:
        for lang_dir in translations.iterdir():
            if lang_dir.is_dir():
                dest_path = lang_dir / "strings.xml"
                missing_path = lang_dir / "missing_strings.xml"
                compare_and_update(source_path, dest_path, missing_path)

    except Exception:
        logger.exception("Failed to process %s translations: ", app)
