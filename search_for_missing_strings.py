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

# Loop through destination folders
# If you want to search only one language folder add +"/your_lang" to destination_directory
# like this => for root, dirs, files in os.walk(destination_directory+"/ar"):
for root, dirs, files in os.walk(destination_directory):
    if "strings.xml" in files:
        # Get destination file path
        destination_file = os.path.join(root, "strings.xml")
        
        # Get destination folder name (language code)
        destination_folder = os.path.dirname(destination_file)
        language_code = os.path.basename(destination_folder)

        # Output file path
        output_file = os.path.join(destination_folder, "missing_strings.xml")

        # Check if source and destination files exist
        if not os.path.isfile(source_file):
            print(f"Error: {source_file} not found.")
            exit(1)

        if not os.path.isfile(destination_file):
            print(f"Error: {destination_file} not found.")
            exit(1)

        # Extract strings from destination file
        destination_strings = extract_strings(destination_file)

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

            print(f"No missing strings for {language_code}")
        else:
            # Save missing strings to output file
            with open(output_file, 'w') as file:
                for string_tag in missing_strings:
                    file.write(string_tag)

            num_missing = len(missing_strings)
            print(f"{language_code} - {num_missing} missing strings.")

# Prompt the user to press a key before closing the terminal window
input("\nPress Enter to exit...")
sys.exit(0)  # Optional, but ensures proper termination of the script
