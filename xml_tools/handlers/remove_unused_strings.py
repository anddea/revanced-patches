"""Remove unused strings."""

import logging
import os
from pathlib import Path

from defusedxml import lxml
from lxml import etree as et

from config.settings import Settings
from utils.xml_processor import XMLProcessor

logger = logging.getLogger("xml_tools")

# Constants
BLACKLISTED_STRINGS: set[str] = {
    "revanced_remember_video_quality_mobile",
    "revanced_remember_video_quality_wifi",
    "revanced_sb_api_url_sum",
    "revanced_sb_enabled",
    "revanced_sb_enabled_sum",
    "revanced_sb_toast_on_skip",
    "revanced_sb_toast_on_skip_sum",
    "revanced_spoof_streaming_data_type_entry_android_creator",
    "revanced_third_party_youtube_music_not_installed_dialog_title",
}

PREFIX_TO_IGNORE: tuple[str, ...] = (
    "revanced_icon_",
    "revanced_shorts_custom_actions_",
    "revanced_spoof_app_version_target_entry_",
    "revanced_spoof_streaming_data_side_effects_",
)

settings_instance = Settings()

SCRIPT_DIR = settings_instance.BASE_DIR
SEARCH_DIRECTORIES = [str(SCRIPT_DIR.parent / "revanced-patches")]
ALLOWED_EXTENSIONS = (".kt", ".java", ".xml")


def get_base_name(name: str) -> str:
    """Return the base name by stripping known suffixes from a string name.

    Args:
        name (str): The original string name.

    Returns:
        str: The string name with known suffixes removed.

    Example:
        >>> get_base_name("my_setting_summary_on")
        'my_setting'

    """
    suffixes = ["_title", "_summary_off", "_summary_on", "_summary"]
    for suffix in suffixes:
        if name.endswith(suffix):
            return name[: -len(suffix)]
    return name


def search_in_files(directories: list[str], name_values: set[str]) -> dict[str, list[str]]:
    """Search for string names in all files within specified directories.

    Args:
        directories (list[str]): list of directory paths to search.
        name_values (set[str]): set of string names to search for.

    Returns:
        dict[str, list[str]]: Dictionary mapping each string name to a list of file paths where it was found.

    Raises:
        OSError: If there are problems accessing the directories or files.
        UnicodeDecodeError: If a file cannot be read as UTF-8.

    Notes:
        - Skips hidden directories and 'build' directories
        - Ignores 'strings.xml' and 'missing_strings.xml' files
        - Only searches files with extensions defined in ALLOWED_EXTENSIONS
        - Searches for both original names and their base forms (without suffixes)

    """
    results = {name: [] for name in name_values}

    for directory in directories:
        abs_dir = Path(directory).resolve()
        logger.info("Searching in directory: %s (exists: %s)", abs_dir, Path(abs_dir).exists())

        for root, dirs, files in os.walk(directory):
            # Skip hidden and build directories
            dirs[:] = [d for d in dirs if not d.startswith(".") and d != "build"]

            for file in files:
                if file in ("strings.xml", "missing_strings.xml") or not file.endswith(ALLOWED_EXTENSIONS):
                    continue

                file_path = Path(root) / file
                try:
                    with file_path.open(encoding="utf-8") as f:
                        content = f.read()
                        for name in name_values:
                            # Check both original name and base name
                            if name in content or get_base_name(name) in content:
                                results[name].append(file_path)
                except Exception:
                    logger.exception("Error reading %s: ", file_path)

    return results


def should_remove(name: str, unused_names: set[str]) -> bool:
    """Determine if a string should be removed based on various criteria.

    Args:
        name (str): The string name to check.
        unused_names (set[str]): set of string names that were not found in any source files.

    Returns:
        bool: True if the string should be removed, False otherwise.

    Notes:
        A string should be removed if:
        - The string or its base name is in the unused_names set
        - The string is not in BLACKLISTED_STRINGS
        - The string does not start with any prefix in PREFIX_TO_IGNORE

    """
    base_name = get_base_name(name)
    return (
        (name in unused_names or base_name in unused_names)
        and name not in BLACKLISTED_STRINGS
        and not any(name.startswith(prefix) for prefix in PREFIX_TO_IGNORE)
    )


def process_xml_file(file_path: Path, unused_names: set[str]) -> None:
    """Process a single XML file to remove unused strings.

    Args:
        file_path (Path): Path to the XML file to process.
        unused_names (set[str]): set of string names that should be considered for removal.

    Notes:
        - Creates a new XML tree containing only the strings that should be kept
        - Only writes the file if strings were actually removed
        - Maintains original XML structure and attributes

    """
    try:
        _, _, strings_dict = XMLProcessor.parse_file(file_path)

        # Count strings before removal
        initial_count = len(strings_dict)

        # Create new root with only used strings
        new_root = et.Element("resources")
        kept_strings = 0
        for name, data in sorted(strings_dict.items()):
            if not should_remove(name, unused_names):
                string_elem = lxml.fromstring(data["text"].encode())
                new_root.append(string_elem)
                kept_strings += 1

        # Only write if strings were actually removed
        if kept_strings < initial_count:
            XMLProcessor.write_file(file_path, new_root)
            logger.info(
                "Updated %s: removed %s strings, kept %s strings",
                file_path,
                initial_count - kept_strings,
                kept_strings,
            )
        else:
            logger.info("No changes needed for %s", file_path)

    except Exception:
        logger.exception("Error processing %s: ", file_path)


def process(app: str) -> None:
    """Remove unused strings from XML files for a given application.

    Args:
        app (str): The application identifier to process.

    Notes:
        - Processes both the source strings file and all translation files
        - Uses settings from the Settings class to determine file locations
        - Maintains a log of all operations
        - Skips writing files if no changes are needed

    """
    settings = Settings()
    base_path = settings.get_resource_path(app, "settings")
    source_path = base_path / "host/values/strings.xml"
    translations = settings.get_resource_path(app, "translations")

    try:
        # Get source strings
        _, _, source_strings = XMLProcessor.parse_file(source_path)

        # Find unused strings using direct file search
        search_results = search_in_files(SEARCH_DIRECTORIES, set(source_strings.keys()))
        unused_names = {name for name, files in search_results.items() if not files}

        # Process source file
        if unused_names:
            process_xml_file(source_path, unused_names)

        # Process translation files
        for lang_dir in translations.iterdir():
            if lang_dir.is_dir():
                dest_path = lang_dir / "strings.xml"
                if dest_path.exists():
                    process_xml_file(dest_path, unused_names)

    except Exception:
        logger.exception("Error during processing: ")
