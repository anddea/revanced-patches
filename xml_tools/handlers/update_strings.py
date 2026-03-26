"""Create updated_strings.xml files based on a list of keys from an input text file."""

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
    # Regex to find name="key_here" patterns, capturing the key
    key_pattern = re.compile(r'name="(\w+)"')

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
