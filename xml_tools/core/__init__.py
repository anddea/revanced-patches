"""Core package for application logging."""

from .xml_logging import log_process, setup_logging

__all__: list[str] = ["log_process", "setup_logging"]
