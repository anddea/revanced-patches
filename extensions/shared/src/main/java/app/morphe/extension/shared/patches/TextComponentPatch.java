/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3187
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.patches;

import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class TextComponentPatch {
    /**
     * Injection point.
     * <p>
     * Logs if new litho text layout is used.
     */
    public static boolean useNewLithoTextCreation(boolean useNewLithoTextCreation) {
        // Don't force flag on/off unless debugging patch hooks,
        // because forcing off with newer YT targets causes Shorts player to show no buttons,
        // presumably because the old litho data isn't in the layout data.
        Logger.printDebug(() -> "useNewLithoTextCreation: " + useNewLithoTextCreation);
        return useNewLithoTextCreation;
    }

}
