"""Check git diff and create updated_strings.xml for forced strings."""

from __future__ import annotations

import logging
import re
import xml.etree.ElementTree as ET
from typing import TYPE_CHECKING

import git
from defusedxml import ElementTree as DefusedET

from config import Settings
from utils.xml_processor import XMLProcessor

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")


class GitDiffProcessor:
    """Encapsulates Git operations to find changes in specific files."""

    def __init__(self, repo_path: Path) -> None:
        """Initialize the processor with the path to the git repository.

        Args:
            repo_path (Path): The root path of the git repository.

        """
        try:
            self.repo = git.Repo(repo_path, search_parent_directories=True)
            logger.debug("Git repository initialized at: %s", self.repo.working_dir)
        except git.InvalidGitRepositoryError:
            logger.exception("Error: Not a valid git repository: %s", repo_path)
            raise

    def get_added_strings_from_diff(self, file_path: Path) -> dict[str, str]:
        """Get the diff for a specific file, extract the name and full text of all added string resources.

        Args:
            file_path (Path): The path to the file to diff.

        Returns:
            dict[str, str]: A dictionary mapping the string 'name' to the full XML string tag.

        """
        # Return early if the file doesn't exist
        if not file_path.exists():
            return {}

        relative_path = file_path.relative_to(self.repo.working_dir)

        # Check if the file is even part of the diff to avoid unnecessary processing
        if str(relative_path) not in self.repo.git.diff("HEAD", "--name-only"):
            return {}

        diff_output = self.repo.git.diff("HEAD", "--", str(relative_path))
        if not diff_output:
            return {}

        added_strings: dict[str, str] = {}

        # This pattern correctly handles single and multi-line string tags
        pattern = re.compile(r'^\+\s*(<string name="([^"]+)"[^>]*>.*?</string>)', re.MULTILINE | re.DOTALL)

        for match in pattern.finditer(diff_output):
            full_tag = match.group(1).strip()
            string_name = match.group(2)
            added_strings[string_name] = full_tag
            logger.debug("Found added string in diff for '%s': name='%s'", relative_path, string_name)

        return added_strings


def force_update_strings(target_path: Path, force_path: Path) -> bool:
    """Force-updates a target strings.xml file from a forced_strings.xml source.

    It ensures every string in the force_path exists and is identical in the
    target_path. If a string exists, it's overwritten; if it's missing, it's added.

    Args:
        target_path: The path to the `strings.xml` to modify.
        force_path: The path to the `forced_strings.xml` source.

    Returns:
        True if the file was modified, False otherwise.

    """
    if not target_path.exists():
        logger.warning("Target file for force-update does not exist, skipping: %s", target_path)
        return False

    _, target_root, _ = XMLProcessor.parse_file(target_path)
    _, _, forced_strings = XMLProcessor.parse_file(force_path)

    if target_root is None or not forced_strings:
        return False

    modified = False
    target_elements = {elem.get("name"): elem for elem in target_root.findall(".//string[@name]")}

    for name, data in forced_strings.items():
        # try:
        new_elem = DefusedET.fromstring(data["text"])
        if name in target_elements:
            existing_elem = target_elements[name]
            # Compare raw text to see if an update is needed
            if XMLProcessor.element_to_string(existing_elem) != XMLProcessor.element_to_string(new_elem):
                # Replace attributes, text, and children
                existing_elem.clear()  # Clear children
                existing_elem.attrib.update(new_elem.attrib)
                existing_elem.text = new_elem.text
                existing_elem.tail = new_elem.tail
                existing_elem[:] = new_elem[:]  # Copy over new children
                logger.debug("Force-updated string '%s' in %s", name, target_path.name)
                modified = True
        else:
            # Add the missing string
            target_root.append(new_elem)
            logger.debug("Force-added missing string '%s' to %s", name, target_path.name)
            modified = True
        # except DefusedET.ParseError:
        #     logger.exception("Could not parse forced string for key '%s'", name)

    if modified:
        XMLProcessor.write_file(target_path, target_root)
        logger.info("Force-updated strings in %s", target_path)
        return True

    return False


def get_keys_from_xml(file_path: Path) -> set[str]:
    """Parse an XML file and return a set of all string names.

    Args:
        file_path (Path): Path to the XML file.

    Returns:
        set[str]: A set of string names, or an empty set if the file doesn't exist or is invalid.

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
    """Create or update an `updated_strings.xml` file with the provided strings.

    Args:
        output_path (Path): The path to the `updated_strings.xml` file.
        strings_to_add (dict[str, str]): A dictionary of string names to full XML tags.

    """
    if not strings_to_add:
        # This case is handled before calling, but as a safeguard
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
        logger.info("No new unique strings to add to %s (keys may already exist).", output_path)


def process(app: str) -> None:
    """Generate updated_strings.xml files in translation directories based on diff.

    Iterate through translation directories, check for
    `forced_strings.xml`, and if found, analyze git diffs for relevant changes in
    both host and translation `strings.xml` files to create `updated_strings.xml`.

    Args:
        app (str): The application name (e.g., 'youtube', 'music').

    """
    settings = Settings()
    try:
        processor = GitDiffProcessor(settings.BASE_DIR)
    except git.InvalidGitRepositoryError:
        return

    host_strings_path = settings.get_resource_path(app, "settings/host/values/strings.xml")
    translations_dir = settings.get_resource_path(app, "translations")

    if not translations_dir.is_dir():
        logger.warning("Translations directory not found: %s", translations_dir)
        return

    # Get diffs for the two main files once to avoid repeated git calls in the loop
    host_changes = processor.get_added_strings_from_diff(host_strings_path)

    total_updates = 0
    # The primary loop is by language directory
    for lang_dir in translations_dir.iterdir():
        if not lang_dir.is_dir():
            continue

        forced_strings_path = lang_dir / "forced_strings.xml"
        if not forced_strings_path.exists():
            logger.debug("No forced_strings.xml in '%s', skipping.", lang_dir.name)
            continue

        # Force-update strings in the main translation file
        lang_strings_path = lang_dir / "strings.xml"
        force_update_strings(lang_strings_path, forced_strings_path)

        # Create updated_strings.xml from host diff
        forced_keys = get_keys_from_xml(forced_strings_path)
        if not forced_keys:
            logger.info("Found empty forced_strings.xml in '%s', skipping diff check.", lang_dir.name)
            continue

        # Filter changes from the main host/strings.xml using forced keys
        strings_to_update = {name: xml_tag for name, xml_tag in host_changes.items() if name in forced_keys}

        # ruff: noqa: ERA001
        # logger.debug("Processing language '%s' with %d forced keys.", lang_dir.name, len(forced_keys))

        # # Filter changes from the main host/strings.xml
        # relevant_host_changes = {name: xml_tag for name, xml_tag in host_changes.items() if name in forced_keys}

        # # Filter changes from this specific language's strings.xml
        # lang_strings_path = lang_dir / "strings.xml"
        # lang_changes = processor.get_added_strings_from_diff(lang_strings_path)
        # relevant_lang_changes = {name: xml_tag for name, xml_tag in lang_changes.items() if name in forced_keys}

        # strings_to_update = {**relevant_host_changes, **relevant_lang_changes}

        if strings_to_update:
            logger.info(
                "Found %d relevant string additions in host for language '%s'.",
                len(strings_to_update),
                lang_dir.name,
            )
            output_path = lang_dir / "updated_strings.xml"
            update_or_create_xml(output_path, strings_to_update)
            total_updates += 1
        else:
            logger.debug("No relevant uncommitted changes found in host for language '%s'.", lang_dir.name)

    if total_updates == 0:
        logger.info("✅ No new diff updates to process for any language.")
