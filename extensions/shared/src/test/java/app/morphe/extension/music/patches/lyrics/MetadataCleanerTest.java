/*
 * Copyright (C) 2026 dajiaohuang
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 */

package app.morphe.extension.music.patches.lyrics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetadataCleanerTest {
    @Test
    public void stripsTrailingFeatureCreditsFromParenthesesAndBrackets() {
        assertEquals("Synthetic track", MetadataCleaner.stripFeatureCreditSuffix(
                "Synthetic track (feat. Guest Artist)"));
        assertEquals("Synthetic track", MetadataCleaner.stripFeatureCreditSuffix(
                "Synthetic track [featuring Guest Artist]"));
    }

    @Test
    public void preservesOtherParentheticalTextAndUnbracketedCredits() {
        assertEquals("Synthetic track (Live)",
                MetadataCleaner.stripFeatureCreditSuffix("Synthetic track (Live)"));
        assertEquals("Synthetic track feat. Guest Artist",
                MetadataCleaner.stripFeatureCreditSuffix("Synthetic track feat. Guest Artist"));
    }

    @Test
    public void splitsLocalizedArtistConjunctionsWithoutSplittingWords() {
        assertArrayEquals(new String[] {"Artist One", "Artist Two"},
                MetadataCleaner.splitArtists("Artist One і Artist Two"));
        assertArrayEquals(new String[] {"Artist One", "Artist Two"},
                MetadataCleaner.splitArtists("Artist One y Artist Two"));
        assertArrayEquals(new String[] {"Artist One", "Artist Two"},
                MetadataCleaner.splitArtists("Artist One und Artist Two"));
        assertArrayEquals(new String[] {"Artist One", "Artist Two"},
                MetadataCleaner.splitArtists("Artist One Y Artist Two"));
        assertArrayEquals(new String[] {"ArtistY"},
                MetadataCleaner.splitArtists("ArtistY"));
    }

    @Test
    public void selectsPrimaryArtistBeforeLocalizedConjunction() {
        assertEquals("Artist One", MetadataCleaner.firstArtist("Artist One і Artist Two"));
        assertEquals("Artist One", MetadataCleaner.firstArtist("Artist One UND Artist Two"));
    }
}
