# Copyright (C) 2026 anddea

"""Check duplicate string text values."""

from __future__ import annotations

import logging
from collections import defaultdict
from typing import TYPE_CHECKING

from defusedxml import ElementTree as DefusedET

from config.settings import Settings

if TYPE_CHECKING:
    from pathlib import Path

logger = logging.getLogger("xml_tools")


def extract_string_texts(path: Path) -> dict[str, str]:
    """Extract string names and their text content from an XML file."""
    try:
        tree = DefusedET.parse(str(path))
    except (OSError, DefusedET.ParseError):
        logger.exception("Failed to parse %s: ", path)
        return {}

    strings: dict[str, str] = {}
    root = tree.getroot()
    if root is None:
        return strings

    for elem in root.findall(".//string"):
        name = elem.get("name")
        text = "".join(elem.itertext())
        if name and text.strip():
            strings[name] = text

    return strings


def find_duplicate_strings(path: Path) -> dict[str, list[str]]:
    """Find string names that share the same text content."""
    names_by_text: defaultdict[str, list[str]] = defaultdict(list)
    for name, text in extract_string_texts(path).items():
        names_by_text[text].append(name)

    return {text: names for text, names in names_by_text.items() if len(names) > 1}


def _get_strings_path(app: str) -> Path:
    """Get the app host strings resource checked for duplicate text."""
    return Settings().get_resource_path(app, "settings/host/values/strings.xml")


def process(app: str, _base_dir: Path | None = None) -> None:
    """Process app string resources and log duplicate string names."""
    path = _get_strings_path(app)
    if not path.exists():
        logger.warning("String file not found: %s", path)
        return

    duplicate_strings = find_duplicate_strings(path)
    if not duplicate_strings:
        logger.info("✅ No duplicate strings found")
        return

    logger.info("Duplicate string text found in %s:", path)
    for names in duplicate_strings.values():
        logger.info("  ❌ %s", ", ".join(sorted(names)))
