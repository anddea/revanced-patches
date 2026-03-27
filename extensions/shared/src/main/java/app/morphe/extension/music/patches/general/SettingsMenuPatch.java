package app.morphe.extension.music.patches.general;

import androidx.preference.PreferenceScreen;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.BaseSettingsMenuPatch;
import app.morphe.extension.shared.settings.BooleanSetting;

@SuppressWarnings("unused")
public final class SettingsMenuPatch extends BaseSettingsMenuPatch {
    private static final BooleanSetting HIDE_SETTINGS_MENU_PARENT_TOOLS =
            Settings.HIDE_SETTINGS_MENU_PARENT_TOOLS;

    public static void hideSettingsMenu(PreferenceScreen mPreferenceScreen) {
        if (mPreferenceScreen != null) {
            for (SettingsMenuComponent component : SettingsMenuComponent.values())
                if (component.setting.get())
                    removePreference(mPreferenceScreen, component.key);
        }
    }

    public static boolean hideParentToolsMenu(boolean original) {
        return !HIDE_SETTINGS_MENU_PARENT_TOOLS.get() && original;
    }

    private enum SettingsMenuComponent {
        GENERAL("settings_header_general", Settings.HIDE_SETTINGS_MENU_GENERAL),
        PLAYBACK("settings_header_playback", Settings.HIDE_SETTINGS_MENU_PLAYBACK),
        DATA_SAVING("settings_header_data_saving", Settings.HIDE_SETTINGS_MENU_DATA_SAVING),
        DOWNLOADS_AND_STORAGE("settings_header_downloads_and_storage", Settings.HIDE_SETTINGS_MENU_DOWNLOADS_AND_STORAGE),
        NOTIFICATIONS("settings_header_notifications", Settings.HIDE_SETTINGS_MENU_NOTIFICATIONS),
        PRIVACY_AND_LOCATION("settings_header_privacy_and_location", Settings.HIDE_SETTINGS_MENU_PRIVACY_AND_LOCATION),
        RECOMMENDATIONS("settings_header_recommendations", Settings.HIDE_SETTINGS_MENU_RECOMMENDATIONS),
        PAID_MEMBERSHIPS("settings_header_paid_memberships", Settings.HIDE_SETTINGS_MENU_PAID_MEMBERSHIPS),
        ABOUT("settings_header_about_youtube_music", Settings.HIDE_SETTINGS_MENU_ABOUT);

        private final String key;
        private final BooleanSetting setting;

        SettingsMenuComponent(String key, BooleanSetting setting) {
            this.key = key;
            this.setting = setting;
        }
    }
}
