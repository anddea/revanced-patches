package app.morphe.extension.music.patches.navigation;

import static app.morphe.extension.music.utils.ExtendedUtils.IS_6_27_OR_GREATER;
import static app.morphe.extension.music.utils.ExtendedUtils.IS_8_29_OR_GREATER;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;

import android.graphics.Color;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import app.morphe.extension.music.patches.utils.PatchStatus;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.utils.ExtendedUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class NavigationPatch {
    private static final int colorGrey12 = PatchStatus.DarkTheme()
            ? ResourceUtils.getColor("ytm_color_grey_12")
            : ResourceUtils.getColor("revanced_color_grey_12");

    @NonNull
    private static String lastYTNavigationEnumName = "";

    public static void setLastAppNavigationEnum(@Nullable Enum<?> ytNavigationEnumName) {
        if (ytNavigationEnumName != null) {
            lastYTNavigationEnumName = ytNavigationEnumName.name();
        }
    }

    public static int enableCustomNavigationBarColor() {
        try {
            if (Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR.get()) {
                return Color.parseColor(Settings.ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE.get());
            }
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_custom_navigation_bar_color_value_invalid_invalid_toast"));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
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

        for (NavigationButton button : NavigationButton.values()) {
            if (button.ytEnumNames.contains(lastYTNavigationEnumName)) {
                if (button.replace) {
                    Runnable onClickAction = button.onClickAction;
                    if (onClickAction != null) {
                        view.setOnClickListener(v -> onClickAction.run());
                        Utils.runOnMainThreadDelayed(() -> view.setOnClickListener(v -> onClickAction.run()), 500);
                    }
                }
                hideViewUnderCondition(button.hidden, view);
            }
        }
    }

    public static String replaceBrowseId(Object component, String browseId, String fieldName) {
        for (NavigationButton button : NavigationButton.values()) {
            if (button.replace && button.browseId.equals(browseId)) {
                String replaceBrowseId = Settings.CHANGE_START_PAGE.get().getBrowseId();
                if (replaceBrowseId.isEmpty()) {
                    replaceBrowseId = NavigationButton.HOME.browseId;
                }
                try {
                    Field browseIdField = component.getClass().getField(fieldName);
                    browseIdField.setAccessible(true);
                    browseIdField.set(component, replaceBrowseId);
                    return replaceBrowseId;
                } catch (Exception ignored) {
                    Logger.printException(() -> "replaceBrowseId failed");
                }
            }
        }

        return browseId;
    }

    public static int replaceNavigationIcon(int drawableId) {
        for (NavigationButton button : NavigationButton.values()) {
            if (button.replace &&
                    (drawableId == button.unSelectedDrawableId || drawableId == button.selectedDrawableId)) {
                int replaceDrawableId = button.replaceDrawableId;
                if (replaceDrawableId != 0) {
                    return replaceDrawableId;
                }
            }
        }

        return drawableId;
    }

    public static Spanned replaceNavigationLabel(@NonNull Spanned sourceStyle) {
        for (NavigationButton button : NavigationButton.values()) {
            if (button.ytEnumNames.contains(lastYTNavigationEnumName) && button.replace) {
                String label = button.label;
                if (!label.isEmpty()) {
                    return Utils.newSpanUsingStylingOfAnotherSpan(sourceStyle, label);
                }
            }
        }

        return sourceStyle;
    }

    private enum NavigationButton {
        HOME(
                Arrays.asList("TAB_HOME"),
                Settings.HIDE_NAVIGATION_HOME_BUTTON.get(),
                "FEmusic_home"
        ),
        SAMPLES(
                Arrays.asList("TAB_SAMPLES"),
                Settings.HIDE_NAVIGATION_SAMPLES_BUTTON.get(),
                IS_6_27_OR_GREATER && !IS_8_29_OR_GREATER &&
                        Settings.REPLACE_NAVIGATION_SAMPLES_BUTTON.get(),
                "FEmusic_immersive",
                "search",
                "yt_fill_samples_vd_theme_24",
                "yt_outline_samples_vd_theme_24",
                "yt_outline_search_vd_theme_24",
                ExtendedUtils::openSearch
        ),
        EXPLORE(
                Arrays.asList("TAB_EXPLORE"),
                Settings.HIDE_NAVIGATION_EXPLORE_BUTTON.get(),
                "FEmusic_explore"
        ),
        LIBRARY(
                Arrays.asList(
                        "LIBRARY_MUSIC",
                        "TAB_BOOKMARK" // YouTube Music 8.24+
                ),
                Settings.HIDE_NAVIGATION_LIBRARY_BUTTON.get(),
                "FEmusic_library_landing"
        ),
        UPGRADE(
                Arrays.asList("TAB_MUSIC_PREMIUM"),
                Settings.HIDE_NAVIGATION_UPGRADE_BUTTON.get(),
                Settings.REPLACE_NAVIGATION_UPGRADE_BUTTON.get(),
                "SPunlimited",
                "settings",
                "yt_fill_youtube_music_vd_theme_24",
                "yt_outline_youtube_music_vd_theme_24",
                "yt_outline_gear_vd_theme_24",
                ExtendedUtils::openSetting
        );

        private final List<String> ytEnumNames;
        private final boolean hidden;
        private final boolean replace;
        @NonNull
        private final String browseId;
        @NonNull
        private final String label;
        private final int selectedDrawableId;
        private final int unSelectedDrawableId;
        private final int replaceDrawableId;
        @Nullable
        private final Runnable onClickAction;

        NavigationButton(@NonNull List<String> ytEnumNames, boolean hidden,
                         @NonNull String browseId) {
            this(ytEnumNames, hidden, false, browseId, null, null, null, null, null);
        }

        NavigationButton(@NonNull List<String> ytEnumNames, boolean hidden, boolean replace,
                         @NonNull String browseId, @Nullable String label,
                         @Nullable String selectedIcon, @Nullable String unSelectedIcon,
                         @Nullable String replaceIcon, @Nullable Runnable onClickAction) {
            this.ytEnumNames = ytEnumNames;
            this.hidden = hidden;
            this.replace = replace;
            this.browseId = browseId;
            this.label = label != null ? ResourceUtils.getString(label) : "";
            this.selectedDrawableId = selectedIcon != null ? ResourceUtils.getDrawableIdentifier(selectedIcon) : 0;
            this.unSelectedDrawableId = unSelectedIcon != null ? ResourceUtils.getDrawableIdentifier(unSelectedIcon) : 0;
            this.replaceDrawableId = replaceIcon != null ? ResourceUtils.getDrawableIdentifier(replaceIcon) : 0;
            this.onClickAction = onClickAction;
        }
    }
}
