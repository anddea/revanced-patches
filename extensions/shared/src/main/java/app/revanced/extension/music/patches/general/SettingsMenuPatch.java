package app.revanced.extension.music.patches.general;

import androidx.preference.PreferenceScreen;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.BaseSettingsMenuPatch;

@SuppressWarnings("unused")
public final class SettingsMenuPatch extends BaseSettingsMenuPatch {

    public static void hideSettingsMenu(PreferenceScreen mPreferenceScreen) {
        if (mPreferenceScreen == null) return;
        for (SettingsMenuComponent component : SettingsMenuComponent.values())
            if (component.enabled)
                removePreference(mPreferenceScreen, component.key);
    }

    public static boolean hideParentToolsMenu(boolean original) {
        return !Settings.HIDE_SETTINGS_MENU_PARENT_TOOLS.get() && original;
    }

    private enum SettingsMenuComponent {
        GENERAL("settings_header_general", Settings.HIDE_SETTINGS_MENU_GENERAL.get()),
        PLAYBACK("settings_header_playback", Settings.HIDE_SETTINGS_MENU_PLAYBACK.get()),
        DATA_SAVING("settings_header_data_saving", Settings.HIDE_SETTINGS_MENU_DATA_SAVING.get()),
        DOWNLOADS_AND_STORAGE("settings_header_downloads_and_storage", Settings.HIDE_SETTINGS_MENU_DOWNLOADS_AND_STORAGE.get()),
        NOTIFICATIONS("settings_header_notifications", Settings.HIDE_SETTINGS_MENU_NOTIFICATIONS.get()),
        PRIVACY_AND_LOCATION("settings_header_privacy_and_location", Settings.HIDE_SETTINGS_MENU_PRIVACY_AND_LOCATION.get()),
        RECOMMENDATIONS("settings_header_recommendations", Settings.HIDE_SETTINGS_MENU_RECOMMENDATIONS.get()),
        PAID_MEMBERSHIPS("settings_header_paid_memberships", Settings.HIDE_SETTINGS_MENU_PAID_MEMBERSHIPS.get()),
        ABOUT("settings_header_about_youtube_music", Settings.HIDE_SETTINGS_MENU_ABOUT.get());

        private final String key;
        private final boolean enabled;

        SettingsMenuComponent(String key, boolean enabled) {
            this.key = key;
            this.enabled = enabled;
        }
    }
}
