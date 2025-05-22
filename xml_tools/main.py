"""CLI tool to run xml commands."""

from __future__ import annotations

import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any, Callable

import click

from config import Settings
from core import log_process, setup_logging
from handlers import (
    check_prefs,
    check_prefs_reverse,
    check_strings,
    missing_strings,
    remove_unused_resources,
    remove_unused_strings,
    replace_strings,
    sort_strings,
    update_strings,
)
from utils import GitClient

if TYPE_CHECKING:
    from logging import Logger

settings = Settings()


@dataclass
class CLIConfig:
    """Configuration for CLI commands."""

    log_file: str | None
    rvx_base_dir: Path | None
    app: str
    logger: Logger


def get_rvx_base_dir(logger: Logger) -> str:
    """Get the RVX base directory from the environment."""
    rvx_dir = os.getenv("RVX_BASE_DIR")
    if not rvx_dir:
        logger.error("RVX_BASE_DIR must be provided for replace operation.")
        sys.exit(1)
    return rvx_dir


def is_rvx_dir_needed(options: dict[str, Any]) -> bool:
    """Determine if rvx_base_dir validation is needed based on options."""
    # Update operation doesn't strictly need RVX dir unless source keys could come from there
    # For now, assume it only uses local host strings.xml
    return any(options.get(key) for key in ["run_all", "replace", "prefs", "reverse", "check"])


@click.group(invoke_without_command=True)
@click.option("--log-file", type=str, help="Path to log file")
@click.option("--rvx-base-dir", type=str, envvar="RVX_BASE_DIR", help="Path to RVX 'patches' directory")
@click.option("-a", "--all", "run_all", is_flag=True, help="Run all commands in order")
@click.option("-m", "--missing", is_flag=True, help="Run missing strings check")
@click.option("-r", "--replace", is_flag=True, help="Run replace strings operation")
@click.option("--remove", is_flag=True, help="Remove unused strings")
@click.option("--remove-resources", is_flag=True, help="Remove unused resource files")  # Add this line
@click.option("-s", "--sort", is_flag=True, help="Sort strings in XML files")
@click.option("-c", "--check", is_flag=True, help="Run missing strings check")
@click.option("-p", "--prefs", is_flag=True, help="Run missing preferences check")
@click.option("-pr", "--reverse", is_flag=True, help="Run missing preferences check (reverse)")
@click.option(
    "--update-file",
    type=click.Path(exists=True, dir_okay=False, readable=True, path_type=Path),
    help="Create updated_strings.xml from keys listed in the specified file.",
)
@click.option("--youtube/--music", default=True, help="Process YouTube or Music strings")
@click.pass_context
def cli(ctx: click.Context, **kwargs: dict[str, Any]) -> None:
    """CLI tool for processing XML commands."""
    log_file = kwargs.get("log_file")
    log_file = log_file if isinstance(log_file, str) else None

    app: str = "youtube" if kwargs.get("youtube") else "music"

    logger = setup_logging(Path(log_file) if log_file else None)

    rvx_base_dir = kwargs.get("rvx_base_dir")
    rvx_base_dir = rvx_base_dir if isinstance(rvx_base_dir, str) else None

    if is_rvx_dir_needed(kwargs) and not rvx_base_dir:
        rvx_base_dir = get_rvx_base_dir(logger)

    ctx.obj = CLIConfig(
        log_file=log_file,
        rvx_base_dir=Path(rvx_base_dir) if rvx_base_dir else None,
        app=app,
        logger=logger,
    )

    command_flags = [
        "run_all",
        "missing",
        "replace",
        "remove",
        "remove_resources",
        "sort",
        "check",
        "prefs",
        "reverse",
        "update_file",
    ]
    if kwargs.get("run_all"):
        process_all(ctx.obj)
    elif any(kwargs.get(flag) for flag in command_flags):
        handle_individual_operations(ctx.obj, kwargs)
    else:
        # If no command flag/option is provided, show help
        click.echo(ctx.get_help())
        sys.exit(0)


def process_all(config: CLIConfig) -> None:
    """Run all operations in sequence."""
    # Note: The 'update_strings' command requires a specific input file,
    # so it's generally NOT included in 'run_all'. Keep it this way unless
    # there's a standard input file path to use.
    logger = config.logger
    base_dir = config.rvx_base_dir

    if base_dir is None:
        logger.error("Base directory (RVX_BASE_DIR) is required for '--all' operation.")
        sys.exit(1)

    git = GitClient(base_dir)
    if not git.sync_repository():
        sys.exit(1)

    handlers: list[tuple[str, Callable[..., Any], list[str | Path]]] = [
        ("Replace Strings (YouTube)", replace_strings.process, ["youtube", base_dir]),
        ("Replace Strings (YouTube Music)", replace_strings.process, ["music", base_dir]),
        ("Remove Unused Strings (YouTube)", remove_unused_strings.process, ["youtube"]),
        ("Remove Unused Strings (YouTube Music)", remove_unused_strings.process, ["music"]),
        ("Remove Unused Resources (YouTube)", remove_unused_resources.process, ["youtube"]),
        ("Remove Unused Resources (YouTube Music)", remove_unused_resources.process, ["music"]),
        ("Sort Strings (YouTube)", sort_strings.process, ["youtube"]),
        ("Sort Strings (YouTube Music)", sort_strings.process, ["music"]),
        ("Missing Strings Creation (YouTube)", missing_strings.process, ["youtube"]),
        ("Missing Strings Creation (YouTube Music)", missing_strings.process, ["music"]),
        ("Missing Prefs Check", check_prefs.process, ["youtube", base_dir]),
        ("Missing Prefs Check (Reverse)", check_prefs_reverse.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube)", check_strings.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube Music)", check_strings.process, ["music", base_dir]),
    ]

    for name, handler, args in handlers:
        log_process(logger, name)
        typed_args = [arg if isinstance(arg, Path) else str(arg) for arg in args]
        try:
            handler(*typed_args)
        except Exception:
            logger.exception("Error during process step '%s': ", name)
            sys.exit(1)


def handle_operation(
    config: CLIConfig,
    operation_name: str,
    handler: Callable[..., Any],
    *args: tuple[Any, ...],
) -> None:
    """Handle a single operation, including logging and error handling."""
    log_process(config.logger, operation_name)
    try:
        # Ensure Path arguments are passed correctly if needed
        handler(*args)
    except Exception:
        config.logger.exception("Error during %s: ", operation_name)
        sys.exit(1)


def handle_individual_operations(config: CLIConfig, options: dict[str, Any]) -> None:
    """Handle individual operations based on user flags."""
    base_dir = config.rvx_base_dir
    app = config.app
    git = GitClient(base_dir) if base_dir else None

    operations: list[tuple[str, str, Callable[..., Any], tuple[Any, ...]]] = [
        ("missing", "Missing Strings Check", missing_strings.process, (app,)),
        ("remove", "Remove Unused Strings", remove_unused_strings.process, (app,)),
        ("remove_resources", "Remove Unused Resources", remove_unused_resources.process, (app,)),
        ("sort", "Sort Strings", sort_strings.process, (app,)),
        ("replace", "Replace Strings", replace_strings.process, (app, base_dir)),
        ("check", "Check Strings", check_strings.process, (app, base_dir)),
        ("prefs", "Check Preferences", check_prefs.process, (app, base_dir)),
        ("reverse", "Check Preferences (Reverse)", check_prefs_reverse.process, (app, base_dir)),
        ("update_file", "Update Strings from File", update_strings.process, (app, options.get("update_file"))),
    ]

    something_processed = False
    for option_key, operation_name, handler, args in operations:
        if options.get(option_key):
            something_processed = True
            # Validation for operations requiring base_dir
            if option_key in ("replace", "check", "prefs", "reverse") and base_dir is None:
                config.logger.error(
                    "Base directory (RVX_BASE_DIR) is required for %s operation.",
                    operation_name.lower(),
                )
                sys.exit(1)

            # Special handling for 'replace' which needs git sync
            if option_key == "replace":
                if git is not None:
                    if git.sync_repository():
                        handle_operation(config, operation_name, handler, *args)
                    else:
                        sys.exit(1)
                else:
                    config.logger.error("Git client could not be initialized (base_dir missing?).")
                    sys.exit(1)
            elif option_key == "update_file":
                if args[1] is None:
                    config.logger.error("Input file path is missing for update operation.")
                    sys.exit(1)
                handle_operation(config, operation_name, handler, *args)
            else:
                handle_operation(config, operation_name, handler, *args)

    if not something_processed and not options.get("run_all"):
        config.logger.info("No specific operation requested. Use --help for options.")


if __name__ == "__main__":
    cli()
