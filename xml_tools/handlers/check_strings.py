"""Check missing strings keys."""

import logging
import re
from pathlib import Path

from config.settings import Settings

logger = logging.getLogger("xml_tools")
BLACKLIST: set[str] = {
    # Not merged from RVX
    "revanced_swipe_overlay_alternative_ui_summary_off",
    "revanced_swipe_overlay_alternative_ui_summary_on",
    "revanced_swipe_overlay_alternative_ui_title",
    # Removed
    "revanced_preference_category_po_token_visitor_data",
    "revanced_spoof_streaming_data_po_token_summary",
    "revanced_spoof_streaming_data_po_token_title",
    "revanced_spoof_streaming_data_po_token_visitor_data_about_summary",
    "revanced_spoof_streaming_data_po_token_visitor_data_about_title",
    "revanced_spoof_streaming_data_visitor_data_summary",
    "revanced_spoof_streaming_data_visitor_data_title",
}


def extract_keys(path: Path) -> set[str]:
    """Extract keys from XML file.

    Args:
        path: Path to XML file

    Returns:
        set of extracted keys

    """
    key_pattern = re.compile(r'name="(\w+)"')
    keys_found: set[str] = set()

    with path.open(encoding="utf-8") as file:
        for line in file:
            matches = key_pattern.findall(line)
            keys_found.update(matches)

    return keys_found


def process(app: str, base_dir: Path) -> None:
    """Process prefs files to find missing keys.

    Args:
        app: Application name (youtube/music)
        base_dir: Base directory of RVX patches operations

    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")

    # Define file paths using base_dir
    prefs_path_1 = base_dir / "src/main/resources" / app / "settings/host/values/strings.xml"
    prefs_path_2 = base_path / "host/values/strings.xml"

    # Extract keys from both files
    keys_1 = extract_keys(prefs_path_1)
    keys_2 = extract_keys(prefs_path_2)

    # Find missing keys
    missing_keys = keys_1 - keys_2 - BLACKLIST

    # Log results
    if missing_keys:
        logger.info("Missing keys found:")
        for key in sorted(missing_keys):
            logger.info(key)
    else:
        logger.info("No missing keys found")
