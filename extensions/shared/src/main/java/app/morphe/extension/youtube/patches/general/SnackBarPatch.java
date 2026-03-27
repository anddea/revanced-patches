package app.morphe.extension.youtube.patches.general;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final AtomicBoolean lithoSnackBarLoaded = new AtomicBoolean(false);
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

    public static void setLithoSnackBarBackgroundColor(FrameLayout frameLayout, int color) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND) {
            return;
        }
        frameLayout.setBackgroundColor(color);
    }

    public static Context invertSnackBarTheme(Context mContext) {
        if (INVERT_SERVER_SIDE_SNACK_BAR_THEME) {
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

    public static void lithoSnackBarLoaded() {
        lithoSnackBarLoaded.compareAndSet(false, true);
    }

    public static int getLithoColor(int originalValue) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND &&
                lithoSnackBarLoaded.compareAndSet(true, false)) {
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

    private static int getBlackColor() {
        if (blackColor == 0) blackColor = ResourceUtils.getColor("revanced_snack_bar_color_dark");
        return blackColor;
    }

    private static int getWhiteColor() {
        if (whiteColor == 0) whiteColor = ResourceUtils.getColor("revanced_snack_bar_color_light");
        return whiteColor;
    }
}
