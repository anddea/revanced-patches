"""Check missing prefs keys in XML files by comparing extracted keys and values."""

from __future__ import annotations

import difflib
import logging
import re
from typing import TYPE_CHECKING

from config.settings import Settings

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")
BLACKLIST: set[str] = {
    "revanced_extended_settings_search_history",
    "revanced_swipe_overlay_alternative_ui",
    "revanced_swipe_overlay_text_size",
}


def extract_key_lines(path: Path) -> dict[str, str]:
    """Extract lines containing preference keys from an XML file.

    Args:
        path: Path to the XML file.

    Returns:
        A dictionary where keys are preference keys and values are the
        corresponding XML lines. Returns an empty dictionary if the file is not found.

    """
    try:
        key_pattern = re.compile(r'android:key="(\w+)"')
        key_lines: dict[str, str] = {}

        with path.open(encoding="utf-8") as file:
            for line in file:
                match = key_pattern.search(line)
                if match:
                    key = match.group(1)
                    key_lines[key] = line.strip()

    except FileNotFoundError:
        logger.exception("Failed to extract keys from %s: ", path)
        return {}
    else:
        return key_lines


def normalize_line(line: str, ignored_attributes: list[str]) -> str:
    """Normalize an XML line by removing specified ignored attributes.

    Args:
        line: The XML line to normalize.
        ignored_attributes: A list of attribute names to remove (e.g., ["app:searchDependency"].

    Returns:
        The normalized line with ignored attributes removed.

    """
    for attribute in ignored_attributes:
        line = re.sub(rf'\s*{attribute}="[^"]*"\s*', " ", line)
    return line.strip()


def _get_default_ignored_attributes() -> list[str]:
    """Retrieve the default list of XML attributes to ignore during comparison.

    Returns:
        A list of attribute names that should be ignored when comparing XML lines.

    """
    return [
        "android:dependency",
        "app:searchDependency",
    ]


def _get_default_key_only_check() -> list[str]:
    """Retrieve the default list of keys for key-only existence checks.

    Returns:
        A list of preference keys that should only be checked for existence, not full comparison.

    """
    return [
        "revanced_change_shorts_repeat_state",
        "revanced_custom_seekbar_color_accent",
        "revanced_custom_seekbar_color_primary",
        "revanced_hide_player_youtube_music_button",
        "revanced_overlay_button_external_downloader_queue_manager",
        "revanced_override_video_download_button_queue_manager",
        "revanced_override_youtube_music_button_about_prerequisite",
        "revanced_swipe_volume_sensitivity",
        "revanced_whitelist_settings",
    ]


def _compare_key_lines(
    key_lines_1: dict[str, str],
    key_lines_2: dict[str, str],
    ignored_attributes: list[str],
    key_only_check: list[str],
    blacklist: set[str],
) -> tuple[set[str], set[str], dict[str, dict[str, str]]]:
    """Compare key-value pairs from two XML files and identify differences.

    Args:
        key_lines_1: Extracted key-value pairs from the first file.
        key_lines_2: Extracted key-value pairs from the second file.
        ignored_attributes: List of attributes to ignore during comparison.
        key_only_check: List of keys where only existence matters, not content.
        blacklist: A set of keys to ignore completely.

    Returns:
        A tuple containing:
        - missing_keys: Keys present in file1 but missing in file2 (not in key_only_check).
        - missing_key_only: Keys present in file1 but missing in file2 (from key_only_check).
        - different_lines: Dictionary of keys with differing XML lines between files.

    """
    key_only_set: set[str] = set(key_only_check)

    # Apply blacklist filtering
    key_lines_1 = {k: v for k, v in key_lines_1.items() if k not in blacklist}
    key_lines_2 = {k: v for k, v in key_lines_2.items() if k not in blacklist}

    missing_keys_all = set(key_lines_1.keys()) - set(key_lines_2.keys())
    missing_keys = missing_keys_all - key_only_set
    missing_key_only = missing_keys_all & key_only_set

    different_lines: dict[str, dict[str, str]] = {}
    common_keys = (set(key_lines_1.keys()) & set(key_lines_2.keys())) - key_only_set

    for key in common_keys:
        normalized_line1 = normalize_line(key_lines_1[key], ignored_attributes)
        normalized_line2 = normalize_line(key_lines_2[key], ignored_attributes)

        if normalized_line1 != normalized_line2:
            different_lines[key] = {
                "file1": key_lines_1[key],
                "file2": key_lines_2[key],
            }

    return missing_keys, missing_key_only, different_lines


def _fine_grained_colored_diff(line1: str, line2: str) -> str:
    """Generate a fine-grained diff highlighting differences with ANSI colors.

    Args:
        line1: First line for comparison.
        line2: Second line for comparison.

    Returns:
        A string containing the diff output with insertions in green and deletions in red.

    """
    matcher = difflib.SequenceMatcher(None, line1, line2)
    output = ""

    for opcode, a0, a1, b0, b1 in matcher.get_opcodes():
        if opcode == "equal":
            output += line1[a0:a1]
        elif opcode == "insert":
            output += "\033[92m" + line2[b0:b1] + "\033[0m"  # Green for insertions
        elif opcode == "delete":
            output += "\033[91m" + line1[a0:a1] + "\033[0m"  # Red for deletions
        elif opcode == "replace":
            output += "\033[91m" + line1[a0:a1] + "\033[0m"
            output += "\033[92m" + line2[b0:b1] + "\033[0m"
    return output


def _log_comparison_results(
    missing_keys: set[str],
    missing_key_only: set[str],
    different_lines: dict[str, dict[str, str]],
    key_lines_1: dict[str, str],
) -> None:
    """Log the results of the key comparison.

    Args:
        missing_keys: Keys missing and not in key_only_check.
        missing_key_only: Keys missing but in key_only_check.
        different_lines: Dictionary of different lines.
        key_lines_1: Key lines from the first file.

    """
    if missing_keys:
        logger.info("Missing keys found in file2 (full line check):")
        for key in sorted(missing_keys):
            logger.info("  ❌  Key: %s, Line: %s", key, key_lines_1[key])

    if missing_key_only:
        logger.info("Missing keys found in file2 (existence-only check):")
        for key in sorted(missing_key_only):
            logger.info("  ❌  Key: %s", key)

    if different_lines:
        logger.info("Different lines found for common keys (after ignoring specified attributes):")
        for key, lines in different_lines.items():
            logger.info("  ❌  Key: %s", key)
            diff_output = _fine_grained_colored_diff(lines["file1"], lines["file2"])
            logger.info("    Diff:\n%s", diff_output)

    if not missing_keys and not missing_key_only and not different_lines:
        logger.info("✅  No missing or different preferences found (after specified checks).")


def process(
    app: str,
    base_dir: Path,
    ignored_attributes: list[str] | None = None,
    key_only_check: list[str] | None = None,
    blacklist: set[str] | None = None,
) -> None:
    """Process XML preference files and identify missing or differing preferences.

    Args:
        app: The application name (e.g., "youtube", "music").
        base_dir: The base directory where RVX patches are located.
        ignored_attributes: Optional list of attributes to ignore during comparison.
        key_only_check: Optional list of keys for which only existence should be checked.
        blacklist: Optional set of keys to completely ignore.

    Logs:
        Missing keys, keys with differences, or a message indicating no issues were found.

    """
    if ignored_attributes is None:
        ignored_attributes = _get_default_ignored_attributes()
    if key_only_check is None:
        key_only_check = _get_default_key_only_check()
    blacklist = BLACKLIST if blacklist is None else blacklist.union(BLACKLIST)

    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")

    prefs_path_1 = base_dir / "src/main/resources/youtube/settings/xml/revanced_prefs.xml"
    prefs_path_2 = base_path / "xml/revanced_prefs.xml"

    try:
        key_lines_1 = extract_key_lines(prefs_path_1)
        key_lines_2 = extract_key_lines(prefs_path_2)

        if not key_lines_1 or not key_lines_2:
            logger.warning("One or both preference files could not be read.")
            return

        missing_keys, missing_key_only, different_lines = _compare_key_lines(
            key_lines_1,
            key_lines_2,
            ignored_attributes,
            key_only_check,
            blacklist,
        )

        _log_comparison_results(missing_keys, missing_key_only, different_lines, key_lines_1)
    except Exception:
        logger.exception("Failed to process preference files: ")
