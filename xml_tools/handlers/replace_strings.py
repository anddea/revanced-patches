"""Get strings from provided source and replace strings in destination."""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from defusedxml import ElementTree

from config.settings import Settings
from utils.xml_processor import XMLProcessor

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")
# ruff: noqa: ERA001


def update_strings(target_path: Path, source_path: Path, filter_keys: set[str] | None = None) -> None:
    """Update target XML file with strings from source file.

    Args:
        target_path: Path to target XML file
        source_path: Path to source XML file
        filter_keys: Optional set of keys to filter which strings are updated.
                     If None, all strings are updated (subject to blacklist).

    """
    blacklist = {
        "revanced_enable_swipe_brightness_summary_off",
        "revanced_enable_swipe_brightness_summary_on",
        "revanced_enable_swipe_volume_summary_off",
        "revanced_enable_swipe_volume_summary_on",
    }
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
            if name in blacklist:
                continue  # Skip blacklisted strings

            # Apply filter if provided
            if filter_keys is not None and name not in filter_keys:
                continue

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
            elif name not in blacklist and (filter_keys is None or name in filter_keys):
                # Add new element (only if not blacklisted and passes filter)
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
            additional_keys = None
            # {
            #     "revanced_swipe_show_circular_overlay_title",
            #     "revanced_swipe_show_circular_overlay_summary_on",
            #     "revanced_swipe_show_circular_overlay_summary_off",
            #     "revanced_swipe_overlay_minimal_style_title",
            #     "revanced_swipe_overlay_minimal_style_summary_on",
            #     "revanced_swipe_overlay_minimal_style_summary_off",
            # }

            if additional_keys:
                rvx_lang_path = base_dir / "src/main/resources/addresources" / f"values-{lang_dir.name}" / "strings.xml"
                # Pass the filter set to update_strings
                if rvx_lang_path.exists():
                    update_strings(target_path, rvx_lang_path, filter_keys=additional_keys)
            else:
                rvx_lang_path = rvx_base_path / "translations" / lang_dir.name / "strings.xml"
                if rvx_lang_path.exists():
                    update_strings(target_path, rvx_lang_path)
