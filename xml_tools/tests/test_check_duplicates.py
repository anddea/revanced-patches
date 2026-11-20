# Copyright (C) 2026 anddea

"""Regression checks for intentional duplicate resource groups."""

from pathlib import Path
from unittest import TestCase
from unittest.mock import patch

from handlers import check_duplicates


class DuplicateStringTests(TestCase):
    """Allow the dynamic Rime label without hiding other duplicates."""

    def test_only_exact_blacklisted_group_is_allowed(self) -> None:
        """Report ordinary duplicates and additions to an otherwise allowed group."""
        allowed = {
            "morphe_music_lyrics_rime_dict_title": "Rime Wordlist",
            "morphe_music_lyrics_romanization_provider_rime": "Rime Wordlist",
        }
        cases: tuple[tuple[dict[str, str], dict[str, list[str]]], ...] = (
            (allowed, {}),
            ({"a": "Same", "b": "Same"}, {"Same": ["a", "b"]}),
            ({**allowed, "extra": "Rime Wordlist"}, {"Rime Wordlist": [*allowed, "extra"]}),
        )
        for strings, expected in cases:
            with (
                self.subTest(strings=strings),
                patch.object(check_duplicates, "extract_string_texts", return_value=strings),
            ):
                self.assertEqual(  # noqa: PT009 - Standard-library unittest assertion.
                    check_duplicates.find_duplicate_strings(Path("strings.xml")), expected,
                )
