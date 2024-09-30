import os
from lxml import etree
from utils import Utils

# Constants for blacklisted and prefixed strings
BLACKLISTED_STRINGS = (
    # YouTube
    "revanced_remember_video_quality_mobile",
    "revanced_remember_video_quality_wifi",
    # YouTube Music
    "revanced_sb_api_url_sum",
    "revanced_sb_enabled",
    "revanced_sb_enabled_sum",
    "revanced_sb_toast_on_skip",
    "revanced_sb_toast_on_skip_sum",
)
PREFIX_TO_IGNORE = (
    "revanced_icon_",
    "revanced_spoof_app_version_target_entry_",
    "revanced_spoof_streaming_data_side_effects_",
)


def get_base_name(name):
    """Return the base name by stripping '_title' or '_summary' suffix."""
    if name.endswith("_title"):
        return name[:-6]
    elif name.endswith("_summary"):
        return name[:-8]
    elif name.endswith("_summary_off"):
        return name[:-12]
    elif name.endswith("_summary_on"):
        return name[:-11]
    return name


def search_in_files(directories, name_values):
    """
    Search for the values in all files with allowed extensions within the
    specified directories, excluding 'strings.xml' files. It also checks
    for the base string by stripping the '_title' and '_summary' suffixes.

    Args:
        directories (list): List of directories to search in.
        name_values (list): List of 'name' attribute values to search for.

    Returns:
        dict: Dictionary with 'name' values as keys and list of file paths
        where they were found as values.
    """
    results = {name: [] for name in name_values}
    allowed_extensions = (".kt", ".java", ".xml")

    for directory in directories:
        for root, dirs, files in os.walk(directory):
            # Ignore dot directories and the build directory
            dirs[:] = [d for d in dirs if not d.startswith(".") and d != "build"]
            for file in files:
                if file in ("strings.xml", "missing_strings.xml") or not file.endswith(allowed_extensions):
                    continue
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, "r", encoding="utf-8") as f:
                        content = f.read()
                        for name in name_values:
                            # Check if the name exists in the content first
                            if name in content:
                                results[name].append(file_path)
                            else:
                                # If not, then check the base name
                                base_name = get_base_name(name)
                                if base_name in content:
                                    results[name].append(file_path)
                except Exception as e:
                    print(f"Error reading {file_path}: {e}")

    return results


def should_remove(name, unused_names):
    """
    Determine whether a string with the given 'name' attribute should be
    removed. It checks both the original name and its base form without the
    '_title' or '_summary' suffix.

    Args:
        name (str): The value of the 'name' attribute.
        unused_names (list): List of 'name' attribute values that are not used.

    Returns:
        bool: True if the element should be removed, False otherwise.
    """
    base_name = get_base_name(name)
    return (
        (name in unused_names or base_name in unused_names)
        and name not in BLACKLISTED_STRINGS
        and not any(name.startswith(prefix) for prefix in PREFIX_TO_IGNORE)
    )


def remove_unused_strings(xml_file_paths, unused_names):
    """
    Remove strings with unused 'name' attributes from the specified XML files
    and write the sorted strings back to the file.

    Args:
        xml_file_paths (list): List of paths to XML files.
        unused_names (list): List of 'name' attribute values that are not used.
    """
    for file_path in xml_file_paths:
        tree = etree.parse(file_path)
        root = tree.getroot()

        # Create a dictionary of strings to keep
        strings_dict = {}
        for element in root.findall(".//*[@name]"):
            name = element.get("name")
            if not should_remove(name, unused_names):
                strings_dict[name] = element.text

        # Write the sorted strings back to the file
        write_sorted_strings(file_path, strings_dict)


def check_translation_files(main_xml_path, translation_files):
    """
    Check each translation file against the main XML file, remove strings
    that don't exist in the main XML file, and write the sorted strings back.

    Args:
        main_xml_path (str): Path to the main XML file.
        translation_files (list): List of paths to translation XML files.
    """
    main_tree = etree.parse(main_xml_path)
    main_root = main_tree.getroot()
    main_names = set(element.get("name") for element in main_root.findall(".//*[@name]"))

    for translation_file in translation_files:
        translation_tree = etree.parse(translation_file)
        translation_root = translation_tree.getroot()

        # Create a dictionary of strings to keep
        strings_dict = {}
        for element in translation_root.findall(".//*[@name]"):
            name = element.get("name")
            if name in main_names:
                strings_dict[name] = element.text

        # Write the sorted strings back to the file
        write_sorted_strings(translation_file, strings_dict)


def write_sorted_strings(file_path, strings_dict):
    """
    Write the strings to the XML file sorted by their name attributes.

    Args:
        file_path (str): Path to the XML file.
        strings_dict (dict): Dictionary of strings to write.
    """
    ensure_directory_exists(os.path.dirname(file_path))

    # Create the root element and add strings sorted by name
    root = etree.Element("resources")
    for name in sorted(strings_dict.keys()):
        string_element = etree.Element("string", name=name)
        string_element.text = strings_dict[name]
        root.append(string_element)

    # Write the XML file with 4-space indentation
    tree = etree.ElementTree(root)
    xml_bytes = etree.tostring(tree, encoding="utf-8", pretty_print=True, xml_declaration=True)

    # Manually adjust the indentation to 4 spaces
    xml_string = xml_bytes.decode("utf-8").replace("  <string", "    <string")

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(xml_string)


def ensure_directory_exists(directory):
    """
    Ensure that the specified directory exists.

    Args:
        directory (str): Path to the directory.
    """
    os.makedirs(directory, exist_ok=True)


def main():
    # Get the directories based on the user selection (YouTube or Music)
    args = Utils.get_arguments()
    xml_file_path = args["source_file"]
    translation_dir = args["destination_directory"]

    directories_to_search = ["../revanced-patches", "../revanced-integrations"]

    # Parse the main XML file to get the 'name' attribute values
    name_values, _, _ = Utils.parse_xml(xml_file_path)

    # Search for the 'name' values in the specified directories
    search_results = search_in_files(directories_to_search, name_values)

    # Determine which 'name' values are not used
    unused_names = [name for name, files in search_results.items() if not files]

    # Remove unused strings from the main XML file
    remove_unused_strings([xml_file_path], unused_names)

    # Collect paths to all translation strings.xml files
    translation_files = [
        f
        for lang_code in os.listdir(translation_dir)
        for f in (
            os.path.join(translation_dir, lang_code, "strings.xml"),
            os.path.join(translation_dir, lang_code, "missing_strings.xml"),
        )
        if os.path.isfile(f)
    ]

    # Remove unused strings from all translation strings.xml files
    remove_unused_strings(translation_files, unused_names)

    # Check translation files against the main XML file
    check_translation_files(xml_file_path, translation_files)


if __name__ == "__main__":
    main()
