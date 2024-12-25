"""Settings."""

from dataclasses import dataclass
from pathlib import Path


@dataclass
class Settings:
    """Application settings and configuration for XML processing tools.

    This class manages all path configurations and settings for the XML processing tools.
    It automatically resolves absolute paths based on the project structure and
    provides methods to access resource locations.

    Attributes:
        XML_TOOLS_DIR (Path): Absolute path to the xml_tools directory
        BASE_DIR (Path): Absolute path to the project root directory
        SRC_DIR (Path): Absolute path to the source directory
        RESOURCES_DIR (Path): Absolute path to the resources directory

    """

    def __post_init__(self) -> None:
        """Initialize all path attributes with absolute paths.

        This method is automatically called after class instantiation and sets up
        all directory paths as absolute paths based on the location of this settings file.

        Returns:
            None

        Raises:
            FileNotFoundError: If critical directories cannot be found

        """
        # Get absolute path to the xml_tools directory (where settings.py is located)
        self.XML_TOOLS_DIR = Path(__file__).resolve().parent.parent

        # Get absolute path to the project root (parent of xml_tools)
        self.BASE_DIR = self.XML_TOOLS_DIR.parent

        # Define all other paths as absolute paths
        self.SRC_DIR = (self.BASE_DIR / "patches/src").resolve()
        self.RESOURCES_DIR = (self.SRC_DIR / "main" / "resources").resolve()

    # XML file settings
    XML_ENCODING: str = "utf-8"
    MAX_LINE_LENGTH: int = 120

    def get_resource_path(self, app: str, resource_type: str) -> Path:
        """Get absolute path to a specific resource directory or file.

        Args:
            app (str): Application identifier (e.g., 'youtube' or 'music')
            resource_type (str): Type of resource or path relative to the app directory
                               (e.g., 'settings/xml/prefs.xml')

        Returns:
            Path: Absolute path to the requested resource

        Example:
            >>> settings = Settings()
            >>> path = settings.get_resource_path('youtube', 'settings/xml/prefs.xml')
            >>> str(path)
            '/absolute/path/to/project/src/main/resources/youtube/settings/xml/prefs.xml'

        """
        return (self.RESOURCES_DIR / app / resource_type).resolve()
