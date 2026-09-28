/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.spoof;

import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

@SuppressWarnings("unused")
public class SpoofAppVersionPatch {

    private static final boolean DISABLE_BOLD_ICONS = isSpoofingToLessThan("20.30.00");

    /**
     * Injection point.
     * Used on YouTube 20.31 ~ 21.04.
     */
    public static boolean disableShortsBoldIcons(boolean original) {
        return !DISABLE_BOLD_ICONS && original;
    }

    /**
     * Injection point.
     * Used on YouTube 21.05+.
     *
     * <p>Called after the universal app-version override and only for the Reel endpoints.
     */
    public static String getShortsAppVersionOverride(String version) {
        return DISABLE_BOLD_ICONS
                ? "20.30.40" // Oldest version that supports new Shorts overlay endpoint response.
                : version;
    }
}
