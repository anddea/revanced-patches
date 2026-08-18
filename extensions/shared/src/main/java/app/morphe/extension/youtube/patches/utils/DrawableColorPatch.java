/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.utils;

import org.apache.commons.lang3.ArrayUtils;

import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.youtube.patches.theme.ThemePatch;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_COLORS = {
            0xFF282828, // drawer content view background
            0xFF212121, // comments chip background
            0xFF181818, // music related results panel background
            0xFF0F0F0F, // comments chip background (new layout)
            0xFA212121, // video chapters list background
    };

    private static final int[] LIGHT_COLORS = {
            -1,         // comments chip background
            0xFFF9F9F9, // music related results panel background
            0xFAFFFFFF, // video chapters list background
    };

    public static int getLithoColor(int colorValue) {
        if (ArrayUtils.contains(DARK_COLORS, colorValue)) {
            // Stock keeps YouTube's distinct dark shades instead of flattening them to one color.
            if (ThemePatch.isStockDarkTheme()) return colorValue;
            return BaseThemeUtils.getThemeDarkColor();
        } else if (ArrayUtils.contains(LIGHT_COLORS, colorValue)) {
            return BaseThemeUtils.getThemeLightColor();
        }
        return colorValue;
    }
}
