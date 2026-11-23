# Copyright (C) 2026 anddea

"""Regression checks for semantic string diffs."""

from __future__ import annotations

import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from handlers import update_from_diff
from handlers.update_from_diff import EnhancedXMLProcessor


class StringDiffTests(unittest.TestCase):
    """Keep layout changes out of translator update queues."""

    def test_surrounding_layout_does_not_change_string(self) -> None:
        """Comments, indentation and ordering must not flag unchanged strings."""
        before = '<resources><string name="a">A</string>\n    <!-- note -->\n<string name="b">B</string></resources>'
        after = '<resources>\n<string name="b">B</string>\n\n<string name="a">A</string>\n</resources>'
        self.assertEqual(  # noqa: PT009 - Use unittest assertions with the standard-library runner.
            EnhancedXMLProcessor.parse_string(before)[2],
            EnhancedXMLProcessor.parse_string(after)[2],
        )

    def test_resource_changes_are_preserved(self) -> None:
        """Detect changes to text, attributes, markup and whitespace inside strings."""
        original = '<string name="a">Hello <b>world</b> !</string>'
        changes = (
            original.replace("Hello", "Hi"),
            original.replace('name="a"', 'name="a" translatable="false"'),
            original.replace("<b>world</b>", "<i>world</i>"),
            original.replace("</b> !", "</b>!"),
        )
        before = EnhancedXMLProcessor.parse_string(f"<resources>{original}</resources>")[2]
        for changed in changes:
            with self.subTest(changed=changed):
                after = EnhancedXMLProcessor.parse_string(f"<resources>{changed}</resources>")[2]
                self.assertNotEqual(before, after)  # noqa: PT009 - Standard-library unittest assertion.


class EmptyUpdateTests(unittest.TestCase):
    """Remove empty queues even when no forced strings file exists."""

    @staticmethod
    def _run_process(lang_dir: Path, host_changes: dict[str, str], lang_changes: dict[str, str]) -> None:
        """Exercise the public entry point with isolated resource paths and Git diffs."""
        with (
            patch.object(update_from_diff, "Settings") as settings_class,
            patch.object(update_from_diff, "GitDiffProcessor") as processor_class,
        ):
            settings_class.return_value.get_resource_path.side_effect = [
                lang_dir.parent / "host.xml", lang_dir.parent,
            ]
            processor_class.return_value.get_changed_strings.side_effect = [host_changes, lang_changes]
            update_from_diff.process("music")

    def test_empty_update_file_is_removed(self) -> None:
        """Clean existing empty files and files emptied by translator changes."""
        for content in ("", '<string name="a">A</string>'):
            with self.subTest(content=content), TemporaryDirectory() as directory:
                lang_dir = Path(directory) / "ko-rKR"
                lang_dir.mkdir()
                updated_path = lang_dir / "updated_strings.xml"
                updated_path.write_text(f"<resources>{content}</resources>", encoding="utf-8")
                self._run_process(lang_dir, {}, {"a": '<string name="a">Translated</string>'})
                self.assertFalse(updated_path.exists())  # noqa: PT009

    def test_pending_update_is_preserved(self) -> None:
        """Keep a queue populated with a real host change."""
        with TemporaryDirectory() as directory:
            lang_dir = Path(directory) / "ko-rKR"
            lang_dir.mkdir()
            changed = {"a": '<string name="a">New text</string>'}
            self._run_process(lang_dir, changed, {})
            self.assertTrue((lang_dir / "updated_strings.xml").exists())  # noqa: PT009
