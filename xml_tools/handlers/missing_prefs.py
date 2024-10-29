from pathlib import Path
from typing import Set
import re
import logging

from config.settings import Settings
from core.exceptions import XMLProcessingError

logger = logging.getLogger("xml_tools")


def extract_keys(path: Path) -> Set[str]:
    """Extract keys from XML file.

    Args:
        path: Path to XML file

    Returns:
        Set of extracted keys

    Raises:
        XMLProcessingError: If parsing fails
    """
    try:
        key_pattern = re.compile(r'android:key="(\w+)"')  # Compile the regex pattern to match keys
        keys_found = set()  # Use a set to store unique keys

        # Open the XML file and search for the keys
        with open(path, "r", encoding="utf-8") as file:
            for line in file:
                matches = key_pattern.findall(line)  # Find all keys in the line
                keys_found.update(matches)  # Add found keys to the set

        return keys_found
    except Exception as e:
        logger.error(f"Failed to extract keys from {path}: {e}")
        raise XMLProcessingError(f"Failed to extract keys from {path}: {e}")


def process(app: str, base_dir: Path) -> None:
    """Process prefs files to find missing keys.

    Args:
        app: Application name (youtube/music)
        base_dir: Base directory of RVX patches operations
    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")

    # Define file paths using base_dir
    prefs_path_1 = base_dir / "src/main/resources/youtube/settings/xml/revanced_prefs.xml"
    prefs_path_2 = base_path / "xml/revanced_prefs.xml"

    try:
        # Extract keys from both files
        keys_1 = extract_keys(prefs_path_1)
        keys_2 = extract_keys(prefs_path_2)

        # Find missing keys
        missing_keys = keys_1 - keys_2

        # Log results
        if missing_keys:
            logger.info("Missing keys found:")
            for key in sorted(missing_keys):
                logger.info(key)
        else:
            logger.info("No missing keys found")

    except XMLProcessingError as e:
        logger.error(f"Failed to process preference files: {e}")
        raise
