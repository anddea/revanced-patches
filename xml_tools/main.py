"""CLI tool to run xml commands."""

from __future__ import annotations

import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any

import click

from config import Settings
from core import log_process, setup_logging
from handlers import (
    check_icons,
    check_prefs,
    check_prefs_reverse,
    check_strings,
    missing_strings,
    remove_unused_resources,
    remove_unused_strings,
    replace_strings,
    sort_strings,
    update_from_diff,
    update_strings,
)
from utils import GitClient

if TYPE_CHECKING:
    from collections.abc import Callable
    from logging import Logger

settings = Settings()


@dataclass(frozen=True)
class Operation:
    """Represents a single CLI operation and its execution metadata."""

    key: str
    name: str
    handler: Callable[..., Any]
    args: tuple[Any, ...]


@dataclass
class CLIConfig:
    """Configuration for CLI commands."""

    log_file: str | None
    rvx_base_dir: Path | None
    app: str
    logger: Logger


@click.group(invoke_without_command=True)
@click.option("--log-file", type=str, help="Path to log file")
@click.option("--rvx-base-dir", type=str, envvar="RVX_BASE_DIR", help="Path to RVX 'patches' directory")
@click.option("-a", "--all", "run_all", is_flag=True, help="Run all commands in order")
@click.option("-m", "--missing", is_flag=True, help="Run missing strings check")
@click.option("-r", "--replace", is_flag=True, help="Run replace strings operation")
@click.option("--remove", is_flag=True, help="Remove unused strings")
@click.option("--remove-resources", is_flag=True, help="Remove unused resource files")
@click.option("-s", "--sort", is_flag=True, help="Sort strings in XML files")
@click.option("-c", "--check", is_flag=True, help="Run missing strings check")
@click.option("-p", "--prefs", is_flag=True, help="Run missing preferences check")
@click.option("-pr", "--reverse", is_flag=True, help="Run missing preferences check (reverse)")
@click.option("--icons", is_flag=True, help="Check icon preference keys against XML.")
@click.option(
    "--update-file",
    type=click.Path(exists=True, dir_okay=False, readable=True, path_type=Path),
    help="Create updated_strings.xml from keys listed in the specified file.",
)
@click.option(
    "--update-from-diff",
    is_flag=True,
    help="Check git diff and create updated_strings.xml for forced strings.",
)
@click.option("--youtube", is_flag=True, help="Process YouTube")
@click.option("--music", is_flag=True, help="Process Music")
@click.option("--reddit", is_flag=True, help="Process Reddit")
@click.option("--debug", is_flag=True, help="Enable debug logging")
@click.pass_context
def cli(ctx: click.Context, **kwargs: dict[str, Any]) -> None:
    """CLI tool for processing XML commands."""
    log_file = kwargs.get("log_file")
    log_file = log_file if isinstance(log_file, str) else None

    flags = [bool(kwargs.get("youtube")), bool(kwargs.get("music")), bool(kwargs.get("reddit"))]
    if sum(flags) > 1:
        exc: str = "You can only use one of --youtube, --music, or --reddit at a time."
        raise click.UsageError(exc)

    app: str = (
        "youtube"
        if kwargs.get("youtube")
        else "music"
        if kwargs.get("music")
        else "reddit"
        if kwargs.get("reddit")
        else "youtube"  # The default fallback if nothing is clicked
    )
    debug: bool = bool(kwargs.get("debug", False))

    logger = setup_logging(Path(log_file) if log_file else None, debug=debug)

    rvx_base_dir_str = kwargs.get("rvx_base_dir")
    if not rvx_base_dir_str:
        rvx_base_dir_str = os.getenv("RVX_BASE_DIR")

    rvx_base_dir = Path(rvx_base_dir_str) if isinstance(rvx_base_dir_str, str) else None

    ctx.obj = CLIConfig(
        log_file=log_file,
        rvx_base_dir=rvx_base_dir,
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
        "update_from_diff",
        "icons",
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
    logger = config.logger
    base_dir = config.rvx_base_dir

    git_ready = False

    # Initialize Git state if base_dir is valid
    if base_dir is not None and base_dir.exists():
        try:
            git = GitClient(base_dir)
            if git.sync_repository():
                git_ready = True
            else:
                logger.warning("Git sync failed. Operations requiring Git (Replace) will be skipped.")
        except (RuntimeError, OSError, ValueError) as e:
            logger.warning("Failed to initialize Git client: %s. Operations requiring Git will be skipped.", e)

    handlers: list[tuple[str, Callable[..., Any], list[Any]]] = [
        ("Replace Strings (YouTube)", replace_strings.process, ["youtube", base_dir]),
        ("Replace Strings (YouTube Music)", replace_strings.process, ["music", base_dir]),
        ("Remove Unused Strings (YouTube)", remove_unused_strings.process, ["youtube"]),
        ("Remove Unused Strings (YouTube Music)", remove_unused_strings.process, ["music"]),
        ("Sort Strings (YouTube)", sort_strings.process, ["youtube"]),
        ("Sort Strings (YouTube Music)", sort_strings.process, ["music"]),
        ("Sort Strings (Reddit)", sort_strings.process, ["reddit"]),
        ("Missing Strings Creation (YouTube)", missing_strings.process, ["youtube"]),
        ("Missing Strings Creation (YouTube Music)", missing_strings.process, ["music"]),
        ("Missing Strings Creation (Reddit)", missing_strings.process, ["reddit"]),
        ("Remove Unused Resources (YouTube)", remove_unused_resources.process, ["youtube"]),
        ("Remove Unused Resources (YouTube Music)", remove_unused_resources.process, ["music"]),
        ("Missing Prefs Check", check_prefs.process, ["youtube", base_dir]),
        ("Missing Prefs Check (Reverse)", check_prefs_reverse.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube)", check_strings.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube Music)", check_strings.process, ["music", base_dir]),
        ("Check Icon Preferences", check_icons.process, ["youtube"]),
        ("Update Strings from Git Diff (YouTube)", update_from_diff.process, ["youtube"]),
        ("Update Strings from Git Diff (YouTube Music)", update_from_diff.process, ["music"]),
    ]

    for name, handler, args in handlers:
        # Check if this operation requires base_dir
        requires_base_dir = base_dir in args

        if requires_base_dir:
            if base_dir is None:
                logger.warning("Skipping '%s': RVX_BASE_DIR is not defined.", name)
                continue

            if not base_dir.exists():
                logger.warning("Skipping '%s': RVX_BASE_DIR '%s' does not exist.", name, base_dir)
                continue

            # Additional check for operations requiring valid Git state
            if "Replace Strings" in name and not git_ready:
                logger.warning("Skipping '%s': Git sync failed or invalid repo.", name)
                continue

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


def _validate_base_dir(
    config: CLIConfig,
    operation_name: str,
    base_dir: Path | None,
) -> bool:
    if base_dir is None:
        config.logger.warning(
            "Skipping '%s': RVX_BASE_DIR is not defined.",
            operation_name,
        )
        return False

    if not base_dir.exists():
        config.logger.warning(
            "Skipping '%s': RVX_BASE_DIR '%s' does not exist.",
            operation_name,
            base_dir,
        )
        return False

    return True


def _run_operation(
    config: CLIConfig,
    operation: Operation,
    base_dir: Path | None,
) -> None:
    if operation.key == "replace":
        try:
            git = GitClient(base_dir or Path())
            if git.sync_repository():
                handle_operation(
                    config,
                    operation.name,
                    operation.handler,
                    *operation.args,
                )
            else:
                config.logger.warning(
                    "Skipping '%s': Git sync failed.",
                    operation.name,
                )
        except (RuntimeError, OSError) as e:
            config.logger.warning(
                "Skipping '%s': Failed to initialize Git client: %s",
                operation.name,
                e,
            )
        return

    if operation.key == "update_file" and operation.args[1] is None:
        config.logger.warning(
            "Skipping '%s': Input file path is missing.",
            operation.name,
        )
        return

    handle_operation(
        config,
        operation.name,
        operation.handler,
        *operation.args,
    )


def handle_individual_operations(
    config: CLIConfig,
    options: dict[str, Any],
) -> None:
    """Handle individual operations based on user flags."""
    base_dir = config.rvx_base_dir
    app = config.app

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
        ("update_from_diff", "Update Forced Strings from Git Diff", update_from_diff.process, (app,)),
        ("icons", "Check Icon Preferences", check_icons.process, (app,)),
    ]

    something_processed = False
    base_dir_required = {"replace", "check", "prefs", "reverse"}

    for option_key, operation_name, handler, args in operations:
        if not options.get(option_key):
            continue

        something_processed = True

        if option_key in base_dir_required and not _validate_base_dir(config, operation_name, base_dir):
            continue

        op = Operation(option_key, operation_name, handler, args)
        _run_operation(config, op, base_dir)

    if not something_processed and not options.get("run_all"):
        config.logger.info(
            "No specific operation requested. Use --help for options.",
        )


if __name__ == "__main__":
    cli()
