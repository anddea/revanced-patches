"""CLI tool to run xml commands."""

from __future__ import annotations

import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Callable

import click

from config import Settings
from core import log_process, setup_logging
from handlers import (
    check_prefs,
    check_strings,
    missing_strings,
    remove_unused_strings,
    replace_strings,
    sort_strings,
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


def get_rvx_base_dir(logger: Logger) -> Path:
    """Get the RVX base directory from the environment."""
    rvx_dir = os.getenv("RVX_BASE_DIR")
    if not rvx_dir:
        logger.error("RVX_BASE_DIR must be provided for replace operation.")
        sys.exit(1)
    return Path(rvx_dir)


def is_rvx_dir_needed(options: dict) -> bool:
    """Determine if rvx_base_dir validation is needed based on options."""
    return options.get("run_all") or options.get("replace") or options.get("prefs") or options.get("check")


@click.group(invoke_without_command=True)
@click.option("--log-file", type=str, help="Path to log file")
@click.option("--rvx-base-dir", type=str, envvar="RVX_BASE_DIR", help="Path to RVX 'patches' directory")
@click.option("-a", "--all", "run_all", is_flag=True, help="Run all commands in order")
@click.option("-m", "--missing", is_flag=True, help="Run missing strings check")
@click.option("-r", "--replace", is_flag=True, help="Run replace strings operation")
@click.option("--remove", is_flag=True, help="Remove unused strings")
@click.option("-s", "--sort", is_flag=True, help="Sort strings in XML files")
@click.option("-c", "--check", is_flag=True, help="Run missing strings check")
@click.option("-p", "--prefs", is_flag=True, help="Run missing preferences check")
@click.option("--youtube/--music", default=True, help="Process YouTube or Music strings")
@click.pass_context
def cli(ctx: click.Context, **kwargs: dict[str, bool | str | None]) -> None:
    """CLI tool for processing XML commands."""
    log_file = kwargs.get("log_file")
    app = "youtube" if kwargs.get("youtube") else "music"

    logger = setup_logging(Path(log_file) if log_file else None)

    rvx_base_dir = kwargs.get("rvx_base_dir") or get_rvx_base_dir(logger) if is_rvx_dir_needed(kwargs) else None

    ctx.obj = CLIConfig(
        log_file=log_file,
        rvx_base_dir=Path(rvx_base_dir) if rvx_base_dir else None,
        app=app,
        logger=logger,
    )

    if kwargs.get("run_all"):
        process_all(ctx.obj)

    handle_individual_operations(ctx.obj, kwargs)


def process_all(config: CLIConfig) -> None:
    """Run all operations in sequence."""
    logger = config.logger
    base_dir = config.rvx_base_dir

    git = GitClient(base_dir)
    if not git.sync_repository():
        sys.exit(1)

    handlers: list[tuple[str, Callable, list[str]]] = [
        ("Replace Strings (YouTube)", replace_strings.process, ["youtube", base_dir]),
        ("Replace Strings (YouTube Music)", replace_strings.process, ["music", base_dir]),
        ("Remove Unused Strings (YouTube)", remove_unused_strings.process, ["youtube"]),
        ("Remove Unused Strings (YouTube Music)", remove_unused_strings.process, ["music"]),
        ("Sort Strings (YouTube)", sort_strings.process, ["youtube"]),
        ("Sort Strings (YouTube Music)", sort_strings.process, ["music"]),
        ("Missing Strings Creation (YouTube)", missing_strings.process, ["youtube"]),
        ("Missing Strings Creation (YouTube Music)", missing_strings.process, ["music"]),
        ("Missing Prefs Check", check_prefs.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube)", check_strings.process, ["youtube", base_dir]),
        ("Missing Strings Check (YouTube Music)", check_strings.process, ["music", base_dir]),
    ]

    for name, handler, args in handlers:
        log_process(logger, name)
        handler(*args)


def handle_individual_operations(config: CLIConfig, options: dict) -> None:
    """Handle individual operations based on user flags."""
    logger = config.logger
    base_dir = config.rvx_base_dir
    app = config.app

    try:
        if options.get("missing"):
            log_process(logger, "Missing Strings Check")
            missing_strings.process(app)

        if options.get("remove"):
            log_process(logger, "Remove Unused Strings")
            remove_unused_strings.process(app)

        if options.get("replace"):
            git = GitClient(base_dir)
            if git.sync_repository():
                log_process(logger, "Replace Strings")
                replace_strings.process(app, base_dir)

        if options.get("sort"):
            log_process(logger, "Sort Strings")
            sort_strings.process(app)

        if options.get("check"):
            log_process(logger, "Check Strings")
            check_strings.process(app, base_dir)

        if options.get("prefs"):
            log_process(logger, "Check Preferences")
            check_prefs.process(app, base_dir)

    except Exception:
        logger.exception("Error during processing: ")
        sys.exit(1)


if __name__ == "__main__":
    cli()
