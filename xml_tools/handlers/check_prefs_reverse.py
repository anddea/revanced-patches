"""Check missing prefs keys."""

import logging
import re
from pathlib import Path

from config.settings import Settings

logger = logging.getLogger("xml_tools")
BLACKLIST: set[str] = {
    "revanced_bypass_image_region_restrictions_domain",
    "revanced_enable_cairo_seekbar",
    "revanced_enable_swipe_speed",
    "revanced_external_downloader_package_name_video_long_press",
    "revanced_gms_show_dialog",
    "revanced_gradient_seekbar_colors",
    "revanced_gradient_seekbar_positions",
    "revanced_hide_shorts_comments_disabled_button",
    "revanced_shorts_custom_actions_speed",
    "revanced_swipe_speed_sensitivity",
}


def extract_keys(path: Path) -> set[str]:
    """Extract keys from XML file.

    Args:
        path: Path to XML file

    Returns:
        set of extracted keys

    """
    try:
        key_pattern = re.compile(r'android:key="(\w+)"')  # Compile the regex pattern to match keys
        keys_found = set()  # Use a set to store unique keys

        # Open the XML file and search for the keys
        with path.open(encoding="utf-8") as file:
            for line in file:
                matches = key_pattern.findall(line)  # Find all keys in the line
                keys_found.update(matches)  # Add found keys to the set

    except FileNotFoundError:
        logger.exception("Failed to extract keys from %s: ", path)
    else:
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
    prefs_path_1 = base_path / "xml/revanced_prefs.xml"
    prefs_path_2 = base_dir / "src/main/resources/youtube/settings/xml/revanced_prefs.xml"

    try:
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

    except Exception:
        logger.exception("Failed to process preference files: ")
