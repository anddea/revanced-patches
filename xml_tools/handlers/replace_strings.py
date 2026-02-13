"""Module for updating XML strings from source to target files."""

from __future__ import annotations

import logging
import shutil
from pathlib import Path
from typing import TYPE_CHECKING, Any

from defusedxml import ElementTree as DefusedET

from config.settings import Settings
from utils.xml_processor import XMLProcessor

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")


def _find_source_translation_file(source_base_path: Path, lang_code: str) -> Path | None:
    """Find a source translation file by matching the language code prefix.

    Matches 'ar' to 'values-ar-rSA', 'in' to 'values-in', etc.

    Args:
        source_base_path: The base directory containing 'values-*' folders.
        lang_code: The two-letter language code (e.g., 'ar').

    Returns:
        The path to the 'strings.xml' file if found, otherwise None.

    """
    # Use a generator expression with next() to find the first match efficiently.
    matching_dir = next(
        (d for d in source_base_path.iterdir() if d.is_dir() and d.name.startswith(f"values-{lang_code}")),
        None,  # Default to None if no directory matches
    )

    if matching_dir:
        source_file = matching_dir / "youtube/strings.xml"
        if source_file.exists():
            return source_file
    return None


def update_strings(target_path: Path, source_path: Path, filter_keys: set[str] | None = None) -> None:
    """Update target XML file with strings from source file.

    Args:
        target_path: Path to target XML file
        source_path: Path to source XML file
        filter_keys: Optional set of keys to filter which strings are updated.
                     If None, all strings are updated (subject to blacklist).

    """
    blacklist: set[str] = {
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
        existing_elements: dict[str, Any] = {
            elem.get("name"): elem  # type: ignore[dict-item]
            for elem in target_root.findall(".//string")
            if elem.get("name") is not None
        }

        # Update existing strings or add new ones
        for name, data in source_strings.items():
            if name in blacklist:
                continue  # Skip blacklisted strings

            # Apply filter if provided
            if filter_keys is not None and name not in filter_keys:
                continue

            if name in existing_elements:
                # Update existing element
                existing_elem: Any = existing_elements[name]
                new_elem: Any = DefusedET.fromstring(data["text"])
                # Replace attributes and children
                existing_elem.attrib.clear()
                existing_elem.attrib.update(new_elem.attrib)
                existing_elem[:] = new_elem[:]
                existing_elem.text = new_elem.text
                existing_elem.tail = new_elem.tail
            elif name not in blacklist and (filter_keys is None or name in filter_keys):
                # Add new element (only if not blacklisted and passes filter)
                new_elem: Any = DefusedET.fromstring(data["text"])
                target_root.append(new_elem)

        # Write updated file
        XMLProcessor.write_file(target_path, target_root)
        logger.debug("Updated strings in %s", target_path)

    except Exception:
        logger.exception("Failed to update strings in %s: ", target_path)


def update_base_strings(base_path: Path, rvx_base_path: Path) -> None:
    """Update the base strings.xml file from RVX source."""
    source_path = base_path / "host/values/strings.xml"
    rvx_source_path = rvx_base_path / "settings/host/values/strings.xml"
    if rvx_source_path.exists():
        update_strings(source_path, rvx_source_path)


def sync_translations(translations_path: Path, rvx_base_path: Path) -> None:
    """Sync translation folders and strings from RVX source."""
    rvx_translations = rvx_base_path / "translations"
    if not rvx_translations.exists():
        return

    # Create new translation folders if they don't exist
    for lang_dir in rvx_translations.iterdir():
        if not lang_dir.is_dir():
            continue
        lang = lang_dir.name
        dest_lang_dir = translations_path / lang
        dest_strings_path = dest_lang_dir / "strings.xml"
        source_strings_path = lang_dir / "strings.xml"

        if not dest_lang_dir.exists() and source_strings_path.exists():
            dest_lang_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source_strings_path, dest_strings_path)
            logger.info("Created new translation folder %s and copied strings.xml", lang)

        # Update existing translation strings
        if source_strings_path.exists() and dest_strings_path.exists():
            update_strings(dest_strings_path, source_strings_path)


def update_translations_with_keys(
    translations_path: Path,
    base_dir: Path,
    additional_keys: set[str] | None = None,
) -> None:
    """Update translation strings with specific keys."""
    source_base_path = base_dir / "src/main/resources/addresources"

    for lang_dir in translations_path.iterdir():
        if not lang_dir.is_dir():
            continue

        target_path = lang_dir / "strings.xml"

        # Find the corresponding source file using the helper function
        rvx_lang_path = _find_source_translation_file(source_base_path, lang_dir.name)

        if rvx_lang_path:
            logger.debug("Found source %s for target %s", rvx_lang_path, target_path)
            update_strings(target_path, rvx_lang_path, filter_keys=additional_keys)
        else:
            logger.warning("No matching source translation found for language: %s", lang_dir.name)


def process(app: str, base_dir: Path) -> None:
    """Process files to update strings and copy new translation folders.

    Args:
        app: Application name (youtube/music).
        base_dir: Base directory of RVX patches operations.

    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")
    translations_path = settings.get_resource_path(app, "translations")
    rvx_base_path = base_dir / "src/main/resources" / app

    # Update base strings file
    update_base_strings(base_path, rvx_base_path)

    # Handle translations
    # ruff: noqa: ERA001
    # additional_keys: set[str] = {
    #     "revanced_hide_ask_button_summary_off",
    #     "revanced_hide_ask_button_summary_on",
    #     "revanced_hide_ask_button_title",
    #     "revanced_hide_ask_section_summary_off",
    #     "revanced_hide_ask_section_summary_on",
    #     "revanced_hide_ask_section_title",
    #     "revanced_hide_hype_button_summary_off",
    #     "revanced_hide_hype_button_summary_on",
    #     "revanced_hide_hype_button_title",
    #     "revanced_hide_player_control_buttons_background_summary_off",
    #     "revanced_hide_player_control_buttons_background_summary_on",
    #     "revanced_hide_player_control_buttons_background_title",
    #     "revanced_hide_promote_button_summary_off",
    #     "revanced_hide_promote_button_summary_on",
    #     "revanced_hide_promote_button_title",
    #     "revanced_hide_shorts_hashtag_button_summary_off",
    #     "revanced_hide_shorts_hashtag_button_summary_on",
    #     "revanced_hide_shorts_hashtag_button_title",
    #     "revanced_hide_shorts_preview_comment_summary_off",
    #     "revanced_hide_shorts_preview_comment_summary_on",
    #     "revanced_hide_shorts_preview_comment_title",
    #     "revanced_swipe_lowest_value_enable_auto_brightness_overlay_text",
    #     "revanced_swipe_overlay_minimal_style_summary_off",
    #     "revanced_swipe_overlay_minimal_style_summary_on",
    #     "revanced_swipe_overlay_minimal_style_title",
    #     "revanced_swipe_overlay_progress_brightness_color_summary",
    #     "revanced_swipe_overlay_progress_brightness_color_title",
    #     "revanced_swipe_overlay_progress_color_invalid_toast",
    #     "revanced_swipe_overlay_progress_color_summary",
    #     "revanced_swipe_overlay_progress_color_title",
    #     "revanced_swipe_overlay_progress_seek_color_summary",
    #     "revanced_swipe_overlay_progress_seek_color_title",
    #     "revanced_swipe_overlay_progress_speed_color_summary",
    #     "revanced_swipe_overlay_progress_speed_color_title",
    #     "revanced_swipe_overlay_progress_volume_color_summary",
    #     "revanced_swipe_overlay_progress_volume_color_title",
    #     "revanced_swipe_overlay_style_entry_1",
    #     "revanced_swipe_overlay_style_entry_2",
    #     "revanced_swipe_overlay_style_entry_3",
    #     "revanced_swipe_overlay_style_entry_4",
    #     "revanced_swipe_overlay_style_entry_5",
    #     "revanced_swipe_overlay_style_entry_6",
    #     "revanced_swipe_overlay_style_entry_7",
    #     "revanced_swipe_overlay_style_title",
    #     "revanced_swipe_show_circular_overlay_summary_off",
    #     "revanced_swipe_show_circular_overlay_summary_on",
    #     "revanced_swipe_show_circular_overlay_title",
    #     "revanced_swipe_text_overlay_size_invalid_toast",
    #     "revanced_swipe_text_overlay_size_summary",
    #     "revanced_swipe_text_overlay_size_title",
    # }

    if "morphe-patches" in str(base_dir):
        update_translations_with_keys(translations_path, base_dir)
        # update_translations_with_keys(translations_path, base_dir, additional_keys)
    else:
        sync_translations(translations_path, rvx_base_path)
