package app.revanced.extension.music.patches.navigation;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import app.revanced.extension.music.patches.utils.PatchStatus;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class NavigationPatch {
    private static final int colorGrey12 = PatchStatus.DarkTheme()
            ? ResourceUtils.getColor("ytm_color_grey_12")
            : ResourceUtils.getColor("revanced_color_grey_12");

    public static Enum<?> lastPivotTab;

    public static int enableCustomNavigationBarColor() {
        try {
            if (Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR.get()) {
                return Color.parseColor(Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE.get());
            }
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_custom_navigation_bar_color_value_invalid_invalid_toast"));
            Utils.showToastShort(str("revanced_extended_reset_to_default_toast"));
            Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE.resetToDefault();
        }

        return colorGrey12;
    }

    public static void hideNavigationLabel(TextView textview) {
        hideViewUnderCondition(Settings.HIDE_NAVIGATION_LABEL.get(), textview);
    }

    public static void hideNavigationButton(@NonNull View view) {
        if (Settings.HIDE_NAVIGATION_BAR.get() && view.getParent() != null) {
            hideViewUnderCondition(true, (View) view.getParent());
            return;
        }

        for (NavigationButton button : NavigationButton.values())
            if (lastPivotTab.name().equals(button.name))
                hideViewUnderCondition(button.enabled, view);
    }

    private enum NavigationButton {
        HOME("TAB_HOME", Settings.HIDE_NAVIGATION_HOME_BUTTON.get()),
        SAMPLES("TAB_SAMPLES", Settings.HIDE_NAVIGATION_SAMPLES_BUTTON.get()),
        EXPLORE("TAB_EXPLORE", Settings.HIDE_NAVIGATION_EXPLORE_BUTTON.get()),
        LIBRARY("LIBRARY_MUSIC", Settings.HIDE_NAVIGATION_LIBRARY_BUTTON.get()),
        UPGRADE("TAB_MUSIC_PREMIUM", Settings.HIDE_NAVIGATION_UPGRADE_BUTTON.get());

        private final boolean enabled;
        private final String name;

        NavigationButton(String name, boolean enabled) {
            this.enabled = enabled;
            this.name = name;
        }
    }
}
