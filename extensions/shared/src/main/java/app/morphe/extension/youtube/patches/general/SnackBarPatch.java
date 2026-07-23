/*
 * Copyright (C) 2025-2026 anddea
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

package app.morphe.extension.youtube.patches.general;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("unused")
public final class SnackBarPatch {
    private static final boolean HIDE_SNACK_BAR =
            Settings.HIDE_SNACK_BAR.get();
    private static final boolean HIDE_SERVER_SIDE_SNACK_BAR =
            Settings.HIDE_SERVER_SIDE_SNACK_BAR.get();
    private static final boolean CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND =
            !HIDE_SNACK_BAR && !HIDE_SERVER_SIDE_SNACK_BAR && Settings.CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND.get();
    private static final boolean INVERT_SNACK_BAR_THEME =
            !HIDE_SNACK_BAR && Settings.INVERT_SNACK_BAR_THEME.get();
    private static final boolean INVERT_SERVER_SIDE_SNACK_BAR_THEME =
            !HIDE_SERVER_SIDE_SNACK_BAR && INVERT_SNACK_BAR_THEME;
    private static final int SNACK_BAR_BLACK_COLOR = 0xFF0F0F0F;
    private static final int SNACK_BAR_WHITE_COLOR = 0xFFF1F1F1;
    private static int blackColor = 0;
    private static int whiteColor = 0;

    public static boolean hideSnackBar() {
        return HIDE_SNACK_BAR;
    }

    public static void hideLithoSnackBar(FrameLayout frameLayout) {
        if (HIDE_SERVER_SIDE_SNACK_BAR) {
            Utils.hideViewByLayoutParams(frameLayout);
        }
    }

    public static void setLithoSnackBarBackground(View view) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND) {
            int snackBarRoundedCornersBackgroundIdentifier =
                    ResourceUtils.getDrawableIdentifier("snackbar_rounded_corners_background");
            Context mContext = invertSnackBarTheme(view.getContext());
            Drawable snackBarRoundedCornersBackground = mContext.getDrawable(snackBarRoundedCornersBackgroundIdentifier);
            if (snackBarRoundedCornersBackground != null) {
                view.setBackground(snackBarRoundedCornersBackground);
            }
        }
    }

    private static final ThreadLocal<Boolean> isCreatingLithoSnackBar = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void enterLithoSnackBarCreation() {
        isCreatingLithoSnackBar.set(Boolean.TRUE);
    }

    public static void exitLithoSnackBarCreation() {
        isCreatingLithoSnackBar.set(Boolean.FALSE);
    }

    public static void setLithoSnackBarBackgroundColor(FrameLayout frameLayout, int color) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND) {
            return;
        }
        frameLayout.setBackgroundColor(color);
    }

    public static Context invertSnackBarTheme(Context mContext) {
        if (INVERT_SNACK_BAR_THEME) {
            String styleId = ThemeUtils.isDarkModeEnabled()
                    ? "Base.Theme.YouTube.Light"
                    : "Base.Theme.YouTube.Dark";
            int styleIdentifier = ResourceUtils.getStyleIdentifier(styleId);
            mContext = new ContextThemeWrapper(mContext, styleIdentifier);
        }

        return mContext;
    }

    public static Enum<?> invertSnackBarTheme(Enum<?> appTheme, Enum<?> darkTheme) {
        if (INVERT_SNACK_BAR_THEME) {
            return appTheme == darkTheme
                    ? null
                    : darkTheme;
        }

        return appTheme;
    }

    public static void setLithoSnackBarView(View view) {
        if (view != null) {
            int tagKey = ResourceUtils.getDrawableIdentifier("snackbar_rounded_corners_background");
            if (tagKey != 0) {
                view.setTag(tagKey, Boolean.TRUE);
            }
        }
    }

    public static int getLithoColor(Drawable drawable, int originalValue) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND && isSnackbarDrawable(drawable)) {
            if (originalValue == SNACK_BAR_BLACK_COLOR) {
                return INVERT_SERVER_SIDE_SNACK_BAR_THEME
                        ? getWhiteColor()
                        : getBlackColor();
            } else if (originalValue == SNACK_BAR_WHITE_COLOR) {
                return INVERT_SERVER_SIDE_SNACK_BAR_THEME
                        ? getBlackColor()
                        : getWhiteColor();
            }
        }

        return originalValue;
    }

    private static boolean isSnackbarDrawable(Drawable drawable) {
        if (Boolean.TRUE.equals(isCreatingLithoSnackBar.get())) {
            return true;
        }
        if (drawable == null) {
            return false;
        }
        Drawable.Callback callback = drawable.getCallback();
        View view = getViewFromCallback(callback);
        if (view == null) {
            return false;
        }
        int tagKey = ResourceUtils.getDrawableIdentifier("snackbar_rounded_corners_background");
        int leadingAssetTagId = ResourceUtils.getIdIdentifier("elements_accessibility_view_tag_id");
        while (true) {
            if (tagKey != 0 && Boolean.TRUE.equals(view.getTag(tagKey))) {
                return true;
            }
            String className = view.getClass().getName();
            if (className.endsWith("BottomUiContainer")) {
                return true;
            }
            if (leadingAssetTagId != 0) {
                Object tag = view.getTag(leadingAssetTagId);
                if (tag != null && "eml.snackbar.leading_asset".equals(tag.toString())) {
                    return true;
                }
            }
            ViewParent parent = view.getParent();
            if (!(parent instanceof View)) {
                break;
            }
            view = (View) parent;
        }
        return false;
    }

    private static View getViewFromCallback(Drawable.Callback callback) {
        int depth = 0;
        while (callback != null && depth < 10) {
            if (callback instanceof View) {
                return (View) callback;
            }
            if (callback instanceof Drawable) {
                callback = ((Drawable) callback).getCallback();
            } else {
                break;
            }
            depth++;
        }
        return null;
    }

    private static int getBlackColor() {
        if (blackColor == 0) blackColor = ResourceUtils.getColor("revanced_snack_bar_color_dark");
        return blackColor;
    }

    private static int getWhiteColor() {
        if (whiteColor == 0) whiteColor = ResourceUtils.getColor("revanced_snack_bar_color_light");
        return whiteColor;
    }
}
