import os
from lxml import etree


def parse_xml(file_path):
    """
    Parse the XML file and extract the values of the 'name' attributes.
    
    Args:
        file_path (str): Path to the XML file.
    
    Returns:
        list: List of values of 'name' attributes.
    """
    tree = etree.parse(file_path)
    root = tree.getroot()

    # Extract the values of the 'name' attributes
    name_values = [element.get('name') for element in root.iter() if 'name' in element.attrib]
    return name_values, tree, root


def search_in_files(directories, name_values):
    """
    Search for the values in all files with allowed extensions within the specified directories, excluding 'strings.xml' files.
    
    Args:
        directories (list): List of directories to search in.
        name_values (list): List of 'name' attribute values to search for.
    
    Returns:
        dict: Dictionary with 'name' values as keys and list of file paths where they were found as values.
    """
    results = {name: [] for name in name_values}
    allowed_extensions = ('.kt', '.java', '.xml')

    for directory in directories:
        for root, dirs, files in os.walk(directory):
            # Ignore dot directories and the build directory
            dirs[:] = [d for d in dirs if not d.startswith('.') and d != 'build']
            for file in files:
                if file == 'strings.xml' or not file.endswith(allowed_extensions):
                    continue
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        for name in name_values:
                            if name in content:
                                results[name].append(file_path)
                except Exception as e:
                    print(f"Error reading {file_path}: {e}")
    
    return results


def remove_unused_strings(xml_file_paths, unused_names):
    """
    Remove strings with unused 'name' attributes from the specified XML files.
    
    Args:
        xml_file_paths (list): List of paths to XML files.
        unused_names (list): List of 'name' attribute values that are not used.
    """
    for file_path in xml_file_paths:
        tree = etree.parse(file_path)
        root = tree.getroot()
        
        # Find and remove elements with unused 'name' attributes
        for element in root.findall(".//*[@name]"):
            if element.get('name') in unused_names:
                root.remove(element)
        
        # Write the updated XML back to the file
        tree.write(file_path, pretty_print=True, xml_declaration=True, encoding='utf-8')


def main():
    xml_file_path = 'src/main/resources/youtube/settings/host/values/strings.xml'
    translation_dir = 'src/main/resources/youtube/translations'
    directories_to_search = [
        '../revanced-patches',
        '../revanced-integrations'
    ]

    # Parse the main XML file to get the 'name' attribute values
    name_values, tree, root = parse_xml(xml_file_path)

    # Search for the 'name' values in the specified directories
    search_results = search_in_files(directories_to_search, name_values)

    # Determine which 'name' values are not used
    unused_names = [name for name, files in search_results.items() if not files]

    # Remove unused strings from the main XML file
    remove_unused_strings([xml_file_path], unused_names)

    # Collect paths to all translation strings.xml files
    translation_files = []
    for lang_code in os.listdir(translation_dir):
        translation_file_path = os.path.join(translation_dir, lang_code, 'strings.xml')
        if os.path.isfile(translation_file_path):
            translation_files.append(translation_file_path)
    
    # Remove unused strings from all translation strings.xml files
    remove_unused_strings(translation_files, unused_names)


if __name__ == '__main__':
    main()
