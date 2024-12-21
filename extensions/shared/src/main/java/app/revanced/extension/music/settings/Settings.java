package app.revanced.extension.music.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.revanced.extension.music.sponsorblock.objects.CategoryBehaviour.SKIP_AUTOMATICALLY;

import androidx.annotation.NonNull;

import app.revanced.extension.music.patches.utils.PatchStatus;
import app.revanced.extension.music.sponsorblock.SponsorBlockSettings;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.settings.LongSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Utils;


@SuppressWarnings("unused")
public class Settings extends BaseSettings {
    // PreferenceScreen: Account
    public static final BooleanSetting HIDE_ACCOUNT_MENU = new BooleanSetting("revanced_hide_account_menu", FALSE);
    public static final StringSetting HIDE_ACCOUNT_MENU_FILTER_STRINGS = new StringSetting("revanced_hide_account_menu_filter_strings", "");
    public static final BooleanSetting HIDE_ACCOUNT_MENU_EMPTY_COMPONENT = new BooleanSetting("revanced_hide_account_menu_empty_component", FALSE);
    public static final BooleanSetting HIDE_HANDLE = new BooleanSetting("revanced_hide_handle", TRUE, true);
    public static final BooleanSetting HIDE_TERMS_CONTAINER = new BooleanSetting("revanced_hide_terms_container", FALSE);


    // PreferenceScreen: Action Bar
    public static final BooleanSetting HIDE_ACTION_BUTTON_LIKE_DISLIKE = new BooleanSetting("revanced_hide_action_button_like_dislike", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_COMMENT = new BooleanSetting("revanced_hide_action_button_comment", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST = new BooleanSetting("revanced_hide_action_button_add_to_playlist", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_DOWNLOAD = new BooleanSetting("revanced_hide_action_button_download", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_SHARE = new BooleanSetting("revanced_hide_action_button_share", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_RADIO = new BooleanSetting("revanced_hide_action_button_radio", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_LABEL = new BooleanSetting("revanced_hide_action_button_label", FALSE, true);
    public static final BooleanSetting EXTERNAL_DOWNLOADER_ACTION_BUTTON = new BooleanSetting("revanced_external_downloader_action", FALSE, true);
    public static final StringSetting EXTERNAL_DOWNLOADER_PACKAGE_NAME = new StringSetting("revanced_external_downloader_package_name", "com.deniscerri.ytdl");


    // PreferenceScreen: Ads
    public static final BooleanSetting HIDE_GENERAL_ADS = new BooleanSetting("revanced_hide_general_ads", TRUE, true);
    public static final BooleanSetting HIDE_MUSIC_ADS = new BooleanSetting("revanced_hide_music_ads", TRUE, true);
    public static final BooleanSetting HIDE_PAID_PROMOTION_LABEL = new BooleanSetting("revanced_hide_paid_promotion_label", TRUE, true);
    public static final BooleanSetting HIDE_PREMIUM_PROMOTION = new BooleanSetting("revanced_hide_premium_promotion", TRUE, true);
    public static final BooleanSetting HIDE_PREMIUM_RENEWAL = new BooleanSetting("revanced_hide_premium_renewal", TRUE, true);


    // PreferenceScreen: Flyout menu
    public static final BooleanSetting ENABLE_COMPACT_DIALOG = new BooleanSetting("revanced_enable_compact_dialog", FALSE);
    public static final BooleanSetting ENABLE_TRIM_SILENCE = new BooleanSetting("revanced_enable_trim_silence", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_LIKE_DISLIKE = new BooleanSetting("revanced_hide_flyout_menu_like_dislike", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT = new BooleanSetting("revanced_hide_flyout_menu_3_column_component", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_ADD_TO_QUEUE = new BooleanSetting("revanced_hide_flyout_menu_add_to_queue", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_CAPTIONS = new BooleanSetting("revanced_hide_flyout_menu_captions", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DELETE_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_delete_playlist", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DISMISS_QUEUE = new BooleanSetting("revanced_hide_flyout_menu_dismiss_queue", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DOWNLOAD = new BooleanSetting("revanced_hide_flyout_menu_download", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_EDIT_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_edit_playlist", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_ALBUM = new BooleanSetting("revanced_hide_flyout_menu_go_to_album", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_ARTIST = new BooleanSetting("revanced_hide_flyout_menu_go_to_artist", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_EPISODE = new BooleanSetting("revanced_hide_flyout_menu_go_to_episode", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_PODCAST = new BooleanSetting("revanced_hide_flyout_menu_go_to_podcast", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_HELP = new BooleanSetting("revanced_hide_flyout_menu_help", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_PIN_TO_SPEED_DIAL = new BooleanSetting("revanced_hide_flyout_menu_pin_to_speed_dial", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_PLAY_NEXT = new BooleanSetting("revanced_hide_flyout_menu_play_next", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_QUALITY = new BooleanSetting("revanced_hide_flyout_menu_quality", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY = new BooleanSetting("revanced_hide_flyout_menu_remove_from_library", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REMOVE_FROM_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_remove_from_playlist", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REPORT = new BooleanSetting("revanced_hide_flyout_menu_report", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER = new BooleanSetting("revanced_hide_flyout_menu_save_episode_for_later", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SAVE_TO_LIBRARY = new BooleanSetting("revanced_hide_flyout_menu_save_to_library", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SAVE_TO_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_save_to_playlist", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SHARE = new BooleanSetting("revanced_hide_flyout_menu_share", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SHUFFLE_PLAY = new BooleanSetting("revanced_hide_flyout_menu_shuffle_play", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SLEEP_TIMER = new BooleanSetting("revanced_hide_flyout_menu_sleep_timer", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_START_RADIO = new BooleanSetting("revanced_hide_flyout_menu_start_radio", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_STATS_FOR_NERDS = new BooleanSetting("revanced_hide_flyout_menu_stats_for_nerds", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SUBSCRIBE = new BooleanSetting("revanced_hide_flyout_menu_subscribe", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_UNPIN_FROM_SPEED_DIAL = new BooleanSetting("revanced_hide_flyout_menu_unpin_from_speed_dial", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_VIEW_SONG_CREDIT = new BooleanSetting("revanced_hide_flyout_menu_view_song_credit", FALSE, true);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE = new BooleanSetting("revanced_replace_flyout_menu_dismiss_queue", FALSE, true);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE_CONTINUE_WATCH = new BooleanSetting("revanced_replace_flyout_menu_dismiss_queue_continue_watch", TRUE);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT = new BooleanSetting("revanced_replace_flyout_menu_report", TRUE, true);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER = new BooleanSetting("revanced_replace_flyout_menu_report_only_player", TRUE, true);


    // PreferenceScreen: General
    public static final StringSetting CHANGE_START_PAGE = new StringSetting("revanced_change_start_page", "FEmusic_home", true);
    public static final BooleanSetting DISABLE_DISLIKE_REDIRECTION = new BooleanSetting("revanced_disable_dislike_redirection", FALSE);
    public static final BooleanSetting ENABLE_LANDSCAPE_MODE = new BooleanSetting("revanced_enable_landscape_mode", FALSE, true);
    public static final BooleanSetting CUSTOM_FILTER = new BooleanSetting("revanced_custom_filter", FALSE);
    public static final StringSetting CUSTOM_FILTER_STRINGS = new StringSetting("revanced_custom_filter_strings", "", true);
    public static final BooleanSetting HIDE_BUTTON_SHELF = new BooleanSetting("revanced_hide_button_shelf", FALSE, true);
    public static final BooleanSetting HIDE_CAROUSEL_SHELF = new BooleanSetting("revanced_hide_carousel_shelf", FALSE, true);
    public static final BooleanSetting HIDE_PLAYLIST_CARD_SHELF = new BooleanSetting("revanced_hide_playlist_card_shelf", FALSE, true);
    public static final BooleanSetting HIDE_SAMPLE_SHELF = new BooleanSetting("revanced_hide_samples_shelf", FALSE, true);
    public static final BooleanSetting HIDE_CAST_BUTTON = new BooleanSetting("revanced_hide_cast_button", TRUE);
    public static final BooleanSetting HIDE_CATEGORY_BAR = new BooleanSetting("revanced_hide_category_bar", FALSE, true);
    public static final BooleanSetting HIDE_FLOATING_BUTTON = new BooleanSetting("revanced_hide_floating_button", FALSE, true);
    public static final BooleanSetting HIDE_TAP_TO_UPDATE_BUTTON = new BooleanSetting("revanced_hide_tap_to_update_button", FALSE, true);
    public static final BooleanSetting HIDE_HISTORY_BUTTON = new BooleanSetting("revanced_hide_history_button", FALSE);
    public static final BooleanSetting HIDE_NOTIFICATION_BUTTON = new BooleanSetting("revanced_hide_notification_button", FALSE, true);
    public static final BooleanSetting HIDE_SOUND_SEARCH_BUTTON = new BooleanSetting("revanced_hide_sound_search_button", FALSE, true);
    public static final BooleanSetting HIDE_VOICE_SEARCH_BUTTON = new BooleanSetting("revanced_hide_voice_search_button", FALSE, true);
    public static final BooleanSetting REMOVE_VIEWER_DISCRETION_DIALOG = new BooleanSetting("revanced_remove_viewer_discretion_dialog", FALSE);
    public static final BooleanSetting RESTORE_OLD_STYLE_LIBRARY_SHELF = new BooleanSetting("revanced_restore_old_style_library_shelf", FALSE, true);
    public static final BooleanSetting SPOOF_APP_VERSION = new BooleanSetting("revanced_spoof_app_version",
            PatchStatus.SpoofAppVersionDefaultBoolean(), true);
    public static final StringSetting SPOOF_APP_VERSION_TARGET = new StringSetting("revanced_spoof_app_version_target",
            PatchStatus.SpoofAppVersionDefaultString(), true);


    // PreferenceScreen: Navigation bar
    public static final BooleanSetting ENABLE_BLACK_NAVIGATION_BAR = new BooleanSetting("revanced_enable_black_navigation_bar", TRUE);
    public static final BooleanSetting HIDE_NAVIGATION_HOME_BUTTON = new BooleanSetting("revanced_hide_navigation_home_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_SAMPLES_BUTTON = new BooleanSetting("revanced_hide_navigation_samples_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_EXPLORE_BUTTON = new BooleanSetting("revanced_hide_navigation_explore_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LIBRARY_BUTTON = new BooleanSetting("revanced_hide_navigation_library_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_UPGRADE_BUTTON = new BooleanSetting("revanced_hide_navigation_upgrade_button", TRUE, true);
    public static final BooleanSetting HIDE_NAVIGATION_BAR = new BooleanSetting("revanced_hide_navigation_bar", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LABEL = new BooleanSetting("revanced_hide_navigation_label", FALSE, true);


    // PreferenceScreen: Player
    public static final BooleanSetting DISABLE_MINI_PLAYER_GESTURE = new BooleanSetting("revanced_disable_mini_player_gesture", FALSE, true);
    public static final BooleanSetting DISABLE_PLAYER_GESTURE = new BooleanSetting("revanced_disable_player_gesture", FALSE, true);
    public static final BooleanSetting ENABLE_BLACK_PLAYER_BACKGROUND = new BooleanSetting("revanced_enable_black_player_background", FALSE, true);
    public static final BooleanSetting ENABLE_COLOR_MATCH_PLAYER = new BooleanSetting("revanced_enable_color_match_player", TRUE);
    public static final BooleanSetting ENABLE_FORCE_MINIMIZED_PLAYER = new BooleanSetting("revanced_enable_force_minimized_player", TRUE);
    public static final BooleanSetting ENABLE_MINI_PLAYER_NEXT_BUTTON = new BooleanSetting("revanced_enable_mini_player_next_button", TRUE, true);
    public static final BooleanSetting ENABLE_MINI_PLAYER_PREVIOUS_BUTTON = new BooleanSetting("revanced_enable_mini_player_previous_button", TRUE, true);
    public static final BooleanSetting ENABLE_SWIPE_TO_DISMISS_MINI_PLAYER = new BooleanSetting("revanced_enable_swipe_to_dismiss_mini_player", TRUE, true);
    public static final BooleanSetting ENABLE_ZEN_MODE = new BooleanSetting("revanced_enable_zen_mode", FALSE);
    public static final BooleanSetting ENABLE_ZEN_MODE_PODCAST = new BooleanSetting("revanced_enable_zen_mode_podcast", FALSE);
    public static final BooleanSetting HIDE_AUDIO_VIDEO_SWITCH_TOGGLE = new BooleanSetting("revanced_hide_audio_video_switch_toggle", FALSE, true);
    public static final BooleanSetting HIDE_COMMENT_CHANNEL_GUIDELINES = new BooleanSetting("revanced_hide_comment_channel_guidelines", TRUE);
    public static final BooleanSetting HIDE_COMMENT_TIMESTAMP_AND_EMOJI_BUTTONS = new BooleanSetting("revanced_hide_comment_timestamp_and_emoji_buttons", FALSE);
    public static final BooleanSetting HIDE_DOUBLE_TAP_OVERLAY_FILTER = new BooleanSetting("revanced_hide_double_tap_overlay_filter", FALSE, true);
    public static final BooleanSetting HIDE_FULLSCREEN_SHARE_BUTTON = new BooleanSetting("revanced_hide_fullscreen_share_button", FALSE, true);
    public static final BooleanSetting REMEMBER_REPEAT_SATE = new BooleanSetting("revanced_remember_repeat_state", TRUE);
    public static final BooleanSetting REMEMBER_SHUFFLE_SATE = new BooleanSetting("revanced_remember_shuffle_state", TRUE);
    public static final BooleanSetting ALWAYS_SHUFFLE = new BooleanSetting("revanced_always_shuffle", FALSE);
    public static final BooleanSetting RESTORE_OLD_COMMENTS_POPUP_PANELS = new BooleanSetting("revanced_restore_old_comments_popup_panels", FALSE, true);
    public static final BooleanSetting RESTORE_OLD_PLAYER_BACKGROUND = new BooleanSetting("revanced_restore_old_player_background", FALSE, true);
    public static final BooleanSetting RESTORE_OLD_PLAYER_LAYOUT = new BooleanSetting("revanced_restore_old_player_layout", FALSE, true);


    // PreferenceScreen: Settings menu
    public static final BooleanSetting HIDE_SETTINGS_MENU_PARENT_TOOLS = new BooleanSetting("revanced_hide_settings_menu_parent_tools", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_GENERAL = new BooleanSetting("revanced_hide_settings_menu_general", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PLAYBACK = new BooleanSetting("revanced_hide_settings_menu_playback", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_DATA_SAVING = new BooleanSetting("revanced_hide_settings_menu_data_saving", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_DOWNLOADS_AND_STORAGE = new BooleanSetting("revanced_hide_settings_menu_downloads_and_storage", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_NOTIFICATIONS = new BooleanSetting("revanced_hide_settings_menu_notification", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PRIVACY_AND_LOCATION = new BooleanSetting("revanced_hide_settings_menu_privacy_and_location", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_RECOMMENDATIONS = new BooleanSetting("revanced_hide_settings_menu_recommendations", FALSE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PAID_MEMBERSHIPS = new BooleanSetting("revanced_hide_settings_menu_paid_memberships", TRUE, true);
    public static final BooleanSetting HIDE_SETTINGS_MENU_ABOUT = new BooleanSetting("revanced_hide_settings_menu_about", FALSE, true);


    // PreferenceScreen: Video
    public static final StringSetting CUSTOM_PLAYBACK_SPEEDS = new StringSetting("revanced_custom_playback_speeds", "0.5\n0.8\n1.0\n1.2\n1.5\n1.8\n2.0", true);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_LAST_SELECTED = new BooleanSetting("revanced_remember_playback_speed_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_playback_speed_last_selected_toast", TRUE);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_LAST_SELECTED = new BooleanSetting("revanced_remember_video_quality_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_video_quality_last_selected_toast", TRUE);
    public static final FloatSetting DEFAULT_PLAYBACK_SPEED = new FloatSetting("revanced_default_playback_speed", 1.0f);
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_MOBILE = new IntegerSetting("revanced_default_video_quality_mobile", -2);
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_WIFI = new IntegerSetting("revanced_default_video_quality_wifi", -2);


    // PreferenceScreen: Miscellaneous
    public static final BooleanSetting CHANGE_SHARE_SHEET = new BooleanSetting("revanced_change_share_sheet", FALSE, true);
    public static final BooleanSetting DISABLE_CAIRO_SPLASH_ANIMATION = new BooleanSetting("revanced_disable_cairo_splash_animation", FALSE, true);
    public static final BooleanSetting DISABLE_DRC_AUDIO = new BooleanSetting("revanced_disable_drc_audio", FALSE, true);
    public static final BooleanSetting ENABLE_OPUS_CODEC = new BooleanSetting("revanced_enable_opus_codec", FALSE, true);
    public static final BooleanSetting SETTINGS_IMPORT_EXPORT = new BooleanSetting("revanced_extended_settings_import_export", FALSE, false);


    // PreferenceScreen: Return YouTube Dislike
    public static final BooleanSetting RYD_ENABLED = new BooleanSetting("revanced_ryd_enabled", TRUE);
    public static final StringSetting RYD_USER_ID = new StringSetting("revanced_ryd_user_id", "", false, false);
    public static final BooleanSetting RYD_DISLIKE_PERCENTAGE = new BooleanSetting("revanced_ryd_dislike_percentage", FALSE);
    public static final BooleanSetting RYD_COMPACT_LAYOUT = new BooleanSetting("revanced_ryd_compact_layout", FALSE);
    public static final BooleanSetting RYD_ESTIMATED_LIKE = new BooleanSetting("revanced_ryd_estimated_like", FALSE, true);
    public static final BooleanSetting RYD_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("revanced_ryd_toast_on_connection_error", FALSE);

    // PreferenceScreen: Return YouTube Username
    public static final BooleanSetting RETURN_YOUTUBE_USERNAME_ABOUT = new BooleanSetting("revanced_return_youtube_username_youtube_data_api_v3_about", FALSE, false);


    // PreferenceScreen: SponsorBlock
    public static final BooleanSetting SB_ENABLED = new BooleanSetting("sb_enabled", TRUE);
    public static final BooleanSetting SB_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("sb_toast_on_connection_error", FALSE);
    public static final BooleanSetting SB_TOAST_ON_SKIP = new BooleanSetting("sb_toast_on_skip", TRUE);
    public static final StringSetting SB_API_URL = new StringSetting("sb_api_url", "https://sponsor.ajay.app");
    public static final StringSetting SB_PRIVATE_USER_ID = new StringSetting("sb_private_user_id", "");
    public static final BooleanSetting SB_USER_IS_VIP = new BooleanSetting("sb_user_is_vip", FALSE);

    public static final StringSetting SB_CATEGORY_SPONSOR = new StringSetting("sb_sponsor", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_SPONSOR_COLOR = new StringSetting("sb_sponsor_color", "#00D400");
    public static final StringSetting SB_CATEGORY_SELF_PROMO = new StringSetting("sb_selfpromo", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_SELF_PROMO_COLOR = new StringSetting("sb_selfpromo_color", "#FFFF00");
    public static final StringSetting SB_CATEGORY_INTERACTION = new StringSetting("sb_interaction", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_INTERACTION_COLOR = new StringSetting("sb_interaction_color", "#CC00FF");
    public static final StringSetting SB_CATEGORY_INTRO = new StringSetting("sb_intro", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_INTRO_COLOR = new StringSetting("sb_intro_color", "#00FFFF");
    public static final StringSetting SB_CATEGORY_OUTRO = new StringSetting("sb_outro", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_OUTRO_COLOR = new StringSetting("sb_outro_color", "#0202ED");
    public static final StringSetting SB_CATEGORY_PREVIEW = new StringSetting("sb_preview", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_PREVIEW_COLOR = new StringSetting("sb_preview_color", "#008FD6");
    public static final StringSetting SB_CATEGORY_FILLER = new StringSetting("sb_filler", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_FILLER_COLOR = new StringSetting("sb_filler_color", "#7300FF");
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC = new StringSetting("sb_music_offtopic", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC_COLOR = new StringSetting("sb_music_offtopic_color", "#FF9900");

    // SB settings not exported
    public static final LongSetting SB_LAST_VIP_CHECK = new LongSetting("sb_last_vip_check", 0L, false, false);

    static {
        // region SB import/export callbacks

        Setting.addImportExportCallback(SponsorBlockSettings.SB_IMPORT_EXPORT_CALLBACK);

        // endregion
    }

    public static final String OPEN_DEFAULT_APP_SETTINGS = "revanced_default_app_settings";

    /**
     * If a setting path has this prefix, then remove it.
     */
    public static final String OPTIONAL_SPONSOR_BLOCK_SETTINGS_PREFIX = "sb_segments_";

    /**
     * Array of settings using intent
     */
    private static final String[] intentSettingArray = new String[]{
            BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN.key,
            CHANGE_START_PAGE.key,
            CUSTOM_FILTER_STRINGS.key,
            CUSTOM_PLAYBACK_SPEEDS.key,
            EXTERNAL_DOWNLOADER_PACKAGE_NAME.key,
            HIDE_ACCOUNT_MENU_FILTER_STRINGS.key,
            SB_API_URL.key,
            SETTINGS_IMPORT_EXPORT.key,
            SPOOF_APP_VERSION_TARGET.key,
            SPOOF_STREAMING_DATA_TYPE.key,
            RETURN_YOUTUBE_USERNAME_ABOUT.key,
            RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT.key,
            RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY.key,
            OPEN_DEFAULT_APP_SETTINGS,
            OPTIONAL_SPONSOR_BLOCK_SETTINGS_PREFIX
    };

    /**
     * @return whether dataString contains settings that use Intent
     */
    public static boolean includeWithIntent(@NonNull String dataString) {
        return Utils.containsAny(dataString, intentSettingArray);
    }
}
