package app.revanced.extension.youtube.patches.general;

import androidx.preference.PreferenceScreen;

import app.revanced.extension.shared.patches.BaseSettingsMenuPatch;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class SettingsMenuPatch extends BaseSettingsMenuPatch {

    public static void hideSettingsMenu(PreferenceScreen mPreferenceScreen) {
        if (mPreferenceScreen == null) return;
        for (SettingsMenuComponent component : SettingsMenuComponent.values())
            if (component.enabled)
                removePreference(mPreferenceScreen, component.key);
    }

    private enum SettingsMenuComponent {
        YOUTUBE_TV("yt_unplugged_pref_key", Settings.HIDE_SETTINGS_MENU_YOUTUBE_TV.get()),
        PARENT_TOOLS("parent_tools_key", Settings.HIDE_SETTINGS_MENU_PARENT_TOOLS.get()),
        PRE_PURCHASE("yt_unlimited_pre_purchase_key", Settings.HIDE_SETTINGS_MENU_PRE_PURCHASE.get()),
        GENERAL("general_key", Settings.HIDE_SETTINGS_MENU_GENERAL.get()),
        ACCOUNT("account_switcher_key", Settings.HIDE_SETTINGS_MENU_ACCOUNT.get()),
        DATA_SAVING("data_saving_settings_key", Settings.HIDE_SETTINGS_MENU_DATA_SAVING.get()),
        AUTOPLAY("auto_play_key", Settings.HIDE_SETTINGS_MENU_AUTOPLAY.get()),
        VIDEO_QUALITY_PREFERENCES("video_quality_settings_key", Settings.HIDE_SETTINGS_MENU_VIDEO_QUALITY_PREFERENCES.get()),
        POST_PURCHASE("yt_unlimited_post_purchase_key", Settings.HIDE_SETTINGS_MENU_POST_PURCHASE.get()),
        OFFLINE("offline_key", Settings.HIDE_SETTINGS_MENU_OFFLINE.get()),
        WATCH_ON_TV("pair_with_tv_key", Settings.HIDE_SETTINGS_MENU_WATCH_ON_TV.get()),
        MANAGE_ALL_HISTORY("history_key", Settings.HIDE_SETTINGS_MENU_MANAGE_ALL_HISTORY.get()),
        YOUR_DATA_IN_YOUTUBE("your_data_key", Settings.HIDE_SETTINGS_MENU_YOUR_DATA_IN_YOUTUBE.get()),
        PRIVACY("privacy_key", Settings.HIDE_SETTINGS_MENU_PRIVACY.get()),
        TRY_EXPERIMENTAL_NEW_FEATURES("premium_early_access_browse_page_key", Settings.HIDE_SETTINGS_MENU_TRY_EXPERIMENTAL_NEW_FEATURES.get()),
        PURCHASES_AND_MEMBERSHIPS("subscription_product_setting_key", Settings.HIDE_SETTINGS_MENU_PURCHASES_AND_MEMBERSHIPS.get()),
        BILLING_AND_PAYMENTS("billing_and_payment_key", Settings.HIDE_SETTINGS_MENU_BILLING_AND_PAYMENTS.get()),
        NOTIFICATIONS("notification_key", Settings.HIDE_SETTINGS_MENU_NOTIFICATIONS.get()),
        THIRD_PARTY("third_party_key", Settings.HIDE_SETTINGS_MENU_THIRD_PARTY.get()),
        CONNECTED_APPS("connected_accounts_browse_page_key", Settings.HIDE_SETTINGS_MENU_CONNECTED_APPS.get()),
        LIVE_CHAT("live_chat_key", Settings.HIDE_SETTINGS_MENU_LIVE_CHAT.get()),
        CAPTIONS("captions_key", Settings.HIDE_SETTINGS_MENU_CAPTIONS.get()),
        ACCESSIBILITY("accessibility_settings_key", Settings.HIDE_SETTINGS_MENU_ACCESSIBILITY.get()),
        ABOUT("about_key", Settings.HIDE_SETTINGS_MENU_ABOUT.get());

        private final String key;
        private final boolean enabled;

        SettingsMenuComponent(String key, boolean enabled) {
            this.key = key;
            this.enabled = enabled;
        }
    }
}
