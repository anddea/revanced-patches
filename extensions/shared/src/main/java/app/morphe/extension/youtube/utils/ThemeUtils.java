/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.utils;

import static app.morphe.extension.shared.utils.ResourceUtils.getColor;
import static app.morphe.extension.shared.utils.ResourceUtils.getDrawable;
import static app.morphe.extension.shared.utils.ResourceUtils.getStyleIdentifier;

import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;
import androidx.annotation.AttrRes;

import app.morphe.extension.shared.utils.BaseThemeUtils;

public class ThemeUtils extends BaseThemeUtils {

    /**
     * @return The resource a theme attribute points to, such as a style or a drawable,
     *         or 0 if the theme does not set it.
     */
    public static int getThemeResourceId(Context context, @AttrRes int attribute) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attribute, value, true)
                ? value.resourceId
                : 0;
    }

    /**
     * Styles the text the way {@code android:textAppearance} and {@code android:textColor}
     * set to theme attributes do in a layout.
     */
    public static void setThemeTextStyle(TextView view, @AttrRes int textAppearanceAttribute,
                                         @AttrRes int textColorAttribute) {
        Context context = view.getContext();
        final int textAppearance = getThemeResourceId(context, textAppearanceAttribute);
        if (textAppearance != 0) {
            view.setTextAppearance(textAppearance);
        }

        TypedValue textColor = new TypedValue();
        if (context.getTheme().resolveAttribute(textColorAttribute, textColor, true)) {
            if (textColor.resourceId != 0) {
                view.setTextColor(context.getColorStateList(textColor.resourceId));
            } else {
                view.setTextColor(textColor.data);
            }
        }
    }



    public static int getThemeId() {
        final String themeName = isDarkModeEnabled()
                ? "Theme.YouTube.Settings.Dark"
                : "Theme.YouTube.Settings";

        return getStyleIdentifier(themeName);
    }

    public static Drawable getTrashButtonDrawable() {
        final String drawableName = isDarkModeEnabled()
                ? "yt_outline_trash_can_white_24"
                : "yt_outline_trash_can_black_24";

        return getDrawable(drawableName);
    }

    public static int getDialogBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black1"
                : "yt_white1";

        return getColor(colorName);
    }

    /**
     * Adjusts the background color based on the current theme.
     *
     * @param isHandleBar If true, applies a stronger darkening factor (0.9) for the handle bar in light theme;
     *                    if false, applies a standard darkening factor (0.95) for other elements in light theme.
     * @return A modified background color, lightened by 20% for dark themes or darkened by 5% (or 10% for handle bar)
     * for light themes to ensure visual contrast.
     */
    public static int getAdjustedBackgroundColor(boolean isHandleBar) {
        final int baseColor = getDialogBackgroundColor();
        float darkThemeFactor = isHandleBar ? 1.25f : 1.115f; // 1.25f for handleBar, 1.115f for others in dark theme.
        float lightThemeFactor = isHandleBar ? 0.9f : 0.95f; // 0.9f for handleBar, 0.95f for others in light theme.
        return isDarkModeEnabled()
                ? adjustColorBrightness(baseColor, darkThemeFactor)  // Lighten for dark theme.
                : adjustColorBrightness(baseColor, lightThemeFactor); // Darken for light theme.
    }

    /**
     * Since {@link android.widget.Toolbar} is used instead of {@link android.support.v7.widget.Toolbar},
     * We have to manually specify the toolbar background.
     *
     * @return toolbar background color.
     */
    public static int getToolbarBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black3"   // Color names used in the light theme
                : "yt_white1";  // Color names used in the dark theme

        return getColor(colorName);
    }
}
