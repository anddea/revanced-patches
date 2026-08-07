# Copyright (C) 2026 anddea

"""Remove unwanted dots from YouTube and YouTube Music string resources."""

from __future__ import annotations

import logging
import re
from typing import TYPE_CHECKING

from config.settings import Settings

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")
APPS: tuple[str, ...] = ("youtube", "music")
RESOURCE_FILE_NAMES: frozenset[str] = frozenset(
    {
        "forced_strings.xml",
        "missing_strings.xml",
        "strings.xml",
        "updated_strings.xml",
    },
)


def remove_dots(text: str) -> str:
    """Apply the dot cleanup rules.

    Single-dot rules only match exact single-dot endings so the following
    double-dot rules can expand exact double dots into ellipses.
    """
    text = re.sub(r"(?<!\.)\.</", "</", text)
    text = re.sub(r"(?<!\.)\.\.</", "...</", text)
    text = re.sub(r'(?<!\.)\."</', '"</', text)
    text = re.sub(r'(?<!\.)\.\."</', '..."</', text)
    text = re.sub(r"(?<!\.)\.(?=[^\S\r\n]*\r?\n)", "", text)
    text = re.sub(r"(?<!\.)\.\.(?=[^\S\r\n]*\r?\n)", "...", text)
    text = text.replace("\n</string>", "</string>")
    text = text.replace(". (", " (")
    text = re.sub(r"(?<!\.)\.%s", "%s", text)
    return re.sub(r"(• [^\n]*)\.$", r"\1", text, flags=re.MULTILINE)


def process_file(path: Path) -> None:
    """Clean one strings.xml file, writing it only when content changes."""
    if not path.is_file():
        logger.warning("Strings file does not exist: %s", path)
        return

    text = path.read_text(encoding="utf-8")
    cleaned_text = remove_dots(text)
    if cleaned_text == text:
        return

    path.write_text(cleaned_text, encoding="utf-8")
    logger.info("Removed unwanted dots from %s", path)


def process() -> None:
    """Clean host and translation resource XMLs for YouTube and YouTube Music."""
    settings = Settings()

    for app in APPS:
        process_file(settings.get_resource_path(app, "settings/host/values/strings.xml"))

        translations_path = settings.get_resource_path(app, "translations")
        if not translations_path.is_dir():
            logger.warning("Translations directory does not exist: %s", translations_path)
            continue

        resource_paths = (path for path in translations_path.rglob("*.xml") if path.name in RESOURCE_FILE_NAMES)
        for path in sorted(resource_paths):
            process_file(path)
