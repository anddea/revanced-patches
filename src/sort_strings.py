import os
import sys
from lxml import etree as ET

# Set default value for the dynamic argument
default_value = "youtube"

# Parse command-line arguments
args = sys.argv[1:]

# Check for --youtube or --music arguments and set the value accordingly
if "--youtube" in args:
    value = "youtube"
    args.remove("--youtube")
elif "--music" in args:
    value = "music"
    args.remove("--music")
else:
    value = default_value

# Define the source file path based on the dynamic value
source_file = f"src/main/resources/{value}/settings/host/values/strings.xml"

# Define the destination directory path based on the dynamic value
destination_directory = f"src/main/resources/{value}/translations"


def parse_xml(file_path):
    """
    Parse the XML file and return the root element.

    :param file_path: Path to the XML file.
    :return: Root element of the XML tree.
    :raises FileNotFoundError: If the file does not exist.
    :raises ET.XMLSyntaxError: If the file is empty or cannot be parsed.
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    with open(file_path, "r", encoding="utf-8") as file:
        if not file.read().strip():
            print(f"File is empty, deleting: {file_path}")
            os.remove(file_path)
            raise ET.XMLSyntaxError(
                f"File is empty and has been deleted: {file_path}"
            )

    tree = ET.parse(file_path)
    return tree.getroot()


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


def write_sorted_strings(file_path, strings_dict):
    """
    Write the strings to the XML file sorted by their name attributes.

    :param file_path: Path to the XML file.
    :param strings_dict: Dictionary of strings to write.
    """
    ensure_directory_exists(os.path.dirname(file_path))

    # Create the root element and add strings sorted by name
    root = ET.Element("resources")
    for name in sorted(strings_dict.keys()):
        string_element = ET.Element("string", name=name)
        string_element.text = strings_dict[name]
        root.append(string_element)

    # Write the XML file with 4-space indentation
    tree = ET.ElementTree(root)
    xml_bytes = ET.tostring(
        tree, encoding="utf-8", pretty_print=True, xml_declaration=True
    )

    # Manually adjust the indentation to 4 spaces
    xml_string = xml_bytes.decode("utf-8").replace("  <string", "    <string")

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(xml_string)


def ensure_directory_exists(directory):
    """
    Ensure that the directory exists. If it does not, create it.

    :param directory: Path to the directory.
    """
    if not os.path.exists(directory):
        os.makedirs(directory)


def sort_strings_in_file(file_path):
    """
    Sort the strings in the XML file at the given path.

    :param file_path: Path to the XML file.
    """
    try:
        root = parse_xml(file_path)
        strings_dict = get_strings_dict(root)
        write_sorted_strings(file_path, strings_dict)
    except (FileNotFoundError, ET.XMLSyntaxError) as e:
        print(f"Error processing file {file_path}: {e}")


def main():
    """
    Main function to handle the sorting of strings in the XML files.
    """
    # Sort strings in the source file
    sort_strings_in_file(source_file)

    # Sort strings in each language-specific destination file
    for dirpath, dirnames, filenames in os.walk(destination_directory):
        for dirname in dirnames:
            lang_dir = os.path.join(dirpath, dirname)
            dest_file_path = os.path.join(lang_dir, "strings.xml")
            sort_strings_in_file(dest_file_path)


if __name__ == "__main__":
    main()
