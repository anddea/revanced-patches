"""Check missing prefs keys."""

import logging
import re
from pathlib import Path

from config.settings import Settings

logger = logging.getLogger("xml_tools")
BLACKLIST: set[str] = {
    "revanced_bypass_image_region_restrictions_domain",
    "revanced_enable_cairo_seekbar",
    "revanced_enable_swipe_seek",
    "revanced_enable_swipe_speed",
    "revanced_external_downloader_package_name_video_long_press",
    "revanced_gemini_transcribe_subtitles_font_size",
    "revanced_gms_show_dialog",
    "revanced_gradient_seekbar_colors",
    "revanced_gradient_seekbar_positions",
    "revanced_hide_shorts_comments_disabled_button",
    "revanced_overlay_button_gemini_about",
    "revanced_overlay_button_gemini_summarize_api_key",
    "revanced_overlay_button_gemini_summarize",
    "revanced_shorts_custom_actions_gemini",
    "revanced_shorts_custom_actions_speed",
    "revanced_swipe_overlay_minimal_style",
    "revanced_swipe_seek_sensitivity",
    "revanced_swipe_show_circular_overlay",
    "revanced_swipe_speed_sensitivity",
    "revanced_swipe_switch_speed_and_seek",
}


def extract_keys(path: Path) -> set[str]:
    """Extract keys from XML file.

    Args:
        path: Path to XML file

    Returns:
        set of extracted keys

    """
    key_pattern = re.compile(r'android:key="(\w+)"')
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
    prefs_path_1 = base_path / "xml/revanced_prefs.xml"
    prefs_path_2 = base_dir / "src/main/resources/youtube/settings/xml/revanced_prefs.xml"

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
