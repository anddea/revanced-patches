/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.general;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

final class OpaqueSystemBars {

    /**
     * The system draws its navigation buttons over this color until Android 15, after which
     * the window always stays transparent there and the views painted here show through.
     *
     * @return An opaque color, if the app theme made the system navigation buttons translucent.
     */
    @ColorInt
    static int systemNavigationBarColor(@ColorInt int original) {
        if (Color.alpha(original) == 255) {
            return original;
        }

        return ShortsPlayerState.getCurrent().isOpen() ? Color.BLACK : BaseThemeUtils.getAppBackgroundColor();
    }

}
