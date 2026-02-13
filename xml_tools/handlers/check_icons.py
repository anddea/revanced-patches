"""Handler to check icons in prefs."""

from __future__ import annotations

import logging
import re
from pathlib import Path

from config import Settings

logger = logging.getLogger("xml_tools")


def parse_keys_from_kotlin(file_path: Path) -> set[str]:
    """Parse VisualPreferencesIcons.kt."""
    if not file_path.exists():
        logger.error("Kotlin file not found at '%s'", file_path)
        return set()

    logger.debug("Parsing Kotlin file: %s", file_path)
    with Path.open(file_path, encoding="utf-8") as f:
        content = f.read()

    # Parse rvxPreferenceKey
    map_block_match = re.search(r"private val rvxPreferenceKey = mapOf\((.*?)\)", content, re.DOTALL)
    rvx_keys: set[str] = set()
    if not map_block_match:
        logger.warning("Could not find 'rvxPreferenceKey' map in the Kotlin file.")
    else:
        map_block = map_block_match.group(1)
        rvx_keys.update(re.findall(r'"(revanced_[^"]+)"\s*to', map_block))
    logger.debug("Found %d keys in rvxPreferenceKey.", len(rvx_keys))

    # Parse emptyTitles
    set_block_match = re.search(r"private val emptyTitles = setOf\((.*?)\)", content, re.DOTALL)
    empty_title_keys: set[str] = set()
    if not set_block_match:
        logger.warning("Could not find 'emptyTitles' set in the Kotlin file.")
    else:
        set_block = set_block_match.group(1)
        empty_title_keys.update(re.findall(r'"([^"]+)"', set_block))
    logger.debug("Found %d keys in emptyTitles.", len(empty_title_keys))

    all_keys = rvx_keys.union(empty_title_keys)
    logger.debug("Total unique keys to check: %d.", len(all_keys))
    return all_keys


def check_keys_in_xml_with_regex(file_path: Path, keys_to_check: set[str]) -> tuple[set[str], set[str]]:
    """Scan the raw text of an XML file for preference keys using regex."""
    if not file_path.exists():
        logger.error("XML file not found at '%s'", file_path)
        return set(), keys_to_check

    logger.debug("Scanning XML file %s", file_path)
    with Path.open(file_path, encoding="utf-8") as f:
        xml_content = f.read()

    found_keys: set[str] = set()
    missing_keys: set[str] = set()

    for key in keys_to_check:
        pattern = f"android:key\\s*=\\s*[\"']{re.escape(key)}[\"']"
        if re.search(pattern, xml_content):
            found_keys.add(key)
        else:
            missing_keys.add(key)

    logger.debug("Checked %d keys against the XML file content.", len(keys_to_check))
    return found_keys, missing_keys


def process(app: str) -> None:
    """Process icons removed keys.

    Args:
        app: The application name.

    """
    settings = Settings()

    # This check is currently specific to YouTube
    if app != "youtube":
        logger.warning("Icon preference check is designed for 'youtube' app. Skipping for '%s'.", app)
        return

    kotlin_file_path = (
        settings.BASE_DIR
        / "patches/src/main/kotlin/app/morphe/patches"
        / app
        / "layout/visual/VisualPreferencesIconsPatch.kt"
    )
    xml_file_path = settings.get_resource_path(app, "settings/xml/revanced_prefs.xml")

    kotlin_keys = parse_keys_from_kotlin(kotlin_file_path)
    if not kotlin_keys:
        return

    found_keys, missing_keys = check_keys_in_xml_with_regex(xml_file_path, kotlin_keys)

    # ruff: noqa: ERA001
    # if found_keys:
    #     logger.info("✅ Found %d keys from Kotlin in the XML file (including commented-out ones):", len(found_keys))
    #     for key in sorted(found_keys):
    #         logger.info("  - %s", key)

    if missing_keys:
        logger.info(
            "❌ Missing %d keys from XML file (defined in Kotlin but not present in XML):",
            len(missing_keys),
        )
        for key in sorted(missing_keys):
            logger.info("  - %s", key)
    elif found_keys:
        logger.info("✅ All keys from VisualPreferencesIcons.kt were found in the XML.")
    else:
        logger.warning("No keys were found or missing. The check might not have run correctly.")
