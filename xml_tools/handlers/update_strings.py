# Copyright (C) 2026 anddea

"""Update translation overlays and copy selected strings between app resources."""

from __future__ import annotations

import logging
import re
from typing import TYPE_CHECKING
from xml.etree import ElementTree as ET

from defusedxml import ElementTree as DefusedET

from config.settings import Settings
from utils.xml_processor import XMLProcessor

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")

OTHER_APP: dict[str, str] = {
    "youtube": "music",
    "music": "youtube",
}


def replace_element(existing_elem: ET.Element, new_elem: ET.Element) -> None:
    """Replace an element's contents while preserving its position in the tree."""
    existing_elem.attrib.clear()
    existing_elem.attrib.update(new_elem.attrib)
    existing_elem[:] = new_elem[:]
    existing_elem.text = new_elem.text
    existing_elem.tail = new_elem.tail


def parse_string_elements(filtered_strings: dict[str, dict[str, str]]) -> dict[str, ET.Element]:
    """Parse string XML fragments into elements keyed by string name."""
    new_elements: dict[str, ET.Element] = {}
    for name, data in filtered_strings.items():
        try:
            string_elem = DefusedET.fromstring(data["text"])
            new_elements[name] = string_elem
        except DefusedET.ParseError:
            logger.exception("Failed to parse string element for key '%s'. Skipping.", name)
            logger.debug("Problematic XML string: %s", data.get("text", "N/A"))
        except Exception:
            logger.exception("Unexpected error processing element for key '%s'", name)

    return new_elements


def build_existing_elements(root: ET.Element) -> dict[str, ET.Element]:
    """Return existing named string elements from the root."""
    return {name: elem for elem in root.findall(".//string[@name]") if (name := elem.get("name")) is not None}


def update_existing_strings(root: ET.Element, new_elements: dict[str, ET.Element]) -> int:
    """Update existing strings in-place and return the number of updates."""
    existing_elements = build_existing_elements(root)
    updated_count = 0
    for name in sorted(set(new_elements) & set(existing_elements)):
        replace_element(existing_elements[name], new_elements[name])
        updated_count += 1

    return updated_count


def append_new_strings(root: ET.Element, new_elements: dict[str, ET.Element]) -> int:
    """Append new strings to the root and return the number of additions."""
    existing_elements = build_existing_elements(root)
    added_count = 0
    for name in sorted(set(new_elements) - set(existing_elements)):
        root.append(new_elements[name])
        added_count += 1

    return added_count


def merge_strings_file(
    target_path: Path,
    strings_to_merge: dict[str, dict[str, str]],
) -> tuple[int, int]:
    """Merge selected string elements into a destination XML file.

    Existing elements are replaced in place and missing elements are appended. A
    new ``resources`` document is created when the destination file does not yet
    exist. Invalid existing XML is never overwritten.

    Args:
        target_path: Destination XML file.
        strings_to_merge: String data keyed by resource name.

    Returns:
        A tuple containing the number of replaced and appended elements.

    """
    if not strings_to_merge:
        return 0, 0

    try:
        new_elements = parse_string_elements(strings_to_merge)
        if not new_elements:
            logger.warning("No valid string elements could be merged into %s.", target_path)
            return 0, 0

        if not target_path.exists() or target_path.stat().st_size == 0:
            root = ET.Element("resources")
        else:
            _, root, _ = XMLProcessor.parse_file(target_path)
            if root is None:
                logger.error("Failed to parse existing destination file: %s", target_path)
                return 0, 0

        updated_count = update_existing_strings(root, new_elements)
        added_count = append_new_strings(root, new_elements)
        if updated_count or added_count:
            XMLProcessor.write_file(target_path, root)

    except Exception:
        logger.exception("Failed to merge strings into %s", target_path)
        return 0, 0
    else:
        return updated_count, added_count


def get_keys_from_xml(file_path: Path) -> set[str]:
    """Parse an XML file and return the set of keys ('name' attributes).

    Args:
        file_path: Path to the XML file.

    Returns:
        A set of keys found in the file. Returns an empty set if the file
        doesn't exist, is empty, or cannot be parsed.

    """
    if not file_path.exists() or file_path.stat().st_size == 0:
        return set()

    try:
        _, _, strings_dict = XMLProcessor.parse_file(file_path)
        return set(strings_dict.keys())
    except Exception:
        logger.exception("Failed to parse keys from %s, returning empty set.", file_path)
        return set()


def extract_keys_from_file(input_file_path: Path) -> set[str]:
    """Extract string keys (name="...") from a text file.

    Args:
        input_file_path: Path to the text file containing keys.

    Returns:
        A set of extracted keys. Returns an empty set if the file cannot be read
        or no keys are found.

    """
    keys_found: set[str] = set()
    # Match name attributes in XML snippets or lines copied from a diff.
    key_pattern = re.compile(r"""\bname\s*=\s*["']([^"']+)["']""")

    try:
        with input_file_path.open("r", encoding="utf-8") as f:
            for line in f:
                matches = key_pattern.findall(line)
                keys_found.update(matches)
        if keys_found:
            logger.info("Found %d keys to update from %s", len(keys_found), input_file_path)
        else:
            logger.warning("No keys found in the input file: %s", input_file_path)
    except OSError:
        logger.exception("Failed to read input key file: %s", input_file_path)
    except Exception:
        logger.exception("An unexpected error occurred while reading keys from %s", input_file_path)

    return keys_found


def create_updated_strings_file(
    output_path: Path,
    filtered_strings: dict[str, dict[str, str]],
) -> None:
    """Create or update updated_strings.xml.

    Args:
        output_path: The path where updated_strings.xml should be saved.
        filtered_strings: A dictionary containing the string data (name -> {text: xml_string})
                          to include in the file.

    """
    if not filtered_strings:
        logger.info("No strings to write to %s. Leaving any existing file unchanged.", output_path)
        return

    try:
        new_elements = parse_string_elements(filtered_strings)
        if not new_elements:
            logger.warning("No valid string elements could be added to %s. File not written.", output_path)
            return

        if not output_path.exists() or output_path.stat().st_size == 0:
            new_root = ET.Element("resources")
            for name in sorted(new_elements.keys()):
                new_root.append(new_elements[name])
            XMLProcessor.write_file(output_path, new_root)
            logger.info("Created %s with %d strings.", output_path, len(new_root))
            return

        _, root, _ = XMLProcessor.parse_file(output_path)
        if root is None:
            logger.error("Failed to parse existing file before updating strings: %s", output_path)
            return

        updated_count = update_existing_strings(root, new_elements)
        added_count = append_new_strings(root, new_elements)
        if not updated_count and not added_count:
            logger.info("No new or changed strings to write to %s.", output_path)
            return

        XMLProcessor.write_file(output_path, root)
        logger.info(
            "Updated %d existing string(s) and added %d new string(s) to %s.",
            updated_count,
            added_count,
            output_path,
        )

    except Exception:
        logger.exception("Failed to create or write updated strings file: %s", output_path)


def filter_host_strings(
    keys_from_input: set[str],
    source_strings: dict[str, dict[str, str]],
    host_strings_path: Path,
) -> dict[str, dict[str, str]]:
    """Return the host strings that were requested by the input file."""
    filtered_host_strings = {key: source_strings[key] for key in keys_from_input if key in source_strings}
    missing_in_host = sorted(keys_from_input - filtered_host_strings.keys())

    if missing_in_host:
        logger.warning(
            "The following keys from the input file were NOT found in the host strings file (%s):",
            host_strings_path,
        )
        for key in missing_in_host:
            logger.warning("  - %s", key)

    return filtered_host_strings


def process_language_directory(
    lang_dir: Path,
    initial_keys_to_consider: set[str],
    filtered_host_strings: dict[str, dict[str, str]],
) -> None:
    """Create or update the per-language updated_strings.xml file."""
    lang_name = lang_dir.name
    missing_strings_path = lang_dir / "missing_strings.xml"
    keys_in_missing_file = get_keys_from_xml(missing_strings_path)

    # Determine which keys should actually be written for *this* language
    keys_to_write_for_this_lang = initial_keys_to_consider - keys_in_missing_file

    # Log skipped keys for this language (optional, can be noisy)
    skipped_keys_for_lang = initial_keys_to_consider - keys_to_write_for_this_lang
    if skipped_keys_for_lang:
        logger.debug(
            "For lang '%s', skipping keys found in missing_strings.xml: %s",
            lang_name,
            sorted(skipped_keys_for_lang),
        )

    # Build the dictionary of strings to actually write for this language
    strings_to_write: dict[str, dict[str, str]] = {
        key: filtered_host_strings[key] for key in keys_to_write_for_this_lang
    }

    # Define the output path and create the file
    output_path = lang_dir / "updated_strings.xml"
    create_updated_strings_file(output_path, strings_to_write)


def process(app: str, input_file_path: Path) -> None:
    """Generate updated_strings.xml files in translation directories.

    Reads keys from the input_file_path, finds the corresponding full strings
    in the host strings.xml. For each translation directory, it checks if the
    key exists in that directory's missing_strings.xml. If it does not, the
    string from the host file is written to that directory's updated_strings.xml file.

    Args:
        app: Application name (e.g., 'youtube', 'music').
        input_file_path: Path to the text file containing the string keys to update.

    """
    settings = Settings()
    keys_from_input = extract_keys_from_file(input_file_path)

    if not keys_from_input:
        logger.error("No keys extracted from the input file. Aborting update process.")
        return

    # Get the source strings from the main host file
    host_strings_path = settings.get_resource_path(app, "settings") / "host/values/strings.xml"
    _, _, source_strings = XMLProcessor.parse_file(host_strings_path)

    if not source_strings:
        logger.error("Could not parse source strings from %s. Aborting.", host_strings_path)
        return

    # Filter the source strings based on the keys from the input file
    # This dictionary holds the potential strings to write {key: {text: xml_string}}
    filtered_host_strings = filter_host_strings(keys_from_input, source_strings, host_strings_path)

    if not filtered_host_strings:
        logger.error("None of the requested keys were found in the host strings file. No files will be generated.")
        return

    # Get the set of keys we might potentially write (those found in host)
    initial_keys_to_consider = set(filtered_host_strings.keys())

    # Process translation directories
    translations_dir = settings.get_resource_path(app, "translations")
    try:
        if not translations_dir.is_dir():
            logger.error("Translations directory not found: %s", translations_dir)
            return

        language_directories = [lang_dir for lang_dir in translations_dir.iterdir() if lang_dir.is_dir()]
        for lang_dir in language_directories:
            process_language_directory(lang_dir, initial_keys_to_consider, filtered_host_strings)

        logger.info("Finished processing %d language directories.", len(language_directories))

    except Exception:
        logger.exception("An error occurred while processing translation directories for app '%s'", app)


def _copy_translation_directory(
    source_lang_dir: Path,
    target_lang_dir: Path,
    keys_to_copy: set[str],
) -> tuple[int, int]:
    """Copy requested translated strings between matching language directories."""
    source_path = source_lang_dir / "strings.xml"
    target_path = target_lang_dir / "strings.xml"

    if not source_path.is_file():
        logger.debug("Skipping %s because %s does not exist.", target_lang_dir.name, source_path)
        return 0, 0

    _, _, source_strings = XMLProcessor.parse_file(source_path)
    strings_to_copy = {key: source_strings[key] for key in keys_to_copy if key in source_strings}
    if not strings_to_copy:
        logger.debug("No requested strings found in source translation %s.", source_path)
        return 0, 0

    return merge_strings_file(target_path, strings_to_copy)


def copy_between_apps(source_app: str, input_file_path: Path) -> None:
    """Copy requested host strings and translations to the other app.

    The input file is parsed in the same way as :func:`process`: every
    ``name`` attribute found in it is treated as a requested resource key. The
    source host file is the authority for which keys are eligible. Matching
    translated entries are then copied into language directories present in
    both applications.

    Args:
        source_app: Source application (``youtube`` or ``music``).
        input_file_path: File containing XML ``name`` attributes to copy.

    """
    target_app = OTHER_APP.get(source_app)
    if target_app is None:
        logger.error("Unsupported source application '%s'.", source_app)
        return

    settings = Settings()
    keys_from_input = extract_keys_from_file(input_file_path)
    if not keys_from_input:
        logger.error("No keys extracted from the input file. Aborting copy process.")
        return

    source_host_path = settings.get_resource_path(source_app, "settings") / "host/values/strings.xml"
    _, _, source_host_strings = XMLProcessor.parse_file(source_host_path)
    if not source_host_strings:
        logger.error("Could not parse source strings from %s. Aborting.", source_host_path)
        return

    strings_to_copy = filter_host_strings(keys_from_input, source_host_strings, source_host_path)
    if not strings_to_copy:
        logger.error("None of the requested keys were found in the source host file. Aborting copy process.")
        return

    target_host_path = settings.get_resource_path(target_app, "settings") / "host/values/strings.xml"
    host_updated, host_added = merge_strings_file(target_host_path, strings_to_copy)
    logger.info(
        "Copied %d host string(s) from %s to %s (%d replaced, %d appended).",
        len(strings_to_copy),
        source_app,
        target_app,
        host_updated,
        host_added,
    )

    source_translations_dir = settings.get_resource_path(source_app, "translations")
    target_translations_dir = settings.get_resource_path(target_app, "translations")
    if not source_translations_dir.is_dir():
        logger.warning("Source translations directory not found: %s", source_translations_dir)
        return
    if not target_translations_dir.is_dir():
        logger.warning("Target translations directory not found: %s", target_translations_dir)
        return

    source_languages = {path.name for path in source_translations_dir.iterdir() if path.is_dir()}
    target_languages = {path.name for path in target_translations_dir.iterdir() if path.is_dir()}
    matching_languages = sorted(source_languages & target_languages)

    total_updated = 0
    total_added = 0
    for language in matching_languages:
        updated_count, added_count = _copy_translation_directory(
            source_translations_dir / language,
            target_translations_dir / language,
            set(strings_to_copy),
        )
        total_updated += updated_count
        total_added += added_count

    skipped_languages = sorted(source_languages - target_languages)
    if skipped_languages:
        logger.info(
            "Skipped %d source translation language(s) not present in %s: %s",
            len(skipped_languages),
            target_app,
            ", ".join(skipped_languages),
        )

    logger.info(
        "Copied requested translations from %s to %s across %d matching language(s) (%d replaced, %d appended).",
        source_app,
        target_app,
        len(matching_languages),
        total_updated,
        total_added,
    )
