import os
import xml.etree.ElementTree as ET
import xml.sax.saxutils as saxutils
from utils import Utils


def get_strings_dict(root):
    """
    Extract strings from the XML root and return them as a dictionary.

    :param root: Root element of the XML tree.
    :return: Dictionary with name attributes as keys and text as values.
    """
    strings_dict = {}
    for string in root.findall("string"):
        name = string.get("name")
        text = string.text
        strings_dict[name] = text
    return strings_dict


def ensure_directory_exists(directory):
    """
    Ensure that the directory exists. If it does not, create it.

    :param directory: Path to the directory.
    """
    if not os.path.exists(directory):
        os.makedirs(directory)


def read_missing_strings(missing_file_path):
    """
    Read the missing strings from the missing_strings.xml file and return them
    as a dictionary.

    :param missing_file_path: Path to the missing_strings.xml file.
    :return: Dictionary of missing strings.
    """
    if os.path.exists(missing_file_path):
        try:
            _, _, missing_root = Utils.parse_xml(missing_file_path)
            return get_strings_dict(missing_root)
        except ET.ParseError as e:
            print(f"Error parsing {missing_file_path}: {e}")
            return {}
    return {}


def escape_xml_chars(text):
    return saxutils.escape(text)


def write_missing_strings(missing_file_path, missing_strings):
    """
    Write the missing strings to the missing_strings.xml file.

    :param missing_file_path: Path to the missing_strings.xml file.
    :param missing_strings: Dictionary of missing strings to write.
    """
    ensure_directory_exists(os.path.dirname(missing_file_path))
    with open(missing_file_path, "w", encoding="utf-8") as f:
        f.write("<?xml version='1.0' encoding='utf-8'?>\n<resources>\n")
        for name, text in missing_strings.items():
            f.write(f'    <string name="{name}">{escape_xml_chars(text)}</string>\n')
        f.write("</resources>\n")


def compare_and_update_missing_file(source_dict, dest_file_path, missing_file_path):
    """
    Compare source strings with destination strings and update
    missing_strings.xml accordingly.

    :param source_dict: Dictionary of source strings.
    :param dest_file_path: Path to the destination XML file.
    :param missing_file_path: Path to the missing_strings.xml file.
    """
    if os.path.exists(dest_file_path):
        try:
            _, _, dest_root = Utils.parse_xml(dest_file_path)
            dest_dict = get_strings_dict(dest_root)
        except ET.ParseError as e:
            print(f"Error parsing {dest_file_path}: {e}")
            dest_dict = {}
    else:
        dest_dict = {}

    # Read existing missing strings
    missing_strings = read_missing_strings(missing_file_path)

    # Update missing strings based on comparison with destination strings
    for name, text in source_dict.items():
        if name in dest_dict:
            if name in missing_strings:
                del missing_strings[name]
        else:
            missing_strings[name] = text

    # Write updated missing strings back to the file if not empty,
    # otherwise delete the file
    if missing_strings:
        write_missing_strings(missing_file_path, missing_strings)
    elif os.path.exists(missing_file_path):
        print(f"Deleting empty missing strings file: {missing_file_path}")
        os.remove(missing_file_path)


def main():
    """
    Main function to handle the XML parsing, comparison, and updating process.
    """
    # Get the directories based on the user selection (YouTube or Music)
    args = Utils.get_arguments()
    source_file = args["source_file"]
    destination_directory = args["destination_directory"]

    try:
        _, _, source_root = Utils.parse_xml(source_file)
    except (FileNotFoundError, ET.ParseError) as e:
        print(f"Error parsing source file {source_file}: {e}")
        return

    source_dict = get_strings_dict(source_root)

    for dirpath, dirnames, filenames in os.walk(destination_directory):
        for dirname in dirnames:
            lang_dir = os.path.join(dirpath, dirname)
            dest_file_path = os.path.join(lang_dir, "strings.xml")
            missing_file_path = os.path.join(lang_dir, "missing_strings.xml")
            compare_and_update_missing_file(source_dict, dest_file_path, missing_file_path)


if __name__ == "__main__":
    main()
