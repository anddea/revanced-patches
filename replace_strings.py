import os
from lxml import etree
import sys

# Set default value for the dynamic argument
default_value = "youtube"

# Parse command-line arguments
args = sys.argv[1:]

# Check for --youtube or --music arguments and set the value accordingly
if '--youtube' in args:
    value = "youtube"
    args.remove('--youtube')
elif '--music' in args:
    value = "music"
    args.remove('--music')
else:
    value = default_value

def main():
    # Define the base directory paths
    base_dir = f"src/main/resources/{value}"
    rvx_base_dir = f"rvx/revanced-patches/src/main/resources/{value}" # replace "rvx" with actual local path to the repo

    # Define the base strings.xml file paths
    base_strings_file = os.path.join(base_dir, "settings/host/values/strings.xml")
    rvx_base_strings_file = os.path.join(rvx_base_dir, "settings/host/values/strings.xml")

    # Read and update base strings.xml file
    update_strings_file(base_strings_file, rvx_base_strings_file)

    # Define the translations directories
    translations_dir = os.path.join(base_dir, "translations")
    rvx_translations_dir = os.path.join(rvx_base_dir, "translations")

    # Iterate through each language directory in "translations"
    for language_dir in os.listdir(translations_dir):
        language_path = os.path.join(translations_dir, language_dir)
        if os.path.isdir(language_path):
            strings_file = os.path.join(language_path, "strings.xml")
            rvx_strings_file = os.path.join(rvx_translations_dir, language_dir, "strings.xml")

            if os.path.exists(rvx_strings_file):
                # Read and update language-specific strings.xml file
                update_strings_file(strings_file, rvx_strings_file)

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
    source_strings_map = {string.get("name"): string.text for string in source_root.findall("string")}

    # Update target strings with source content
    for string in target_root.findall("string"):
        name = string.get("name")
        if name in source_strings_map:
            # Update existing string
            string.text = source_strings_map[name]
            # Remove the string from source map to track which are not in target
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
    Saves the XML document to a file, preserving the XML declaration and indentation.
    
    Args:
    - file_path (str): Path to save the XML file.
    - tree (etree.ElementTree): XML tree object to save.
    """
    xml_declaration = "<?xml version='1.0' encoding='utf-8'?>\n"
    xml_content = etree.tostring(tree, pretty_print=True, xml_declaration=False, encoding='unicode')
    
    # Adjust the indentation to 4 spaces
    xml_content = xml_content.replace('  <string', '    <string')

    with open(file_path, 'w', encoding='utf-8') as file:
        file.write(xml_declaration + xml_content)

if __name__ == "__main__":
    main()
