import os
import re
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

# Define the source file path based on the dynamic value
source_file = f"src/main/resources/{value}/settings/host/values/strings.xml"

# Define the destination directory path based on the dynamic value
destination_directory = f"src/main/resources/{value}/translations"

def extract_strings(file_path):
    """
    Extracts strings from an XML file.

    Args:
        file_path (str): The path to the XML file.

    Returns:
        set: A set of tuples containing the extracted strings.
    """
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
        strings = re.findall(r'<string(?:\s+name="([^"]*)")?(.*?)>(.*?)</string>', content, re.DOTALL)
        return set(strings)

# Extract strings from the source file
source_strings = extract_strings(source_file)

def add_string_to_files(string):
    """
    Adds a string to all relevant files in the destination directory.

    Args:
        string (str): The string to be added.
    """
    strings = string.split('\n')
    for string in strings:
        # Extract the name attribute from the string
        name_attribute_match = re.search(r'name="([^"]*)"', string)
        if not name_attribute_match:
            continue
        name_attribute = name_attribute_match.group(1)

        for root, _, files in os.walk(destination_directory):
            if "strings.xml" not in files:
                continue

            destination_file = os.path.join(root, "updated-strings.xml")
            missing_strings_file = os.path.join(root, "missing-strings.xml")

            with open(missing_strings_file, 'r') as f:
                missing_content = f.read()

            # Remove existing occurrences of the string if it is found in the missing strings file
            if re.search(rf'<string\s+name="{re.escape(name_attribute)}"', missing_content):
                updated_content = ""
                with open(destination_file, 'r') as f:
                    updated_content = f.read()
                updated_content = re.sub(rf'\n?\s*?<string\s+name="{re.escape(name_attribute)}"\s*>.*?</string>', '', updated_content, flags=re.DOTALL)
                with open(destination_file, 'w') as f:
                    f.write(updated_content)
                break

            with open(destination_file, 'r') as f:
                updated_content = f.read()

            # Remove existing occurrences of the string if it is found in the updated strings file
            if re.search(rf'<string\s+name="{re.escape(name_attribute)}"', updated_content):
                updated_content = re.sub(rf'\n?\s*?<string\s+name="{re.escape(name_attribute)}"\s*>.*?</string>', '', updated_content, flags=re.DOTALL)
                with open(destination_file, 'w') as f:
                    f.write(updated_content)

            # Add the string to the updated strings file
            with open(destination_file, 'a') as f:
                stripped_line = string.strip()
                f.write(stripped_line + '\n')

def delete_string_from_files(string):
    """
    Deletes a string from all relevant files in the destination directory.

    Args:
        string (str): The string to be deleted.
    """
    for root, dirs, files in os.walk(destination_directory):
        for file in files:
            if file.endswith(".xml"):
                destination_file = os.path.join(root, file)
                with open(destination_file, 'r') as f:
                    content = f.read()
                name_attributes = re.findall(r'<string\s+name="([^"]*)"\s*>', string)
                updated_content = content
                # Remove all occurrences of each name attribute if it exists
                for name_attr in name_attributes:
                    updated_content = re.sub(rf'\n?\s*?<string\s+name="{re.escape(name_attr)}"\s*>.*?</string>', '', updated_content, flags=re.DOTALL)
                with open(destination_file, 'w') as f:
                    f.write(updated_content)

def find_missing_strings():
    """
    Finds and reports missing strings in the destination directory.
    """
    num_missing = 0
    num_updated = 0

    for root, dirs, files in os.walk(destination_directory):
        if "strings.xml" in files:
            destination_file = os.path.join(root, "strings.xml")
            destination_folder = os.path.dirname(destination_file)
            language_code = os.path.basename(destination_folder)
            output_file = os.path.join(destination_folder, "missing-strings.xml")
            updated_strings_file = os.path.join(destination_folder, "updated-strings.xml")

            if not os.path.isfile(source_file):
                print(f"Error: {source_file} not found.")
                return

            if not os.path.isfile(destination_file):
                print(f"Error: {destination_file} not found.")
                return

            destination_strings = extract_strings(destination_file)

            if os.path.isfile(updated_strings_file):
                updated_strings = extract_strings(updated_strings_file)
                num_updated = sum(1 for name, _, _ in updated_strings)
            else:
                num_updated = 0

            missing_strings = []

            for name, attributes, content in source_strings:
                if name not in {name for name, _, _ in destination_strings}:
                    string_tag = f'<string'
                    if name:
                        string_tag += f' name="{name}"'
                    if attributes:
                        string_tag += f' {attributes.strip()}'
                    string_tag += f'>{content}</string>\n'
                    missing_strings.append(string_tag)

            missing_strings.sort(key=lambda x: re.search(r'name="([^"]*)"', x).group(1))

            if not missing_strings:
                if os.path.isfile(output_file):
                    os.remove(output_file)
                num_missing = 0
            else:
                with open(output_file, 'w') as file:
                    for string_tag in missing_strings:
                        file.write(string_tag)
                num_missing = len(missing_strings)

            print(f"{language_code} - {num_missing} missing strings, {num_updated} updated strings.")

def sort_strings_in_file(file_path):
    """
    Sorts the <string> elements in an XML file alphabetically by their 'name' attribute.

    Args:
        file_path (str): The path to the XML file.
    """
    with open(file_path, 'r') as f:
        content = f.read()

    strings = re.findall(r'<string([^>]*)>(.*?)</string>', content, re.DOTALL)
    strings.sort(key=lambda x: re.search(r'name="([^"]*)"', x[0]).group(1) if re.search(r'name="([^"]*)"', x[0]) else "")

    sorted_content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    sorted_content += '\n'.join(f'\t<string{attributes}>{value}</string>' for attributes, value in strings)
    sorted_content += '\n</resources>'

    with open(file_path, 'w') as f:
        f.write(sorted_content + "\n")

def sort_strings_in_directory(directory):
    """
    Sorts the strings in all XML files within a directory.

    Args:
        directory (str): The path to the directory.
    """
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file == "strings.xml":
                file_path = os.path.join(root, file)
                sort_strings_in_file(file_path)

# Determine which function to call based on the parsed arguments
if len(args) == 0:
    find_missing_strings()
elif len(args) == 2 and args[0] == '-n':
    string = args[1]
    add_string_to_files(string)
elif len(args) == 2 and args[0] == '-d':
    string = args[1]
    delete_string_from_files(string)
elif len(args) == 1 and args[0] == '-s':
    sort_strings_in_file(source_file)
    sort_strings_in_directory(destination_directory)
else:
    print("Invalid arguments. Usage:")
    print("To run original script: script.py")
    print("To add a string to files: script.py -n 'string'")
    print("To delete a string from files: script.py -d 'string'")
    print("To sort strings alphabetically: script.py -s")
    print("Optional arguments: --youtube or --music")

# Prompt the user to press a key before closing the terminal window
input("\nPress Enter to exit...")
sys.exit(0)
