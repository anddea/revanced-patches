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


def create_updated_strings_file(  # noqa: C901
    output_path: Path,
    filtered_strings: dict[str, dict[str, str]],
) -> None:
    """Create or overwrite the updated_strings.xml file with the filtered strings.

    Args:
        output_path: The path where updated_strings.xml should be saved.
        filtered_strings: A dictionary containing the string data (name -> {text: xml_string})
                          to include in the file.

    """
    if not filtered_strings:
        logger.info("No strings to write to %s. Skipping file creation/modification.", output_path)
        # Remove existing file if no strings are provided for this language
        if output_path.exists():
            try:
                output_path.unlink()
                logger.info("Removed existing file as no strings were applicable: %s", output_path)
            except OSError:
                logger.exception("Failed to remove existing file: %s", output_path)
        return

    try:
        new_root = ET.Element("resources")
        # Sort by key name for consistent output
        for name in sorted(filtered_strings.keys()):
            data = filtered_strings[name]
            try:
                string_elem = DefusedET.fromstring(data["text"])
                new_root.append(string_elem)
            except DefusedET.ParseError:
                logger.exception("Failed to parse string element for key '%s'. Skipping.", name)
                logger.debug("Problematic XML string: %s", data.get("text", "N/A"))
            except Exception:
                logger.exception("Unexpected error processing element for key '%s'", name)

        if len(new_root) > 0:  # Only write if we successfully added elements
            XMLProcessor.write_file(output_path, new_root)
            logger.info("Created/Updated %s with %d strings.", output_path, len(new_root))
        else:
            logger.warning("No valid string elements could be added to %s. File not written.", output_path)
            # Ensure empty file is removed if it exists
            if output_path.exists():
                try:
                    output_path.unlink()
                    logger.info("Removed existing file as no valid elements could be added: %s", output_path)
                except OSError:
                    logger.exception("Failed to remove existing file after validation failure: %s", output_path)

    except Exception:
        logger.exception("Failed to create or write updated strings file: %s", output_path)


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
    filtered_host_strings: dict[str, dict[str, str]] = {}
    missing_in_host: set[str] = set()
    for key in keys_from_input:
        if key in source_strings:
            filtered_host_strings[key] = source_strings[key]
        else:
            missing_in_host.add(key)

    if missing_in_host:
        logger.warning(
            "The following keys from the input file were NOT found in the host strings file (%s):",
            host_strings_path,
        )
        for key in sorted(missing_in_host):
            logger.warning("  - %s", key)

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

        processed_langs = 0
        for lang_dir in translations_dir.iterdir():
            if lang_dir.is_dir():
                processed_langs += 1
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

        logger.info("Finished processing %d language directories.", processed_langs)

    except Exception:
        logger.exception("An error occurred while processing translation directories for app '%s'", app)
