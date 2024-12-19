"""Core package for application logging."""

from __future__ import annotations

import logging
import sys
from typing import TYPE_CHECKING, ClassVar

if TYPE_CHECKING:
    from pathlib import Path

# ANSI escape codes for colors
BLUE: str = "\033[94m"
GREEN: str = "\033[92m"
YELLOW: str = "\033[93m"
RED: str = "\033[91m"
CYAN: str = "\033[96m"
DARK_CYAN: str = "\033[1;36m"
RESET: str = "\033[0m"


class ColorFormatter(logging.Formatter):
    """Custom formatter to add colors based on log level and special message formatting.

    Attributes:
        level_colors (dict): Mapping of log levels to their corresponding colors.

    """

    level_colors: ClassVar[dict[str, str]] = {
        "DEBUG": BLUE,
        "INFO": GREEN,
        "WARNING": YELLOW,
        "ERROR": RED,
        "CRITICAL": RED,
    }

    def format(self, record: logging.LogRecord) -> str:
        """Format the log record with colors and special message handling.

        Args:
            record (logging.LogRecord): The log record to format.

        Returns:
            str: The formatted log message with appropriate colors.

        Note:
            - Preserves original record attributes by saving and restoring them
            - Applies special coloring to "Starting process:" messages
            - Colors log levels according to severity

        """
        # Save original values
        original_levelname = record.levelname
        original_msg = record.msg

        # Color the level name
        if record.levelname in self.level_colors:
            record.levelname = f"{self.level_colors[record.levelname]}{record.levelname}{RESET}"

        # If message starts with "Starting process", color that part
        if isinstance(record.msg, str) and record.msg.startswith("Starting process:"):
            record.msg = f"{CYAN}Starting process:{DARK_CYAN}{str(record.msg).split(':', 1)[1]}{RESET}"

        # Format with colors
        formatted_message = super().format(record)

        # Restore original values
        record.levelname = original_levelname
        record.msg = original_msg

        return formatted_message


class ExitOnErrorHandler(logging.Handler):
    """Custom handler to exit the program on ERROR or CRITICAL log levels."""

    def emit(self, record: logging.LogRecord) -> None:
        """Check the log level and exit if it's ERROR or CRITICAL.

        Args:
            record (logging.LogRecord): The log record to evaluate.

        """
        if record.levelno >= logging.ERROR:
            sys.exit(1)


def setup_logging(log_file: Path | None = None, *, debug: bool = True) -> logging.Logger:
    """Configure logging with colored level names for console output and optional file logging.

    Args:
        log_file (Optional[Path]): Path to the log file. If None, only console logging is configured.
        debug (bool): Whether to enable DEBUG level logging. Defaults to True.

    Returns:
        logging.Logger: Configured logger instance.

    """
    # Create logger
    logger = logging.getLogger("xml_tools")

    # Set the base level to DEBUG if debug is True, otherwise INFO
    base_level = logging.DEBUG if debug else logging.INFO
    logger.setLevel(base_level)

    # Remove any existing handlers
    logger.handlers = []

    # Console handler with colors
    console_handler = logging.StreamHandler()
    console_handler.setLevel(base_level)  # Use same level as logger
    console_formatter = ColorFormatter("%(asctime)s - %(levelname)s - %(message)s")
    console_handler.setFormatter(console_formatter)
    logger.addHandler(console_handler)

    # File handler without colors if log_file specified
    if log_file:
        file_handler = logging.FileHandler(log_file)
        file_handler.setLevel(base_level)  # Use same level as logger
        file_formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)

    # Add the ExitOnErrorHandler
    exit_handler = ExitOnErrorHandler()
    logger.addHandler(exit_handler)

    # Log initial setup
    logger.debug("Logging system initialized")
    if log_file:
        logger.debug("Log file created at: %s", log_file)

    return logger


def log_process(logger: logging.Logger, process_name: str) -> None:
    """Log the start of a process with special formatting.

    Args:
        logger (logging.Logger): The logger instance to use.
        process_name (str): Name of the process being started.

    Note:
        Uses special color formatting for "Starting process:" messages.

    """
    logger.info("Starting process: %s", process_name)
