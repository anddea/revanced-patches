"""Check missing strings keys."""

from __future__ import annotations

import logging
import re
from typing import TYPE_CHECKING

from config.settings import Settings

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")
BLACKLIST: set[str] = {
    "revanced_custom_seekbar_color_invalid_toast",
    "revanced_extended_settings_search_hint",
    "revanced_extended_settings_search_history_summary_off",
    "revanced_extended_settings_search_history_summary_on",
    "revanced_extended_settings_search_history_title",
    "revanced_extended_settings_search_no_results_summary",
    "revanced_extended_settings_search_no_results_title",
    "revanced_extended_settings_search_remove_message",
    "revanced_preference_category_po_token_visitor_data",
    "revanced_sb_share_copy_settings_success",
    "revanced_spoof_streaming_data_po_token_summary",
    "revanced_spoof_streaming_data_po_token_title",
    "revanced_spoof_streaming_data_po_token_visitor_data_about_summary",
    "revanced_spoof_streaming_data_po_token_visitor_data_about_title",
    "revanced_spoof_streaming_data_visitor_data_summary",
    "revanced_spoof_streaming_data_visitor_data_title",
    "revanced_swipe_lowest_value_auto_brightness_overlay_text",
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
        logger.info("❌  Missing keys found:")
        for key in sorted(missing_keys):
            logger.info(key)
    else:
        logger.info("✅  No missing keys found")
