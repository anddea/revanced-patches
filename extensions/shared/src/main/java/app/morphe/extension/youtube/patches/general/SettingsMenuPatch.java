package app.morphe.extension.youtube.patches.general;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import app.morphe.extension.shared.patches.BaseSettingsMenuPatch;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class SettingsMenuPatch extends BaseSettingsMenuPatch {

    public static void hideSettingsMenu(PreferenceScreen mPreferenceScreen) {
        if (mPreferenceScreen != null) {
            for (SettingsMenuComponent component : SettingsMenuComponent.values())
                if (component.setting.get())
                    removePreference(mPreferenceScreen, component.key);
        }
    }

    @Nullable
    public static Preference hideWatchOnTVMenu(Preference mPreference) {
        return Settings.HIDE_SETTINGS_MENU_WATCH_ON_TV.get()
                ? null
                : mPreference;
    }

    private enum SettingsMenuComponent {
        YOUTUBE_TV("yt_unplugged_pref_key", Settings.HIDE_SETTINGS_MENU_YOUTUBE_TV),
        PARENT_TOOLS("parent_tools_key", Settings.HIDE_SETTINGS_MENU_PARENT_TOOLS),
        PRE_PURCHASE("yt_unlimited_pre_purchase_key", Settings.HIDE_SETTINGS_MENU_PRE_PURCHASE),
        GENERAL("general_key", Settings.HIDE_SETTINGS_MENU_GENERAL),
        ACCOUNT("account_switcher_key", Settings.HIDE_SETTINGS_MENU_ACCOUNT),
        DATA_SAVING("data_saving_settings_key", Settings.HIDE_SETTINGS_MENU_DATA_SAVING),
        AUTOPLAY("auto_play_key", Settings.HIDE_SETTINGS_MENU_AUTOPLAY_PLAYBACK),
        PLAYBACK("playback_key", Settings.HIDE_SETTINGS_MENU_AUTOPLAY_PLAYBACK),
        VIDEO_QUALITY_PREFERENCES("video_quality_settings_key", Settings.HIDE_SETTINGS_MENU_VIDEO_QUALITY_PREFERENCES),
        POST_PURCHASE("yt_unlimited_post_purchase_key", Settings.HIDE_SETTINGS_MENU_POST_PURCHASE),
        OFFLINE("offline_key", Settings.HIDE_SETTINGS_MENU_OFFLINE),
        WATCH_ON_TV("pair_with_tv_key", Settings.HIDE_SETTINGS_MENU_WATCH_ON_TV),
        MANAGE_ALL_HISTORY("history_key", Settings.HIDE_SETTINGS_MENU_MANAGE_ALL_HISTORY),
        YOUR_DATA_IN_YOUTUBE("your_data_key", Settings.HIDE_SETTINGS_MENU_YOUR_DATA_IN_YOUTUBE),
        PRIVACY("privacy_key", Settings.HIDE_SETTINGS_MENU_PRIVACY),
        TRY_EXPERIMENTAL_NEW_FEATURES("premium_early_access_browse_page_key", Settings.HIDE_SETTINGS_MENU_TRY_EXPERIMENTAL_NEW_FEATURES),
        PURCHASES_AND_MEMBERSHIPS("subscription_product_setting_key", Settings.HIDE_SETTINGS_MENU_PURCHASES_AND_MEMBERSHIPS),
        BILLING_AND_PAYMENTS("billing_and_payment_key", Settings.HIDE_SETTINGS_MENU_BILLING_AND_PAYMENTS),
        NOTIFICATIONS("notification_key", Settings.HIDE_SETTINGS_MENU_NOTIFICATIONS),
        THIRD_PARTY("third_party_key", Settings.HIDE_SETTINGS_MENU_THIRD_PARTY),
        CONNECTED_APPS("connected_accounts_browse_page_key", Settings.HIDE_SETTINGS_MENU_CONNECTED_APPS),
        LIVE_CHAT("live_chat_key", Settings.HIDE_SETTINGS_MENU_LIVE_CHAT),
        CAPTIONS("captions_key", Settings.HIDE_SETTINGS_MENU_CAPTIONS),
        ACCESSIBILITY("accessibility_settings_key", Settings.HIDE_SETTINGS_MENU_ACCESSIBILITY),
        ABOUT("about_key", Settings.HIDE_SETTINGS_MENU_ABOUT);

        private final String key;
        private final BooleanSetting setting;

        SettingsMenuComponent(String key, BooleanSetting setting) {
            this.key = key;
            this.setting = setting;
        }
    }
}
