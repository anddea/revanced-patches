import os
import argparse
import xml.etree.ElementTree as ET


class Utils:
    @staticmethod
    def get_arguments():
        """
        Parse command-line arguments to select between YouTube or Music directories.
        Returns:
            dict: Dictionary with the paths for xml_file_path and translation_dir based on the
            chosen option.
        """
        parser = argparse.ArgumentParser(description="Select directory to perform XML operations on.")
        parser.add_argument(
            "--youtube",
            action="store_true",
            help="Specify the --youtube argument to work with YouTube settings and translations.",
        )
        parser.add_argument(
            "--music",
            action="store_true",
            help="Specify the --music argument to work with music settings and translations.",
        )

        args = parser.parse_args()

        if args.youtube:
            print("You have selected the YouTube directory.")
            return {
                "source_file": "src/main/resources/youtube/settings/host/values/strings.xml",
                "destination_directory": "src/main/resources/youtube/translations",
            }
        elif args.music:
            print("You have selected the Music directory.")
            return {
                "source_file": "src/main/resources/music/settings/host/values/strings.xml",
                "destination_directory": "src/main/resources/music/translations",
            }
        else:
            parser.error("No directory specified. Please use --youtube or --music.")

    @staticmethod
    def parse_xml(file_path):
        """
        Parse an XML file, return the values of the 'name' attributes, the tree object, and the
        root element.

        This function reads the specified XML file, parses it, and extracts the values of all 'name' attributes
        found within the XML elements. It also returns the tree object and the root element for further processing.

        Args:
            file_path (str): Path to the XML file.

        Returns:
            tuple: A tuple containing:
                - list: List of 'name' attribute values.
                - ElementTree: Parsed XML tree object.
                - Element: Root element of the XML tree.

        Raises:
            FileNotFoundError: If the specified file does not exist.
            ET.ParseError: If the file is empty or cannot be parsed, and the file will be deleted.
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

        with open(file_path, "r", encoding="utf-8") as file:
            # Check if the file is empty
            if not file.read().strip():
                os.remove(file_path)
                raise ET.ParseError(f"File is empty and has been deleted: {file_path}")

        # Parse the XML file
        tree = ET.parse(file_path)
        root = tree.getroot()

        # Extract the values of the 'name' attributes
        name_values = [element.get("name") for element in root.iter() if "name" in element.attrib]

        return name_values, tree, root
