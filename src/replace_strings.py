import argparse
import os
import re
from lxml import etree


def parse_arguments():
    """
    Parses command-line arguments using argparse.

    Returns:
    - argparse.Namespace: The parsed arguments namespace.
    """
    parser = argparse.ArgumentParser(
        description="Process RVX patches operations."
    )

    # Add optional argument for --youtube or --music
    parser.add_argument(
        "--youtube", action="store_true", help="Use YouTube as the value."
    )
    parser.add_argument(
        "--music", action="store_true", help="Use music as the value."
    )

    # Add required argument for --rvx-base-dir
    parser.add_argument(
        "--rvx-base-dir",
        type=str,
        required=True,
        help="Specify the base directory of RVX patches operations.",
    )

    return parser.parse_args()


def main():
    # Parse command-line arguments
    args = parse_arguments()

    # Determine value based on --youtube or --music flags
    if args.youtube:
        value = "youtube"
    elif args.music:
        value = "music"
    else:
        value = "youtube"  # Default value

    # Validate and retrieve rvx_base_dir
    rvx_base_dir = args.rvx_base_dir

    # Define the base directory paths
    base_dir = f"src/main/resources/{value}"
    rvx_base_dir_path = os.path.join(rvx_base_dir, "src/main/resources", value)

    # Define the base strings.xml file paths
    base_strings_file = os.path.join(
        base_dir, "settings/host/values/strings.xml"
    )
    rvx_base_strings_file = os.path.join(
        rvx_base_dir_path, "settings/host/values/strings.xml"
    )

    # Read and update base strings.xml file
    update_strings_file(base_strings_file, rvx_base_strings_file)

    # Define the translations directories
    translations_dir = os.path.join(base_dir, "translations")
    rvx_translations_dir = os.path.join(rvx_base_dir_path, "translations")

    # Iterate through each language directory in "translations"
    for language_dir in os.listdir(translations_dir):
        language_path = os.path.join(translations_dir, language_dir)
        if os.path.isdir(language_path):
            strings_file = os.path.join(language_path, "strings.xml")
            rvx_strings_file = os.path.join(
                rvx_translations_dir, language_dir, "strings.xml"
            )

            if os.path.exists(rvx_strings_file):
                # Read and update language-specific strings.xml file
                update_strings_file(strings_file, rvx_strings_file)


def convert_to_positional_format(text):
    """
    Converts non-positional format specifiers in the given text to positional
    format specifiers only if there are multiple specifiers, even of different
    types.

    Args:
    - text (str): The text to convert.

    Returns:
    - str: The text with converted format specifiers, if applicable.
    """
    # Regular expression to match format specifiers like %s, %d, etc.
    specifier_regex = re.compile(r"%([sd])")
    matches = specifier_regex.findall(text)

    # Return original text if less than 2 specifiers are found
    if len(matches) < 2:
        return text

    # Replace each specifier with its positional counterpart
    for i, match in enumerate(matches, start=1):
        text = re.sub(f"%{match}", f"%{i}${match}", text, count=1)

    return text


def update_strings_file(target_file, source_file):
    """
    Updates the target XML file with strings from the source XML file.
    Adds new strings from source to target if not already present.

    Args:
    - target_file (str): Path to the target XML file (this).
    - source_file (str): Path to the source XML file (rvx).
    """
    target_tree = parse_xml_file(target_file)
    source_tree = parse_xml_file(source_file)

    target_root = target_tree.getroot()
    source_root = source_tree.getroot()

    # Create a dictionary for source strings
    source_strings_map = {
        string.get("name"): convert_to_positional_format(string.text)
        for string in source_root.findall("string")
    }

    # Update target strings with source content
    for string in target_root.findall("string"):
        name = string.get("name")
        if name in source_strings_map:
            # Update existing string
            string.text = source_strings_map[name]
            # Remove the string from source map to track which are not in
            # target
            del source_strings_map[name]

    # Add new strings from source to target
    for name, text in source_strings_map.items():
        # Create a new <string> element
        new_string_elem = etree.Element("string", name=name)
        new_string_elem.text = text
        # Append it to the target root
        target_root.append(new_string_elem)

    # Save the updated XML content back to the target file
    save_xml_file(target_file, target_tree)


def parse_xml_file(file_path):
    """
    Parses an XML file and returns the ElementTree object.

    Args:
    - file_path (str): Path to the XML file to parse.

    Returns:
    - etree.ElementTree: Parsed XML tree object.
    """
    parser = etree.XMLParser(remove_blank_text=True)
    tree = etree.parse(file_path, parser)
    return tree


def save_xml_file(file_path, tree):
    """
    Saves the XML document to a file, preserving the XML declaration and
    indentation.

    Args:
    - file_path (str): Path to save the XML file.
    - tree (etree.ElementTree): XML tree object to save.
    """
    xml_declaration = "<?xml version='1.0' encoding='utf-8'?>\n"
    xml_content = etree.tostring(
        tree, pretty_print=True, xml_declaration=False, encoding="unicode"
    )

    # Adjust the indentation to 4 spaces
    xml_content = xml_content.replace("  <string", "    <string")

    with open(file_path, "w", encoding="utf-8") as file:
        file.write(xml_declaration + xml_content)


if __name__ == "__main__":
    main()
