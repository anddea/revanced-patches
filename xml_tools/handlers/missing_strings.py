# Copyright (C) 2026 anddea

"""Find missing strings and create the file with them."""

import logging
from pathlib import Path
from xml.etree import ElementTree as ET

from defusedxml import ElementTree as DefusedET

from config.settings import Settings
from utils.xml_processor import XMLProcessor

logger = logging.getLogger("xml_tools")

def remove_non_translatable_strings(path: Path, names: set[str]) -> None:
    """Remove translations excluded by the host's translatable attribute.

    Translation overlays may omit the attribute, so the host names are authoritative.
    """
    _, root, _ = XMLProcessor.parse_file(path)
    if root is None:
        return

    removed = [element for element in root if element.get("name") in names]
    if not removed:
        return

    for element in removed:
        root.remove(element)
    XMLProcessor.write_file(path, root)
    XMLProcessor.cleanup_if_empty(path)
    logger.debug("Removed %d non-translatable strings from %s", len(removed), path)


def compare_and_update(source_path: Path, dest_path: Path, missing_path: Path) -> None:
    """Clean non-translatable entries from translations and update missing strings.

    Args:
        source_path: Path to source XML file
        dest_path: Path to destination XML file
        missing_path: Path to missing strings file

    """
    try:
        # Parse source and destination files
        _, _, source_strings = XMLProcessor.parse_file(source_path)
        non_translatable = {name for name, data in source_strings.items() if data.get("translatable") == "false"}
        if non_translatable:
            for path in (dest_path, dest_path.parent / "forced_strings.xml", dest_path.parent / "updated_strings.xml"):
                remove_non_translatable_strings(path, non_translatable)
        _, _, dest_strings = XMLProcessor.parse_file(dest_path)

        # Find missing strings excluding those marked non-translatable in the host.
        missing_strings = {
            name: data
            for name, data in source_strings.items()
            if name not in dest_strings and name not in non_translatable
        }

        if missing_strings:
            # Create new root with missing strings
            root = ET.Element("resources")
            for _name, data in sorted(missing_strings.items()):
                string_elem = DefusedET.fromstring(data["text"])
                root.append(string_elem)

            # Write missing strings file
            XMLProcessor.write_file(missing_path, root)
            logger.debug("Modified missing strings file: %s", missing_path)
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
                updated_path = lang_dir / "updated_strings.xml"
                compare_and_update(source_path, dest_path, missing_path)
                XMLProcessor.cleanup_if_empty(missing_path)
                XMLProcessor.cleanup_if_empty(updated_path)

    except Exception:
        logger.exception("Failed to process %s translations: ", app)
