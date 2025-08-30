"""Check Git diff and create updated_strings.xml for forced strings."""

from __future__ import annotations

import logging
import xml.etree.ElementTree as ET
from typing import TYPE_CHECKING

import git
from defusedxml import ElementTree as DefusedET

from config import Settings
from utils.xml_processor import XMLProcessor

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")


class EnhancedXMLProcessor(XMLProcessor):
    """Extends XMLProcessor with the ability to parse XML from a string."""

    @staticmethod
    def parse_string(
        xml_content: str,
    ) -> tuple[ET.ElementTree | None, ET.Element | None, dict[str, dict[str, str]]]:
        """Parse XML content from a string.

        Args:
            xml_content: A string containing the XML document.

        Returns:
            A tuple containing the ElementTree, the root element, and a dictionary
            mapping string names to their full XML element as a string. Returns
            (None, None, {}) on failure.

        """
        try:
            root = DefusedET.fromstring(xml_content)
            tree = ET.ElementTree(root)
            # Store the full element tag as a string to preserve attributes,
            # formatting, and nested tags (e.g., <b>, <i>).
            strings_dict = {
                elem.attrib["name"]: {"text": ET.tostring(elem, encoding="unicode")}
                for elem in root.findall(".//string[@name]")
            }
        except (DefusedET.ParseError, KeyError):
            logger.exception("Failed to parse XML content string: %s")
            return None, None, {}
        else:
            return tree, root, strings_dict


class GitDiffProcessor:
    """Encapsulates Git operations to find changes in string resource files."""

    def __init__(self, repo_path: Path) -> None:
        """Initialize the processor with the path to the git repository.

        Args:
            repo_path: The path to the directory containing the .git folder.

        Raises:
            git.InvalidGitRepositoryError: If the path is not a valid Git repository.

        """
        try:
            self.repo = git.Repo(repo_path, search_parent_directories=True)
            logger.debug("Git repository initialized at: %s", self.repo.working_dir)
        except git.InvalidGitRepositoryError:
            logger.exception("Error: Not a valid git repository: %s", repo_path)
            raise

    def get_changed_strings(self, file_path: Path) -> dict[str, str]:
        """Compare the HEAD and working versions of a file to find added or modified strings.

        This method identifies changes by comparing the full XML element, correctly
        detecting modifications to attributes, text content, and nested tags.

        Args:
            file_path: The absolute path to the strings.xml file to analyze.

        Returns:
            A dictionary mapping the string 'name' to its full XML element string for
            all new or modified strings.

        """
        if not file_path.is_file():
            return {}

        relative_path = file_path.relative_to(self.repo.working_dir)

        # Optimization: If the file is not listed in the diff, there are no changes.
        if str(relative_path) not in self.repo.git.diff("HEAD", "--name-only"):
            return {}

        # Get the current version from the working directory
        new_content = file_path.read_text(encoding="utf-8")
        _, _, new_strings_dict = EnhancedXMLProcessor.parse_string(new_content)

        # Get the previous version from the last commit (HEAD)
        try:
            old_blob = self.repo.head.commit.tree / str(relative_path)
            old_content = old_blob.data_stream.read().decode("utf-8")  # type: ignore[reportUnknownMemberType]
            _, _, old_strings_dict = EnhancedXMLProcessor.parse_string(old_content)  # type: ignore[reportUnknownMemberType]
        except KeyError:
            # File is new in this commit, so all its strings are considered additions.
            old_strings_dict = {}
        except Exception:
            logger.exception("Failed to read previous version of %s from git HEAD.", relative_path)
            return {}

        changed_strings: dict[str, str] = {}
        for name, new_data in new_strings_dict.items():
            if name not in old_strings_dict:
                changed_strings[name] = new_data["text"]
                logger.debug("Found added string: '%s'", name)
            elif new_data["text"] != old_strings_dict.get(name, {}).get("text"):
                changed_strings[name] = new_data["text"]
                logger.debug("Found modified string: '%s'", name)

        return changed_strings


def apply_forced_strings(target_path: Path, strings_to_force: dict[str, dict[str, str]], context: str = "") -> bool:
    """Update a target strings.xml file with a given set of "forced" strings.

    This will either modify existing strings or add new ones to the target file.

    Args:
        target_path: The path to the strings.xml file to modify.
        strings_to_force: A dictionary of strings to apply, from parse_file.
        context: A descriptive string (e.g., language name) for logging.

    Returns:
        True if the file was modified, False otherwise.

    """
    if not target_path.exists() or not strings_to_force:
        return False

    log_path = f"{context}/{target_path.name}" if context else str(target_path)
    _, target_root, _ = XMLProcessor.parse_file(target_path)
    if target_root is None:
        return False

    modified = False
    target_elements = {elem.get("name"): elem for elem in target_root.findall(".//string[@name]")}

    for name, data in strings_to_force.items():
        # ruff: noqa: ERA001
        # try:
        new_elem = DefusedET.fromstring(data["text"])
        if name in target_elements:
            existing_elem = target_elements[name]
            # Only modify if the content is actually different
            if XMLProcessor.element_to_string(existing_elem) != XMLProcessor.element_to_string(new_elem):
                # Perform a deep replacement of the element's content and attributes
                existing_elem.clear()
                existing_elem.attrib.update(new_elem.attrib)
                existing_elem.text = new_elem.text
                existing_elem.tail = new_elem.tail
                existing_elem[:] = new_elem[:]
                logger.debug("Force-updated string '%s' in %s", name, log_path)
                modified = True
        else:
            target_root.append(new_elem)
            logger.debug("Force-added missing string '%s' to %s", name, log_path)
            modified = True
        # except DefusedET.ParseError:
        #     logger.exception("Could not parse forced string XML for key '%s'", name)

    if modified:
        XMLProcessor.write_file(target_path, target_root)
        logger.info("Applied %d forced string(s) to %s.", len(strings_to_force), log_path)
        return True

    return False


def remove_strings_from_xml(file_path: Path, keys_to_remove: set[str], context: str = "") -> bool:
    """Remove strings with specified keys from an XML file.

    Args:
        file_path: The path to the XML file to modify.
        keys_to_remove: A set of string 'name' attributes to remove.
        context: A descriptive string (e.g., language name) for logging.

    Returns:
        True if the file was modified, False otherwise.

    """
    if not file_path.exists() or not keys_to_remove:
        return False

    log_path = f"{context}/{file_path.name}" if context else str(file_path)
    _, root, _ = XMLProcessor.parse_file(file_path)
    if root is None:
        return False

    # Find all elements to remove in one pass
    elements_to_remove = [elem for elem in root.findall(".//string[@name]") if elem.get("name") in keys_to_remove]

    if not elements_to_remove:
        return False

    for elem in elements_to_remove:
        root.remove(elem)
        logger.debug("Removing string '%s' from %s", elem.get("name"), log_path)

    XMLProcessor.write_file(file_path, root)
    logger.info("Removed %d string(s) from %s.", len(keys_to_remove), log_path)
    return True


def get_keys_from_xml(file_path: Path) -> set[str]:
    """Parse an XML file and returns a set of all string names.

    Args:
        file_path: Path to the XML file.

    Returns:
        A set of string names, or an empty set if parsing fails or file not found.

    """
    if not file_path.exists():
        return set()
    try:
        _, _, strings_dict = XMLProcessor.parse_file(file_path)
        return set(strings_dict.keys())
    except Exception:
        logger.exception("Failed to parse keys from %s", file_path)
        return set()


def update_or_create_xml(output_path: Path, strings_to_add: dict[str, str]) -> None:
    """Create or updates an XML file with new strings, avoiding duplicates.

    This is primarily used for generating `updated_strings.xml`.

    Args:
        output_path: The path to the XML file to create or update.
        strings_to_add: A dictionary mapping string names to their full XML elements.

    """
    if not strings_to_add:
        return

    existing_keys: set[str] = set()
    root: ET.Element = ET.Element("resources")

    if output_path.exists():
        logger.debug("File %s exists, reading existing keys.", output_path)
        _, existing_root, _ = XMLProcessor.parse_file(output_path)
        if existing_root is not None:
            root = existing_root
            for elem in root.findall(".//string[@name]"):
                if (name := elem.get("name")) is not None:
                    existing_keys.add(name)

    new_strings_count = 0
    # Sort for consistent file output
    for name, xml_string in sorted(strings_to_add.items()):
        if name not in existing_keys:
            try:
                new_elem = DefusedET.fromstring(xml_string)
                root.append(new_elem)
                new_strings_count += 1
            except DefusedET.ParseError:
                logger.exception("Could not parse XML string for key '%s': %s", name, xml_string)

    if new_strings_count > 0:
        XMLProcessor.write_file(output_path, root)
        logger.info("Updated %s with %d new string(s).", output_path, new_strings_count)
    else:
        logger.info("No new unique strings to add to %s.", output_path)


def _process_language_dir(
    lang_dir: Path,
    processor: GitDiffProcessor,
    host_changed_strings: dict[str, str],
) -> bool:
    """Process all string update logic for a single language directory.

    Args:
        lang_dir: Path to the language-specific resource directory (e.g., `values-es`).
        processor: The configured GitDiffProcessor instance.
        host_changed_strings: A dictionary of changed strings from the host language.

    Returns:
        True if any file within the language directory was modified, False otherwise.

    """
    lang_name = lang_dir.name
    logger.debug("Processing language directory: %s", lang_name)
    was_modified = False

    lang_strings_path = lang_dir / "strings.xml"
    forced_strings_path = lang_dir / "forced_strings.xml"
    updated_strings_path = lang_dir / "updated_strings.xml"

    lang_changed_strings = processor.get_changed_strings(lang_strings_path)
    host_diff_keys = set(host_changed_strings.keys())
    lang_diff_keys = set(lang_changed_strings.keys())

    # Create updated_strings.xml
    # This file contains strings that changed in the host but NOT in this translation.
    # It serves as a "to-do" list for translators.
    existing_lang_keys = get_keys_from_xml(lang_strings_path)
    strings_for_update_xml = {
        name: xml
        for name, xml in host_changed_strings.items()
        if name in existing_lang_keys and name not in lang_diff_keys
    }

    if strings_for_update_xml:
        update_or_create_xml(updated_strings_path, strings_for_update_xml)
        was_modified = True

    # Process forced_strings.xml
    if not forced_strings_path.exists():
        return was_modified

    try:
        _, _, all_forced_strings = XMLProcessor.parse_file(forced_strings_path)
        if not all_forced_strings:
            return was_modified
    except Exception:
        logger.exception("Could not parse %s, skipping forced string logic.", forced_strings_path)
        return was_modified

    # Determine which forced strings to APPLY.
    # Apply a forced string only if the corresponding string in the host
    # strings.xml was NOT part of the latest git diff. This prevents overwriting
    # a translator's recent manual change with an older forced string.
    strings_to_apply = {name: data for name, data in all_forced_strings.items() if name not in host_diff_keys}

    # Determine which forced strings to REMOVE.
    # If a string was changed in the host (the source of truth), its corresponding
    # entry in forced_strings.xml is now obsolete and should be removed to prevent
    # future conflicts.
    keys_to_remove = set(all_forced_strings.keys()) & host_diff_keys

    # Execute the operations.
    if apply_forced_strings(lang_strings_path, strings_to_apply, context=lang_name):
        was_modified = True

    if remove_strings_from_xml(forced_strings_path, keys_to_remove, context=lang_name):
        was_modified = True

    return was_modified


def process(app: str) -> None:
    """Initialize the git processor to update from diff.

    Args:
        app: The name of the application being processed.

    """
    settings = Settings()
    try:
        processor = GitDiffProcessor(settings.BASE_DIR)
    except git.InvalidGitRepositoryError:
        logger.exception("Execution failed: Script must be run within a Git repository.")
        return

    host_strings_path = settings.get_resource_path(app, "settings/host/values/strings.xml")
    translations_dir = settings.get_resource_path(app, "translations")

    if not translations_dir.is_dir():
        logger.warning("Translations directory not found, skipping: %s", translations_dir)
        return

    host_changed_strings = processor.get_changed_strings(host_strings_path)
    if host_changed_strings:
        logger.info("Found %d new/changed string(s) in host diff.", len(host_changed_strings))

    any_updates = False
    for lang_dir in sorted(translations_dir.iterdir()):
        if lang_dir.is_dir() and _process_language_dir(lang_dir, processor, host_changed_strings):
            any_updates = True

    if not any_updates:
        logger.info("✅ No new diff updates to process for any language.")
