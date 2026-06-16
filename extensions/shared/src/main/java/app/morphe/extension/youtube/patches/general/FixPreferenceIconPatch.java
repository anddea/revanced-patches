/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 *
 * Copyright (C) 2026 anddea (https://github.com/anddea)
 */

package app.morphe.extension.youtube.patches.general;

import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.settings.Settings;

import java.util.HashMap;
import java.util.Map;

import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

@SuppressWarnings("unused")
public class FixPreferenceIconPatch {
    private static final boolean REMOVE_BROKEN_PREFERENCE_ICON =
            Settings.RESTORE_OLD_SETTINGS_MENUS.get() || isSpoofingToLessThan("19.35.36");

    private static final String[][] PREFERENCE_ICONS = {
            {"parent_tools_key", "parent_tools_key_icon"},
            {"general_key", "general_key_icon"},
            {"account_switcher_key", "account_switcher_key_icon"},
            {"time_management_key", "time_management_key_icon"},
            {"data_saving_settings_key", "data_saving_settings_key_icon"},
            {"auto_play_key", "auto_play_key_icon"},
            {"playback_key", "auto_play_key_icon"},
            {"video_quality_settings_key", "video_quality_settings_key_icon"},
            {"offline_key", "offline_key_icon"},
            {"pair_with_tv_key", "pair_with_tv_key_icon"},
            {"history_key", "history_key_icon"},
            {"your_data_key", "your_data_key_icon"},
            {"privacy_key", "privacy_key_icon"},
            {"premium_early_access_browse_page_key", "premium_early_access_browse_page_key_icon"},
            {"subscription_product_setting_key", "subscription_product_setting_key_icon"},
            {"billing_and_payment_key", "billing_and_payment_key_icon"},
            {"notification_key", "notification_key_icon"},
            {"connected_accounts_browse_page_key", "connected_accounts_browse_page_key_icon"},
            {"live_chat_key", "live_chat_key_icon"},
            {"captions_key", "captions_key_icon"},
            {"accessibility_settings_key", "accessibility_settings_key_icon"},
            {"about_key", "about_key_icon"},
    };

    private static Map<String, String> preferenceIconMap;

    /**
     * Injection point.
     */
    public static boolean removePreferenceIcon() {
        return REMOVE_BROKEN_PREFERENCE_ICON;
    }

    private static Map<String, String> getPreferenceIconMap() {
        if (preferenceIconMap != null) {
            return preferenceIconMap;
        }

        Map<String, String> map = new HashMap<>();
        for (String[] entry : PREFERENCE_ICONS) {
            map.put(entry[0], entry[1]);

            int stringIdentifier = ResourceUtils.getStringIdentifier(entry[0]);
            if (stringIdentifier != 0) {
                map.put(ResourceUtils.getResources().getString(stringIdentifier), entry[1]);
            }
        }

        preferenceIconMap = map;
        return preferenceIconMap;
    }

    /**
     * Injection point.
     */
    public static int getPreferenceIconResourceIdentifier(String preferenceKey) {
        if (!removePreferenceIcon() || preferenceKey == null) {
            return 0;
        }

        String drawableName = getPreferenceIconMap().get(preferenceKey);
        if (drawableName == null) {
            return 0;
        }

        return ResourceUtils.getDrawableIdentifier(drawableName);
    }
}
