from pathlib import Path
import logging
from lxml import etree as ET

from config.settings import Settings
from core.exceptions import XMLProcessingError
from utils.xml import XMLProcessor

logger = logging.getLogger("xml_tools")


def update_strings(target_path: Path, source_path: Path) -> None:
    """Update target XML file with strings from source file.

    Args:
        target_path: Path to target XML file
        source_path: Path to source XML file
    """
    try:
        # Parse source and target files
        _, target_root, target_strings = XMLProcessor.parse_file(target_path)
        _, _, source_strings = XMLProcessor.parse_file(source_path)

        # Update existing strings
        for elem in target_root.findall(".//string"):
            name = elem.get("name")
            if name in source_strings:
                data = source_strings[name]
                elem.text = data["text"]
                elem.attrib.update(data["attributes"])
                del source_strings[name]

        # Add new strings
        for name, data in sorted(source_strings.items()):
            string_elem = ET.Element("string", **data["attributes"])
            string_elem.text = data["text"]
            target_root.append(string_elem)

        # Write updated file
        XMLProcessor.write_file(target_path, target_root)
        logger.info(f"Updated strings in {target_path}")

    except Exception as e:
        logger.error(f"Failed to update strings in {target_path}: {e}")
        raise XMLProcessingError(str(e))


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

    try:
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

    except Exception as e:
        logger.error(f"Failed to process {app} translations: {e}")
        raise XMLProcessingError(str(e))
