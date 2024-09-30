import re
import os
import argparse


def extract_keys(file_path):
    """
    Extract keys from the XML file.

    Args:
        file_path (str): The path to the XML file.

    Returns:
        set: A set of keys extracted from the file.
    """
    key_pattern = re.compile(r'android:key="(\w+)"')  # Compile the regex pattern to match keys
    keys_found = set()  # Use a set to store unique keys

    # Open the XML file and search for the keys
    with open(file_path, "r", encoding="utf-8") as file:
        for line in file:
            matches = key_pattern.findall(line)  # Find all keys in the line
            keys_found.update(matches)  # Add found keys to the set

    return keys_found


def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description="Search for keys in XML files.")
    parser.add_argument(
        "--rvx-base-dir",
        type=str,
        required=True,
        help="Specify the base directory of RVX patches operations.",
    )

    # Parse the arguments
    args = parser.parse_args()
    base_dir = args.rvx_base_dir

    # Define the file paths based on the base directory provided
    prefs_file_1 = os.path.join(
        base_dir, "src/main/resources/youtube/settings/xml/revanced_prefs.xml"
    )
    prefs_file_2 = "src/main/resources/youtube/settings/xml/revanced_prefs.xml"

    # Check if files exist
    if not os.path.exists(prefs_file_1) or not os.path.exists(prefs_file_2):
        print("Error: One or both XML files are missing.")
        return

    # Extract keys from the first file
    keys_in_file_1 = extract_keys(prefs_file_1)

    # Extract keys from the second file
    keys_in_file_2 = extract_keys(prefs_file_2)

    # Find keys that are in the first file but not in the second
    keys_not_in_file_2 = keys_in_file_1 - keys_in_file_2

    # Print the keys not found in the second file
    for key in keys_not_in_file_2:
        print(key)


if __name__ == "__main__":
    main()
