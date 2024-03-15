#!/bin/bash

# Define source file path
source_file="src/main/resources/youtube/settings/host/values/strings.xml"

# Loop through destination folders
# Replace * with language folder name to check only one language
for destination_file in src/main/resources/youtube/translations/*/strings.xml; do
    # Get destination folder name
    destination_folder=$(dirname "$destination_file")
    
    # Extract language code from destination folder
    language_code=$(basename "$destination_folder")
    
    # Output file path
    output_file="$destination_folder/missing_strings.xml"
    
    # Check if source and destination files exist
    if [ ! -f "$source_file" ]; then
        echo "Error: $source_file not found."
        exit 1
    fi
    
    if [ ! -f "$destination_file" ]; then
        echo "Error: $destination_file not found."
        exit 1
    fi
    
    # Extract strings from source file
    source_strings=$(grep '<string name=' "$source_file")
    
    # Extract strings from destination file
    destination_strings=$(grep '<string name=' "$destination_file")
    
    # Find missing strings
    missing_strings=""
    while IFS= read -r source_string; do
        name=$(echo "$source_string" | sed -n 's/.*name="\([^"]*\)".*/\1/p')
        if ! grep -qF "name=\"$name\"" "$destination_file"; then
            missing_strings+="$source_string\n"
        fi
    done <<< "$source_strings"

    # Remove leading whitespace (including spaces and tabs) before each string in $missing_strings
    missing_strings=$(echo -e "$missing_strings" | sed 's/^[[:blank:]]*//')
    
    # Escape percent signs
    missing_strings="${missing_strings//%/%%}"

    # Check if missing strings exist
    if [ -z "$missing_strings" ]; then
        # Delete $output_file if exists
        if [ -f "$output_file" ]; then
            rm "$output_file"
        fi

        echo "No missing strings for $language_code"
    else
        # Save missing strings to output file
        printf "$missing_strings" > "$output_file"
    
        # echo "Missing strings saved to $output_file."
        num_missing=$(echo -e "$missing_strings" | grep -c '<string name=')
        echo "$language_code - $num_missing missing strings."
    fi
done
