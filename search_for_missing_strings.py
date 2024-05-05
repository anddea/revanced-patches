import os
import re
import sys

# Define source file path
source_file = "src/main/resources/youtube/settings/host/values/strings.xml"

# Define destination directory path
destination_directory = "src/main/resources/youtube/translations"


# Function to extract strings from a file
def extract_strings(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
        strings = re.findall(r'<string(?:\s+name="([^"]*)")?(.*?)>(.*?)</string>', content, re.DOTALL)
        return set(strings)


# Extract strings from source file
source_strings = extract_strings(source_file)


def add_string_to_files(string):
    # Split the multiline string into individual strings
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
            
            if re.search(rf'<string\s+name="{re.escape(name_attribute)}"', updated_content):
                updated_content = re.sub(rf'\n?\s*?<string\s+name="{re.escape(name_attribute)}"\s*>.*?</string>', '', updated_content, flags=re.DOTALL)
                with open(destination_file, 'w') as f:
                    f.write(updated_content)
            
            with open(destination_file, 'a') as f:
                stripped_line = string.strip()
                f.write(stripped_line + '\n')


def delete_string_from_files(string):
    for root, dirs, files in os.walk(destination_directory):
        for file in files:
            if file.endswith(".xml"):
                destination_file = os.path.join(root, file)
                # Read the content of the updated strings file
                with open(destination_file, 'r') as f:
                    content = f.read()
                # Extract name attribute values
                name_attributes = re.findall(r'<string\s+name="([^"]*)"\s*>', string)
                # Remove all occurrences of each name attribute if it exists
                updated_content = content
                for name_attr in name_attributes:
                    updated_content = re.sub(rf'\n?\s*?<string\s+name="{re.escape(name_attr)}"\s*>.*?</string>', '', updated_content, flags=re.DOTALL)
                # Rewrite the updated content to the file
                with open(destination_file, 'w') as f:
                    f.write(updated_content)


# Function to find missing strings
def find_missing_strings():
    num_missing = 0  # Initialize num_missing to 0
    num_updated = 0  # Initialize num_updated to 0

    for root, dirs, files in os.walk(destination_directory):
        if "strings.xml" in files:
            # Get destination file path
            destination_file = os.path.join(root, "strings.xml")
            
            # Get destination folder name (language code)
            destination_folder = os.path.dirname(destination_file)
            language_code = os.path.basename(destination_folder)

            # Output file path
            output_file = os.path.join(destination_folder, "missing-strings.xml")

            # Locate updated-strings.xml file
            updated_strings_file = os.path.join(destination_folder, "updated-strings.xml")

            # Check if source and destination files exist
            if not os.path.isfile(source_file):
                print(f"Error: {source_file} not found.")
                return

            if not os.path.isfile(destination_file):
                print(f"Error: {destination_file} not found.")
                return

            # Extract strings from destination file
            destination_strings = extract_strings(destination_file)

            # Extract strings from updated-strings.xml if exists
            if os.path.isfile(updated_strings_file):
                updated_strings = extract_strings(updated_strings_file)
                # Count name attributes in updated strings
                num_updated = sum(1 for name, _, _ in updated_strings)
            else:
                num_updated = 0

            # Find missing strings
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

            # Sort missing strings by name attribute
            missing_strings.sort(key=lambda x: re.search(r'name="([^"]*)"', x).group(1))

            # Check if missing strings exist
            if not missing_strings:
                # Delete output file if exists
                if os.path.isfile(output_file):
                    os.remove(output_file)

                num_missing = 0
            else:
                # Save missing strings to output file
                with open(output_file, 'w') as file:
                    for string_tag in missing_strings:
                        file.write(string_tag)

                num_missing = len(missing_strings)

            # Print the result
            print(f"{language_code} - {num_missing} missing strings, {num_updated} updated strings.")


# Function to sort strings in a file alphabetically by name attribute value
def sort_strings_in_file(file_path):
    # Read the content of the file
    with open(file_path, 'r') as f:
        content = f.read()
    # Extract strings from the file
    strings = re.findall(r'<string(?:\s+name="([^"]*)")?(.*?)>(.*?)</string>', content, re.DOTALL)
    # Sort strings alphabetically by name attribute value
    strings.sort(key=lambda x: x[0] if x[0] else "")
    # Reconstruct the XML content with sorted strings
    sorted_content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    sorted_content += '\n'.join(f'\t<string name="{name}"{attr}>{value}</string>' for name, attr, value in strings)
    sorted_content += '\n</resources>'
    # Rewrite the sorted content to the file
    with open(file_path, 'w') as f:
        f.write(sorted_content + "\n")


# Function to sort strings in all files within a directory
def sort_strings_in_directory(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file == "strings.xml":
                file_path = os.path.join(root, file)
                sort_strings_in_file(file_path)


# If there are no arguments, call the original script
if len(sys.argv) == 1:
    find_missing_strings()

# If the argument is -n, call the string addition function
elif len(sys.argv) == 3 and sys.argv[1] == '-n':
    string = sys.argv[2]
    add_string_to_files(string)

# If the argument is -d, call the string deletion function
elif len(sys.argv) == 3 and sys.argv[1] == '-d':
    string = sys.argv[2]
    delete_string_from_files(string)

# If the argument is -s, call the string sorting function
elif len(sys.argv) == 2 and sys.argv[1] == '-s':
    # Call the sorting function for both source file and destination directory
    sort_strings_in_file(source_file)
    sort_strings_in_directory(destination_directory)

# If neither condition is met, print a warning
else:
    print("Invalid arguments. Usage:")
    print("To run original script: script.py")
    print("To add a string to files: script.py -n 'string'")
    print("To delete a string from files: script.py -d 'string'")
    print("To sort strings alphabetically: script.py -s")

# Prompt the user to press a key before closing the terminal window
input("\nPress Enter to exit...")
sys.exit(0)  # Optional, but ensures proper termination of the script
