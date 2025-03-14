"""Get strings from provided source and replace strings in destination."""

import logging
from pathlib import Path

from defusedxml import ElementTree

from config.settings import Settings
from utils.xml_processor import XMLProcessor

logger = logging.getLogger("xml_tools")


def update_strings(target_path: Path, source_path: Path) -> None:
    """Update target XML file with strings from source file.

    Args:
        target_path: Path to target XML file
        source_path: Path to source XML file

    """
    try:
        # Parse source and target files
        _, target_root, _ = XMLProcessor.parse_file(target_path)
        _, source_root, source_strings = XMLProcessor.parse_file(source_path)

        if target_root is None:
            logger.error("Failed to parse target XML file: %s", target_path)
            return

        if source_root is None:
            logger.error("Failed to parse source XML file: %s", source_path)
            return

        # Create a dictionary of existing elements
        existing_elements = {elem.get("name"): elem for elem in target_root.findall(".//string")}

        # Update existing strings or add new ones
        for name, data in source_strings.items():
            if name in existing_elements:
                # Update existing element
                existing_elem = existing_elements[name]
                new_elem = ElementTree.fromstring(data["text"])  # type: ignore[reportUnknownMemberType]
                # Replace attributes and children
                existing_elem.attrib.clear()
                existing_elem.attrib.update(new_elem.attrib)
                existing_elem[:] = new_elem[:]
                existing_elem.text = new_elem.text
                existing_elem.tail = new_elem.tail
            else:
                new_elem = ElementTree.fromstring(data["text"])  # type: ignore[reportUnknownMemberType]
                target_root.append(new_elem)

        # Write updated file
        XMLProcessor.write_file(target_path, target_root)
        logger.info("Updated strings in %s", target_path)

    except Exception:
        logger.exception("Failed to update strings in %s: ", target_path)


def process(app: str, base_dir: Path) -> None:
    """Process all files to replace strings.

    Args:
        app: Application name (youtube/music)
        base_dir: Base directory of RVX patches operations

    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")
    source_path = base_path / "host/values/strings.xml"
    translations = settings.get_resource_path(app, "translations")

    # First update base strings file from RVX
    rvx_base_path = base_dir / "src/main/resources" / app
    rvx_source_path = rvx_base_path / "settings/host/values/strings.xml"
    if rvx_source_path.exists():
        update_strings(source_path, rvx_source_path)

    # Process translation files
    for lang_dir in translations.iterdir():
        if lang_dir.is_dir():
            target_path = lang_dir / "strings.xml"
            rvx_lang_path = rvx_base_path / "translations" / lang_dir.name / "strings.xml"

            if rvx_lang_path.exists():
                update_strings(target_path, rvx_lang_path)
