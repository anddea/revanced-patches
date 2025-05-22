"""Remove unused resource files from the resources directory."""

import logging
import os
from pathlib import Path

from config.settings import Settings

logger: logging.Logger = logging.getLogger("xml_tools")

# Allowed file extensions to search for resource references
ALLOWED_EXTENSIONS: tuple[str, ...] = (".kt", ".java", ".xml")
# Directories to search for references
SEARCH_DIRECTORIES: list[str] = [str(Settings().BASE_DIR.parent / "revanced-patches")]
# Prefixes of resource names to exclude from removal
PREFIX_BLACKLIST: tuple[str, ...] = (
    "yt_wordmark_header",
    "yt_premium_wordmark_header",
)


def get_resource_names(directory: Path) -> set[str]:
    """Recursively collect file names (without extensions) from the resources directory.

    Args:
        directory: Path to the resources directory (e.g., resources/youtube).

    Returns:
        A set of file names without extensions.

    """
    resource_names: set[str] = set()

    try:
        for item in directory.rglob("*"):
            if item.is_file():
                resource_names.add(item.stem)
    except OSError:
        logger.exception("Failed to scan resources directory %s: ", directory)

    return resource_names


def search_in_files(directories: list[str], resource_names: set[str]) -> dict[str, list[str]]:
    """Search for resource names in all files within specified directories.

    Args:
        directories: List of directory paths to search.
        resource_names: Set of resource names (without extensions) to search for.

    Returns:
        A dictionary mapping each resource name to a list of file paths where it was found.

    """
    results: dict[str, list[str]] = {name: [] for name in resource_names}

    for directory in directories:
        abs_dir: Path = Path(directory).resolve()
        logger.info("Searching in directory: %s (exists: %s)", abs_dir, abs_dir.exists())

        try:
            for root, dirs, files in os.walk(directory):
                # Skip hidden and build directories
                dirs[:] = [d for d in dirs if not d.startswith(".") and d != "build"]

                for file in files:
                    if not file.endswith(ALLOWED_EXTENSIONS):
                        continue

                    file_path: Path = Path(root) / file
                    try:
                        with file_path.open(encoding="utf-8") as f:
                            content: str = f.read()
                            for name in resource_names:
                                # Check if the resource name appears in the file content
                                if name in content:
                                    results[name].append(str(file_path))
                    except (OSError, UnicodeDecodeError):
                        logger.exception("Error reading %s: ", file_path)
        except OSError:
            logger.exception("Error walking directory %s: ", abs_dir)

    return results


def remove_empty_directories(resources_dir: Path) -> None:
    """Recursively remove empty directories, excluding hidden and build directories.

    Args:
        resources_dir: Path to the resources directory to process.

    Notes:
        Uses a bottom-up approach to ensure subdirectories are processed before parents.
        Performs multiple passes to handle directories that become empty after removal.

    """
    max_passes: int = 5  # Limit to prevent infinite loops
    pass_count: int = 0
    removed_any: bool = True

    while removed_any and pass_count < max_passes:
        removed_any = False
        pass_count += 1
        logger.debug("Starting empty directory removal pass %d", pass_count)

        # Collect directories in bottom-up order using list comprehension
        directories: list[Path] = [dir_path for dir_path in resources_dir.rglob("*") if dir_path.is_dir()]

        # Sort directories by depth (deepest first) to ensure bottom-up processing
        directories.sort(key=lambda p: len(p.parts), reverse=True)

        for dir_path in directories:
            if (
                dir_path.is_dir()
                and not any(dir_path.iterdir())  # Directory is empty
                and not dir_path.name.startswith(".")  # Not a hidden directory
                and "build" not in dir_path.parts  # Not under a build directory
            ):
                try:
                    dir_path.rmdir()
                    logger.info("Removed empty directory: %s", dir_path)
                    removed_any = True
                except OSError:
                    logger.exception("Failed to remove empty directory %s: ", dir_path)
            elif (
                dir_path.is_dir()
                and not any(dir_path.iterdir())
                and (dir_path.name.startswith(".") or "build" in dir_path.parts)
            ):
                logger.debug("Skipped directory %s (hidden or under build)", dir_path)

    if pass_count >= max_passes:
        logger.warning("Reached maximum passes (%d) for empty directory removal", max_passes)


def remove_unused_resource_files(app: str) -> None:
    """Remove unused resource files and empty directories from the resources directory.

    Args:
        app: The application identifier (e.g., 'youtube', 'music').

    Notes:
        - Scans the resources directory to collect all file names without extensions.
        - Searches for references to these names in specified directories.
        - Removes files that are not referenced anywhere and do not start with blacklisted prefixes.
        - Skips files in the translations directory to avoid affecting translation strings.
        - Removes empty directories, excluding those starting with a dot or under a 'build' directory.

    """
    settings: Settings = Settings()
    resources_dir: Path = settings.get_resource_path(app, "")

    try:
        # Get all resource names (without extensions)
        resource_names: set[str] = get_resource_names(resources_dir)
        logger.info("Found %d resource names in %s", len(resource_names), resources_dir)

        # Find where each resource name is used
        search_results: dict[str, list[str]] = search_in_files(SEARCH_DIRECTORIES, resource_names)

        # Identify unused resources, excluding those with blacklisted prefixes
        unused_resources: set[str] = {
            name
            for name, files in search_results.items()
            if not files and not any(name.startswith(prefix) for prefix in PREFIX_BLACKLIST)
        }
        logger.info("Found %d unused resources", len(unused_resources))

        # Process the resources directory to remove unused files
        for item in resources_dir.rglob("*"):
            if item.is_file() and item.stem in unused_resources:
                # Skip files in the translations directory
                if "translations" in item.parts:
                    continue
                try:
                    item.unlink()
                    logger.info("Removed unused resource file: %s", item)
                except OSError:
                    logger.exception("Failed to remove unused resource file %s: ", item)

        # Remove empty directories
        remove_empty_directories(resources_dir)

    except Exception:
        logger.exception("Error during unused resources removal for app '%s': ", app)


def process(app: str) -> None:
    """Process the application to remove unused resource files.

    Args:
        app: The application identifier (e.g., 'youtube', 'music').

    """
    logger.info("Starting process: Remove Unused Resources")
    remove_unused_resource_files(app)
