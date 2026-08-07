package app.morphe.extension.music.patches.navigation;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.EnumMap;
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

    @SuppressLint("ClickableViewAccessibility")
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
                        View.OnTouchListener touchListener = (v, event) -> {
                            int action = event.getAction();
                            if (action == android.view.MotionEvent.ACTION_DOWN) {
                                v.setPressed(true);
                            } else if (action == android.view.MotionEvent.ACTION_UP) {
                                v.setPressed(false);
                                float x = event.getX();
                                float y = event.getY();
                                if (x >= 0 && x <= v.getWidth() && y >= 0 && y <= v.getHeight()) {
                                    onClickAction.run();
                                }
                            } else if (action == android.view.MotionEvent.ACTION_CANCEL) {
                                v.setPressed(false);
                            }
                            return true;
                        };
                        view.setOnTouchListener(touchListener);
                        Utils.runOnMainThreadDelayed(() -> view.setOnTouchListener(touchListener), 500);
                    }
                }
                hideViewUnderCondition(button.hidden, view);
            }
        }
    }

    public static String replaceBrowseId(Object component, String browseId, String fieldName) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onThemeClassInit(Object themeClass) {
        try {
            Class<?> clazz = themeClass.getClass();
            Field bField = clazz.getDeclaredField("b");
            bField.setAccessible(true);
            EnumMap bMap = (EnumMap) bField.get(themeClass);

            Field cField = clazz.getDeclaredField("c");
            cField.setAccessible(true);
            EnumMap cMap = (EnumMap) cField.get(themeClass);

            Field eField = clazz.getDeclaredField("e");
            eField.setAccessible(true);
            EnumMap eMap = (EnumMap) eField.get(themeClass);

            Field fField = clazz.getDeclaredField("f");
            fField.setAccessible(true);
            EnumMap fMap = (EnumMap) fField.get(themeClass);

            Enum<?> samplesEnum = null;
            Enum<?> searchEnum = null;
            Enum<?> upgradeEnum = null;
            Enum<?> settingsEnum = null;

            if (bMap != null) {
                for (Object key : bMap.keySet()) {
                    Enum<?> e = (Enum<?>) key;
                    String name = e.name();
                    switch (name) {
                        case "TAB_SAMPLES":
                            samplesEnum = e;
                            break;
                        case "SEARCH":
                            searchEnum = e;
                            break;
                        case "TAB_MUSIC_PREMIUM":
                            upgradeEnum = e;
                            break;
                        case "SETTINGS":
                            settingsEnum = e;
                            break;
                    }
                }
            }

            if (Settings.REPLACE_NAVIGATION_SAMPLES_BUTTON.get() && samplesEnum != null && searchEnum != null) {
                if (bMap.containsKey(searchEnum)) bMap.put(samplesEnum, bMap.get(searchEnum));
                if (cMap != null && cMap.containsKey(searchEnum)) cMap.put(samplesEnum, cMap.get(searchEnum));
                if (eMap != null && eMap.containsKey(searchEnum)) eMap.put(samplesEnum, eMap.get(searchEnum));
                if (fMap != null && fMap.containsKey(searchEnum)) fMap.put(samplesEnum, fMap.get(searchEnum));
            }

            if (Settings.REPLACE_NAVIGATION_UPGRADE_BUTTON.get() && upgradeEnum != null && settingsEnum != null) {
                if (bMap.containsKey(settingsEnum)) bMap.put(upgradeEnum, bMap.get(settingsEnum));
                if (cMap != null && cMap.containsKey(settingsEnum)) cMap.put(upgradeEnum, cMap.get(settingsEnum));
                if (eMap != null && eMap.containsKey(settingsEnum)) eMap.put(upgradeEnum, eMap.get(settingsEnum));
                if (fMap != null && fMap.containsKey(settingsEnum)) fMap.put(upgradeEnum, fMap.get(settingsEnum));
            }
        } catch (Exception e) {
            Logger.printException(() -> "onThemeClassInit failed", e);
        }
    }

    private enum NavigationButton {
        HOME(
                List.of("TAB_HOME"),
                Settings.HIDE_NAVIGATION_HOME_BUTTON.get(),
                "FEmusic_home"
        ),
        SAMPLES(
                List.of("TAB_SAMPLES"),
                Settings.HIDE_NAVIGATION_SAMPLES_BUTTON.get(),
                Settings.REPLACE_NAVIGATION_SAMPLES_BUTTON.get(),
                "FEmusic_immersive",
                "search",
                "yt_fill_samples_vd_theme_24",
                "yt_outline_samples_vd_theme_24",
                "yt_outline_search_vd_theme_24",
                ExtendedUtils::openSearch
        ),
        EXPLORE(
                List.of("TAB_EXPLORE"),
                Settings.HIDE_NAVIGATION_EXPLORE_BUTTON.get(),
                "FEmusic_explore"
        ),
        LIBRARY(
                List.of(
                        "LIBRARY_MUSIC",
                        "TAB_BOOKMARK" // YouTube Music 8.24+
                ),
                Settings.HIDE_NAVIGATION_LIBRARY_BUTTON.get(),
                "FEmusic_library_landing"
        ),
        UPGRADE(
                List.of("TAB_MUSIC_PREMIUM"),
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
