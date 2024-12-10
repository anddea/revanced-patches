from pathlib import Path
import click
import os
import sys
from typing import Optional, List, Tuple, Callable
from logging import Logger

from config.settings import Settings
from core.exceptions import ConfigError
from core.logging import setup_logging, log_process
from utils.git import GitClient
from handlers import missing_prefs, missing_strings, remove_unused_strings, replace_strings, sort_strings

settings = Settings()


def get_rvx_base_dir() -> Path:
    """Get RVX base directory from environment variable.

    Returns:
        Path: The path to the RVX base directory.

    Raises:
        ConfigError: If RVX_BASE_DIR environment variable is not set.

    Note:
        This function checks for the RVX_BASE_DIR environment variable
        which must be set before running the application.
    """
    rvx_dir = os.getenv("RVX_BASE_DIR")
    if not rvx_dir:
        raise ConfigError("RVX_BASE_DIR environment variable must be set")
    return Path(rvx_dir)


def validate_rvx_base_dir(ctx: click.Context, base_dir: Optional[str] = None) -> Path:
    """Validate and return the RVX base directory path.

    Args:
        ctx (click.Context): The Click context object containing shared resources.
        base_dir (Optional[str], optional): The base directory path string. Defaults to None.

    Returns:
        Path: A validated Path object for the RVX base directory.

    Raises:
        SystemExit: If the base directory validation fails.

    Note:
        If base_dir is not provided, the function attempts to get it from
        the RVX_BASE_DIR environment variable.
    """
    if not base_dir:
        try:
            base_dir = str(get_rvx_base_dir())
        except ConfigError as e:
            ctx.obj['logger'].error(str(e))
            sys.exit(1)
    return Path(base_dir)


def process_all(app: str, base_dir: Path, logger: Logger) -> None:
    """Run all processing steps in sequence for the specified application.

    Args:
        app (str): The application to process ('youtube' or 'music').
        base_dir (Path): The base directory path for RVX operations.
        logger (Logger): The logger instance for recording operations.

    Raises:
        SystemExit: If any processing step fails or Git sync fails.
        Exception: If any handler encounters an error during execution.

    Note:
        This function executes the following steps in order:
        1. Syncs the Git repository
        2. Replaces strings for YouTube and YouTube Music
        3. Removes unused strings
        4. Sorts strings
        5. Checks for missing strings
        6. Checks for missing preferences
    """
    git = GitClient(base_dir)
    if not git.sync_repository():
        sys.exit(1)

    handlers: List[Tuple[str, Callable, List[str]]] = [
        ("Replace Strings (YouTube)", replace_strings.process, ["youtube", base_dir]),
        ("Replace Strings (YouTube Music)", replace_strings.process, ["music", base_dir]),
        ("Remove Unused Strings (YouTube)", remove_unused_strings.process, ["youtube"]),
        ("Remove Unused Strings (YouTube Music)", remove_unused_strings.process, ["music"]),
        ("Sort Strings (YouTube)", sort_strings.process, ["youtube"]),
        ("Sort Strings (YouTube Music)", sort_strings.process, ["music"]),
        ("Missing Strings Check (YouTube)", missing_strings.process, ["youtube"]),
        ("Missing Strings Check (YouTube Music)", missing_strings.process, ["music"]),
        ("Missing Prefs Check", missing_prefs.process, ["youtube", base_dir]),
    ]

    for process_name, handler, args in handlers:
        try:
            log_process(logger, process_name)
            handler(*args)
        except Exception as e:
            logger.error(f"Handler {process_name} failed: {e}")
            sys.exit(1)


@click.group(invoke_without_command=True)
@click.option("--log-file", type=str, help="Path to log file")
@click.option("--rvx-base-dir", type=str, help="Path to RVX 'patches' directory", envvar="RVX_BASE_DIR")
@click.option("-a", "--all", "run_all", is_flag=True, help="Run all commands in order")
@click.option("-m", "--missing", is_flag=True, help="Run missing strings check")
@click.option("-r", "--replace", is_flag=True, help="Run replace strings operation")
@click.option("--remove", is_flag=True, help="Remove unused strings")
@click.option("-s", "--sort", is_flag=True, help="Sort strings in XML files")
@click.option("-p", "--prefs", is_flag=True, help="Run missing preferences check")
@click.option("--youtube/--music", default=True, help="Process YouTube or Music strings")
@click.pass_context
def cli(ctx: click.Context,
        log_file: Optional[str],
        rvx_base_dir: Optional[str],
        run_all: bool,
        missing: bool,
        replace: bool,
        remove: bool,
        sort: bool,
        prefs: bool,
        youtube: bool) -> None:
    """XML processing tools for RVX patches with backwards compatibility.

    Args:
        ctx (click.Context): Click context object for sharing resources between commands.
        log_file (Optional[str]): Path to the log file. If None, logs to stdout.
        rvx_base_dir (Optional[str]): Base directory for RVX operations. Can be set via RVX_BASE_DIR env var.
        run_all (bool): Flag to run all processing steps in sequence.
        missing (bool): Flag to run missing strings check.
        replace (bool): Flag to run string replacement operation.
        remove (bool): Flag to remove unused strings.
        sort (bool): Flag to sort strings in XML files.
        prefs (bool): Flag to run missing preferences check.
        youtube (bool): Flag to process YouTube (--youtube) or Music (--music) strings.

    Raises:
        SystemExit: If any processing step fails.
        Exception: If any operation encounters an error.

    Note:
        - The function initializes logging and validates the RVX base directory when required.
        - Operations are executed in the order specified by command line flags.
        - The --youtube/--music flag determines which application's strings to process.
        - When --all is specified, all operations are run in a predefined sequence.
    """
    # Initialize the logger
    logger = setup_logging(Path(log_file) if log_file else None)

    app = "youtube" if youtube else "music"

    # Store common context
    ctx.obj = {
        "app": app,
        "logger": logger
    }

    # Only validate RVX_BASE_DIR for commands that need it
    needs_rvx_dir = run_all or replace or prefs
    if needs_rvx_dir:
        base_dir = validate_rvx_base_dir(ctx, rvx_base_dir)

    # Handle all operations if --all is specified
    if run_all:
        try:
            process_all(app, base_dir, logger)
            return
        except Exception as e:
            logger.error(f"Error during processing: {e}")
            sys.exit(1)

    # Handle individual operations
    try:
        if missing:
            log_process(logger, "Missing Strings Check")
            missing_strings.process(app)

        if prefs:
            log_process(logger, "Missing Preferences Check")
            missing_prefs.process(app, base_dir)

        if remove:
            log_process(logger, "Remove Unused Strings")
            remove_unused_strings.process(app)

        if replace:
            git = GitClient(base_dir)
            if git.sync_repository():
                log_process(logger, "Replace Strings")
                replace_strings.process(app, base_dir)

        if sort:
            log_process(logger, "Sort Strings")
            sort_strings.process(app)

    except Exception as e:
        logger.error(f"Error during processing: {e}")
        sys.exit(1)


if __name__ == "__main__":
    cli()
