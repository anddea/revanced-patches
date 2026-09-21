package app.morphe.extension.music.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.morphe.extension.music.sponsorblock.objects.CategoryBehaviour.IGNORE;
import static app.morphe.extension.music.sponsorblock.objects.CategoryBehaviour.SKIP_AUTOMATICALLY;
import static app.morphe.extension.shared.settings.Setting.parent;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import app.morphe.extension.music.patches.CrossfadeManager.CrossFadeDuration;
import app.morphe.extension.music.patches.CrossfadeManager.FadeCurve;
import app.morphe.extension.music.patches.general.ChangeStartPagePatch.StartPage;
import app.morphe.extension.music.patches.misc.AlbumMusicVideoPatch.RedirectType;
import app.morphe.extension.music.patches.utils.PatchStatus;
import app.morphe.extension.music.patches.utils.DrawableColorPatch;
import app.morphe.extension.music.sponsorblock.SponsorBlockSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.EnumSetting;
import app.morphe.extension.shared.settings.FloatSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.settings.LongSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class Settings extends SharedYouTubeSettings {
    /** Enables the notification-dot picker for every theme except Material You. */
    private static Setting.Availability notificationDotColorAvailability() {
        return new Setting.Availability() {
            @Override
            public boolean isAvailable() {
                return !DARK_THEME.get().startsWith("material_you_");
            }

            @Override
            public List<Setting<?>> getParentSettings() {
                return Collections.singletonList(DARK_THEME);
            }
        };
    }

    public static final EnumSetting<ClientType> SPOOF_VIDEO_STREAMS_CLIENT_TYPE =
            new EnumSetting<>("morphe_spoof_video_streams_client_type",
                    ClientType.VISIONOS_1_02, true, parent(SPOOF_VIDEO_STREAMS));

    // PreferenceScreen: Account
    public static final BooleanSetting HIDE_ACCOUNT_MENU = new BooleanSetting("revanced_hide_account_menu", FALSE);
    public static final StringSetting HIDE_ACCOUNT_MENU_FILTER_STRINGS = new StringSetting("revanced_hide_account_menu_filter_strings", "", true);
    public static final BooleanSetting HIDE_ACCOUNT_MENU_EMPTY_COMPONENT = new BooleanSetting("revanced_hide_account_menu_empty_component", FALSE);
    public static final BooleanSetting HIDE_HANDLE = new BooleanSetting("revanced_hide_handle", TRUE, true);
    public static final BooleanSetting HIDE_TERMS_CONTAINER = new BooleanSetting("revanced_hide_terms_container", FALSE);


    // PreferenceScreen: Action Bar
    public static final BooleanSetting CHANGE_ACTION_BAR_POSITION = new BooleanSetting("revanced_change_action_bar_position", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_LIKE_DISLIKE = new BooleanSetting("revanced_hide_action_button_like_dislike", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_COMMENT = new BooleanSetting("revanced_hide_action_button_comment", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_LIVE_CHAT_REPLAY = new BooleanSetting("revanced_hide_action_button_live_chat_replay", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_DETAILS = new BooleanSetting("revanced_hide_action_button_details", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST = new BooleanSetting("revanced_hide_action_button_add_to_playlist", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_DOWNLOAD = new BooleanSetting("revanced_hide_action_button_download", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_LYRICS = new BooleanSetting("revanced_hide_action_button_lyrics", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_SHARE = new BooleanSetting("revanced_hide_action_button_share", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_SONG_VIDEO = new BooleanSetting("revanced_hide_action_button_song_video", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_RADIO = new BooleanSetting("revanced_hide_action_button_radio", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_DISABLED = new BooleanSetting("revanced_hide_action_button_disabled", FALSE, true);
    public static final BooleanSetting HIDE_ACTION_BUTTON_LABEL = new BooleanSetting("revanced_hide_action_button_label", FALSE, true);
    public static final BooleanSetting REPLACE_ACTION_BUTTON_LIKE = new BooleanSetting("revanced_replace_action_button_like", FALSE, true);
    public static final BooleanSetting REPLACE_ACTION_BUTTON_LIKE_TYPE = new BooleanSetting("revanced_replace_action_button_like_type", FALSE, true);


    // PreferenceScreen: Ads
    public static final BooleanSetting HIDE_GENERAL_ADS = new BooleanSetting("revanced_hide_general_ads", TRUE, true);
    public static final BooleanSetting HIDE_MUSIC_ADS = new BooleanSetting("revanced_hide_music_ads", TRUE, true);
    public static final BooleanSetting HIDE_PAID_PROMOTION_LABEL = new BooleanSetting("revanced_hide_paid_promotion_label", TRUE, true);
    public static final BooleanSetting HIDE_PREMIUM_PROMOTION = new BooleanSetting("revanced_hide_premium_promotion", TRUE, true);
    public static final BooleanSetting HIDE_PREMIUM_RENEWAL = new BooleanSetting("revanced_hide_premium_renewal", TRUE, true);


    // PreferenceScreen: Flyout menu
    public static final BooleanSetting DISABLE_TRIM_SILENCE = new BooleanSetting("revanced_disable_trim_silence", TRUE);
    public static final BooleanSetting ENABLE_COMPACT_DIALOG = new BooleanSetting("revanced_enable_compact_dialog", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_LIKE_DISLIKE = new BooleanSetting("revanced_hide_flyout_menu_like_dislike", FALSE, true);
    public static final BooleanSetting HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT = new BooleanSetting("revanced_hide_flyout_menu_3_column_component", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_ADD_TO_QUEUE = new BooleanSetting("revanced_hide_flyout_menu_add_to_queue", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_CAPTIONS = new BooleanSetting("revanced_hide_flyout_menu_captions", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DELETE_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_delete_playlist", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DISMISS_QUEUE = new BooleanSetting("revanced_hide_flyout_menu_dismiss_queue", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_DOWNLOAD = new BooleanSetting("revanced_hide_flyout_menu_download", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_EDIT_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_edit_playlist", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_ALBUM = new BooleanSetting("revanced_hide_flyout_menu_go_to_album", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_ARTIST = new BooleanSetting("revanced_hide_flyout_menu_go_to_artist", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_EPISODE = new BooleanSetting("revanced_hide_flyout_menu_go_to_episode", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_GO_TO_PODCAST = new BooleanSetting("revanced_hide_flyout_menu_go_to_podcast", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_HELP = new BooleanSetting("revanced_hide_flyout_menu_help", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_NOT_INTERESTED = new BooleanSetting("revanced_hide_flyout_menu_not_interested", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_PIN_TO_SPEED_DIAL = new BooleanSetting("revanced_hide_flyout_menu_pin_to_speed_dial", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_PLAY_NEXT = new BooleanSetting("revanced_hide_flyout_menu_play_next", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_QUALITY = new BooleanSetting("revanced_hide_flyout_menu_quality", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY = new BooleanSetting("revanced_hide_flyout_menu_remove_from_library", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REMOVE_FROM_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_remove_from_playlist", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_REPORT = new BooleanSetting("revanced_hide_flyout_menu_report", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY = new BooleanSetting("revanced_hide_flyout_menu_save_episode_for_later_save_to_library", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SAVE_TO_PLAYLIST = new BooleanSetting("revanced_hide_flyout_menu_save_to_playlist", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SHARE = new BooleanSetting("revanced_hide_flyout_menu_share", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SHUFFLE_PLAY = new BooleanSetting("revanced_hide_flyout_menu_shuffle_play", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SLEEP_TIMER = new BooleanSetting("revanced_hide_flyout_menu_sleep_timer", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_START_RADIO = new BooleanSetting("revanced_hide_flyout_menu_start_radio", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_STATS_FOR_NERDS = new BooleanSetting("revanced_hide_flyout_menu_stats_for_nerds", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_SUBSCRIBE = new BooleanSetting("revanced_hide_flyout_menu_subscribe", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_TASTE_MATCH = new BooleanSetting("revanced_hide_flyout_menu_taste_match", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_UNPIN_FROM_SPEED_DIAL = new BooleanSetting("revanced_hide_flyout_menu_unpin_from_speed_dial", FALSE);
    public static final BooleanSetting HIDE_FLYOUT_MENU_VIEW_SONG_CREDIT = new BooleanSetting("revanced_hide_flyout_menu_view_song_credit", FALSE);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE = new BooleanSetting("revanced_replace_flyout_menu_dismiss_queue", FALSE);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE_CONTINUE_WATCH = new BooleanSetting("revanced_replace_flyout_menu_dismiss_queue_continue_watch", TRUE);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT = new BooleanSetting("revanced_replace_flyout_menu_report", TRUE);
    public static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER = new BooleanSetting("revanced_replace_flyout_menu_report_only_player", TRUE);


    /** Precompiled presets work on Android 8+, while arbitrary colors require Android 11+. */
    public static final StringSetting DARK_THEME = new StringSetting(
            "morphe_dark_theme", DrawableColorPatch.DEFAULT_DARK_THEME, true);
    /** Arbitrary runtime color, enabled for the Custom selector on Android 11+. */
    public static final StringSetting DARK_THEME_CUSTOM_COLOR = new StringSetting(
            "morphe_dark_theme_custom_color",
            DrawableColorPatch.DEFAULT_DARK_THEME_CUSTOM_COLOR,
            true,
            new Setting.Availability() {
                @Override
                public boolean isAvailable() {
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            && DARK_THEME.isAvailable()
                            && "custom".equals(DARK_THEME.get());
                }

                @Override
                public boolean isVisible() {
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                            && DARK_THEME.isVisible();
                }

                @Override
                public List<Setting<?>> getParentSettings() {
                    return Collections.singletonList(DARK_THEME);
                }
            });
    public static final StringSetting NOTIFICATION_DOT_COLOR = new StringSetting(
            "morphe_notification_dot_color",
            DrawableColorPatch.DEFAULT_NOTIFICATION_DOT_COLOR,
            true,
            notificationDotColorAvailability());
    public static final EnumSetting<StartPage> CHANGE_START_PAGE = new EnumSetting<>("revanced_change_start_page", StartPage.DEFAULT, true);
    public static final BooleanSetting DISABLE_CAIRO_SPLASH_ANIMATION = new BooleanSetting("revanced_disable_cairo_splash_animation", FALSE, true);
    public static final BooleanSetting DISABLE_DISLIKE_REDIRECTION = new BooleanSetting("revanced_disable_dislike_redirection", FALSE);
    public static final BooleanSetting ENABLE_LANDSCAPE_MODE = new BooleanSetting("revanced_enable_landscape_mode", FALSE, true);
    public static final BooleanSetting CUSTOM_FILTER = new BooleanSetting("revanced_custom_filter", FALSE);
    public static final StringSetting CUSTOM_FILTER_STRINGS = new StringSetting("revanced_custom_filter_strings", "", true);

    // Settings menu filter
    public static final StringSetting SETTINGS_MENU_FILTER_STRINGS = new StringSetting("morphe_settings_menu_filter_strings", "", true);
    public static final StringSetting SETTINGS_MENU_FILTER_DISCOVERED = new StringSetting("morphe_settings_menu_filter_discovered", "", true, false);
    public static final BooleanSetting HIDE_BUTTON_SHELF = new BooleanSetting("revanced_hide_button_shelf", FALSE, true);
    public static final BooleanSetting HIDE_CAROUSEL_SHELF = new BooleanSetting("revanced_hide_carousel_shelf", FALSE, true);
    public static final BooleanSetting HIDE_CAST_BUTTON = new BooleanSetting("revanced_hide_cast_button", TRUE);
    public static final BooleanSetting HIDE_CATEGORY_BAR = new BooleanSetting("revanced_hide_category_bar", FALSE, true);
    public static final BooleanSetting HIDE_FLOATING_BUTTON = new BooleanSetting("revanced_hide_floating_button", FALSE, true);
    public static final BooleanSetting HIDE_HISTORY_BUTTON = new BooleanSetting("revanced_hide_history_button", FALSE);
    public static final BooleanSetting HIDE_NOTIFICATION_BUTTON = new BooleanSetting("revanced_hide_notification_button", FALSE, true);
    public static final BooleanSetting HIDE_PLAYLIST_CARD_SHELF = new BooleanSetting("revanced_hide_playlist_card_shelf", FALSE, true);
    public static final BooleanSetting HIDE_SAMPLE_SHELF = new BooleanSetting("revanced_hide_samples_shelf", FALSE, true);
    public static final BooleanSetting HIDE_SEARCH_BUTTON = new BooleanSetting("revanced_hide_search_button", FALSE, true);
    public static final BooleanSetting HIDE_SOUND_SEARCH_BUTTON = new BooleanSetting("revanced_hide_sound_search_button", FALSE, true);
    public static final BooleanSetting HIDE_TAP_TO_UPDATE_BUTTON = new BooleanSetting("revanced_hide_tap_to_update_button", FALSE, true);
    public static final BooleanSetting HIDE_VOICE_SEARCH_BUTTON = new BooleanSetting("revanced_hide_voice_search_button", FALSE, true);
    public static final BooleanSetting REMOVE_VIEWER_DISCRETION_DIALOG = new BooleanSetting("revanced_remove_viewer_discretion_dialog", FALSE);
    public static final BooleanSetting RESTORE_OLD_STYLE_LIBRARY_SHELF = new BooleanSetting("revanced_restore_old_style_library_shelf", FALSE, true);
    public static final BooleanSetting SPOOF_APP_VERSION = new BooleanSetting("revanced_spoof_app_version",
            PatchStatus.SpoofAppVersionDefaultBoolean(), true);
    public static final StringSetting SPOOF_APP_VERSION_TARGET = new StringSetting("revanced_spoof_app_version_target",
            PatchStatus.SpoofAppVersionDefaultString(), true);
    public static final BooleanSetting SPOOF_APP_VERSION_FOR_LYRICS = new BooleanSetting("revanced_spoof_app_version_for_lyrics", FALSE, true);
    public static final StringSetting SPOOF_APP_VERSION_FOR_LYRICS_TARGET = new StringSetting("revanced_spoof_app_version_for_lyrics_target",
            "6.42.55", true);


    // PreferenceScreen: Navigation Bar
    public static final BooleanSetting ENABLE_CUSTOM_NAVIGATION_BAR_COLOR = new BooleanSetting("revanced_enable_custom_navigation_bar_color", FALSE, true);
    public static final StringSetting ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE = new StringSetting("revanced_custom_navigation_bar_color_value", "#000000", true);
    public static final BooleanSetting HIDE_NAVIGATION_HOME_BUTTON = new BooleanSetting("revanced_hide_navigation_home_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_SAMPLES_BUTTON = new BooleanSetting("revanced_hide_navigation_samples_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_EXPLORE_BUTTON = new BooleanSetting("revanced_hide_navigation_explore_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LIBRARY_BUTTON = new BooleanSetting("revanced_hide_navigation_library_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_UPGRADE_BUTTON = new BooleanSetting("revanced_hide_navigation_upgrade_button", TRUE, true);
    public static final BooleanSetting HIDE_NAVIGATION_BAR = new BooleanSetting("revanced_hide_navigation_bar", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LABEL = new BooleanSetting("revanced_hide_navigation_label", FALSE, true);
    public static final BooleanSetting REPLACE_NAVIGATION_SAMPLES_BUTTON = new BooleanSetting("revanced_replace_navigation_samples_button", FALSE, true);
    public static final BooleanSetting REPLACE_NAVIGATION_UPGRADE_BUTTON = new BooleanSetting("revanced_replace_navigation_upgrade_button", FALSE, true);
    public static final BooleanSetting REPLACE_NAVIGATION_BUTTON_ABOUT = new BooleanSetting("revanced_replace_navigation_button_about", FALSE, false);


    // PreferenceScreen: Player
    public static final BooleanSetting ADD_MINIPLAYER_NEXT_BUTTON = new BooleanSetting("revanced_add_miniplayer_next_button", TRUE, true);
    public static final BooleanSetting ADD_MINIPLAYER_PREVIOUS_BUTTON = new BooleanSetting("revanced_add_miniplayer_previous_button", TRUE, true);
    public static final BooleanSetting CHANGE_MINIPLAYER_COLOR = new BooleanSetting("revanced_change_miniplayer_color", FALSE);
    public static final BooleanSetting CHANGE_NAVIGATION_BAR_COLOR = new BooleanSetting("revanced_music_change_navigation_bar_color", FALSE, true, parent(CHANGE_MINIPLAYER_COLOR));
    public static final BooleanSetting CHANGE_PLAYER_BACKGROUND_COLOR = new BooleanSetting("revanced_change_player_background_color", FALSE, true);
    public static final BooleanSetting CROSSFADE_ENABLED = new BooleanSetting("morphe_music_crossfade_enabled", FALSE, true);
    public static final EnumSetting<FadeCurve> CROSSFADE_CURVE = new EnumSetting<>("morphe_music_crossfade_curve", FadeCurve.EQUAL_POWER);
    public static final EnumSetting<CrossFadeDuration> CROSSFADE_DURATION = new EnumSetting<>("morphe_music_crossfade_duration", CrossFadeDuration.MILLISECONDS_3000);
    public static final BooleanSetting CROSSFADE_ON_SKIP = new BooleanSetting("morphe_music_crossfade_on_skip", TRUE);
    public static final BooleanSetting CROSSFADE_ON_AUTO_ADVANCE = new BooleanSetting("morphe_music_crossfade_on_auto_advance", TRUE);
    public static final BooleanSetting CROSSFADE_SESSION_CONTROL = new BooleanSetting("morphe_music_crossfade_session_control", TRUE);
    public static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY = new StringSetting("revanced_custom_player_background_color_primary", "#000000", true);
    public static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY = new StringSetting("revanced_custom_player_background_color_secondary", "#000000", true);
    public static final BooleanSetting CHANGE_SEEK_BAR_POSITION = new BooleanSetting("revanced_change_seekbar_position", FALSE, true);
    public static final BooleanSetting DISABLE_MINIPLAYER_GESTURE = new BooleanSetting("revanced_disable_miniplayer_gesture", FALSE, true);
    public static final BooleanSetting DISABLE_PLAYER_GESTURE = new BooleanSetting("revanced_disable_player_gesture", FALSE, true);
    public static final BooleanSetting ENABLE_FORCED_MINIPLAYER = new BooleanSetting("revanced_enable_forced_miniplayer", TRUE);
    public static final BooleanSetting ENABLE_SMOOTH_TRANSITION_ANIMATION = new BooleanSetting("revanced_enable_smooth_transition_animation", TRUE, true);
    public static final BooleanSetting ENABLE_SWIPE_TO_DISMISS_MINIPLAYER = new BooleanSetting("revanced_enable_swipe_to_dismiss_miniplayer", TRUE, true);
    public static final BooleanSetting ENABLE_THICK_SEEKBAR = new BooleanSetting("revanced_enable_thick_seekbar", TRUE, true);
    public static final BooleanSetting ENABLE_ZEN_MODE = new BooleanSetting("revanced_enable_zen_mode", FALSE, true);
    public static final BooleanSetting ENABLE_ZEN_MODE_PODCAST = new BooleanSetting("revanced_enable_zen_mode_podcast", FALSE, true);
    public static final BooleanSetting HIDE_COMMENT_CHANNEL_GUIDELINES = new BooleanSetting("revanced_hide_comment_channel_guidelines", TRUE);
    public static final BooleanSetting HIDE_DOUBLE_TAP_OVERLAY_FILTER = new BooleanSetting("revanced_hide_double_tap_overlay_filter", FALSE, true);
    public static final BooleanSetting HIDE_COMMENT_TIMESTAMP_AND_EMOJI_BUTTONS = new BooleanSetting("revanced_hide_comment_timestamp_and_emoji_buttons", FALSE);
    public static final BooleanSetting HIDE_FULLSCREEN_SHARE_BUTTON = new BooleanSetting("revanced_hide_fullscreen_share_button", FALSE, true);
    public static final BooleanSetting HIDE_LYRICS_SHARE_BUTTON = new BooleanSetting("revanced_hide_lyrics_share_button", FALSE);
    public static final BooleanSetting HIDE_SONG_VIDEO_TOGGLE = new BooleanSetting("revanced_hide_song_video_toggle", FALSE, true);
    public static final BooleanSetting REMEMBER_REPEAT_SATE = new BooleanSetting("revanced_remember_repeat_state", TRUE);
    public static final BooleanSetting REMEMBER_SHUFFLE_SATE = new BooleanSetting("revanced_remember_shuffle_state", TRUE);
    public static final BooleanSetting ALWAYS_SHUFFLE = new BooleanSetting("revanced_always_shuffle", FALSE);
    public static final BooleanSetting RESTORE_OLD_COMMENTS_POPUP_PANELS = new BooleanSetting("revanced_restore_old_comments_popup_panels", FALSE, true);
    public static final BooleanSetting RESTORE_OLD_PLAYER_BACKGROUND = new BooleanSetting("revanced_restore_old_player_background", FALSE, true);
    public static final BooleanSetting RESTORE_OLD_PLAYER_LAYOUT = new BooleanSetting("revanced_restore_old_player_layout", FALSE, true);

    // PreferenceScreen: Lyrics
    public static final BooleanSetting LYRICS_ENABLED = new BooleanSetting("morphe_music_lyrics_enabled", FALSE, true);
    public static final BooleanSetting LYRICS_KEEP_SCREEN_ON = new BooleanSetting("morphe_music_lyrics_keep_screen_on", FALSE, true);
    public static final String DEFAULT_LYRICS_ORDER =
            "YTMusic,-Captions,Apple,LRCLIB,QQ,NetEase,KuGou,Luna,-PetitLyrics,-bLyrics,-BiniLyrics,-Unison,-SimpMusic,-AMLL,-LunaBeat,-Lyricify,-Spotify,-Musixmatch,-Deezer,";
    public static final StringSetting LYRICS_SOURCE = new StringSetting("morphe_music_lyrics_source", DEFAULT_LYRICS_ORDER, true, parent(LYRICS_ENABLED));
    public static final StringSetting APPLE_MUSIC_TOKEN = new StringSetting("morphe_music_apple_music_token", "", true, parent(LYRICS_ENABLED));
    public static final StringSetting SPOTIFY_TOKEN = new StringSetting("morphe_music_spotify_token", "", true, parent(LYRICS_ENABLED));
    public static final StringSetting DEEZER_ARL = new StringSetting("morphe_music_deezer_arl", "", true, parent(LYRICS_ENABLED));
    public static final StringSetting MUSIXMATCH_TOKEN = new StringSetting("morphe_music_musixmatch_token", "", true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_TRANSLATE = new BooleanSetting("morphe_music_lyrics_translate", FALSE, true, parent(LYRICS_ENABLED));
    public static final StringSetting LYRICS_TRANSLATION_LANGUAGE = new StringSetting("morphe_music_lyrics_translation_language", "DEFAULT", true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_TAP_TO_SEEK = new BooleanSetting("morphe_music_lyrics_tap_to_seek", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_SHOW_COPY_BUTTON = new BooleanSetting("morphe_music_lyrics_show_copy_button", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_SHOW_TRANSLATE_BUTTON = new BooleanSetting("morphe_music_lyrics_show_translate_button", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_USE_AI_TRANSLATION = new BooleanSetting("morphe_music_lyrics_use_ai_translation", FALSE, true, parent(LYRICS_ENABLED));
    public static final StringSetting LYRICS_AI_BASE_URL = new StringSetting("morphe_music_lyrics_ai_base_url", "https://text.pollinations.ai/openai", true, parent(LYRICS_USE_AI_TRANSLATION));
    public static final StringSetting LYRICS_AI_API_TOKEN = new StringSetting("morphe_music_lyrics_ai_api_token", "", true, parent(LYRICS_USE_AI_TRANSLATION));
    public static final StringSetting LYRICS_AI_MODEL = new StringSetting("morphe_music_lyrics_ai_model", "openai-fast", true, parent(LYRICS_USE_AI_TRANSLATION));
    public static final BooleanSetting LYRICS_SHOW_ROMANIZE_BUTTON = new BooleanSetting("morphe_music_lyrics_show_romanize_button", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_SHOW_REFRESH_BUTTON = new BooleanSetting("morphe_music_lyrics_show_refresh_button", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_HIDE_INFO = new BooleanSetting("morphe_music_lyrics_hide_info", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_SWAP_TRANS_ROMA = new BooleanSetting("morphe_music_lyrics_swap_trans_roma", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_ROMANIZE = new BooleanSetting("morphe_music_lyrics_romanize", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_WORD_SYNC = new BooleanSetting("morphe_music_lyrics_word_sync", TRUE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_HIDE_PLAYED = new BooleanSetting("morphe_music_lyrics_hide_played", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_HIDE_UNPLAYED = new BooleanSetting("morphe_music_lyrics_hide_unplayed", FALSE, true, parent(LYRICS_ENABLED));
    public static final IntegerSetting LYRICS_TEXT_SIZE = new IntegerSetting(
            "morphe_music_lyrics_text_size", 24, true,
            new Setting.SliderConfig(14, 40, 1, "sp"), parent(LYRICS_ENABLED));
    public static final IntegerSetting LYRICS_OFFSET_MS = new IntegerSetting(
            "morphe_music_lyrics_offset_ms", 0, true,
            new Setting.SliderConfig(-2_000, 2_000, 1, "ms"), parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_MEDIASESSION = new BooleanSetting("morphe_music_lyrics_mediasession", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_MINIPLAYER = new BooleanSetting("morphe_music_lyrics_miniplayer", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_DISPLAY_ARTIST_FIRST = new BooleanSetting("morphe_music_lyrics_display_artist_first", FALSE, true, parent(LYRICS_ENABLED));
    public static final BooleanSetting LYRICS_USE_EMBEDDED = new BooleanSetting("morphe_music_lyrics_use_embedded", TRUE, true, parent(LYRICS_ENABLED));
    public static final StringSetting LYRICS_CAPTION_COOKIES = new StringSetting("morphe_music_lyrics_caption_cookies", "", true, parent(LYRICS_ENABLED));
    public static final String DEFAULT_LYRICS_REGEX =
            "(?i)\\s*[（(\\[]((official\\s+)?(video|audio|music\\s+video|lyrics?\\s+video|visualizer|mv))[）)\\]]"
            + "|(?i)\\s*[（(\\[]((\\d{4}\\s+)?remaster(ed)?(\\s+\\d{4})?)[）)\\]]"
            + "|(?i)\\s*[（(\\[](mono|stereo|hq|hd|4k|8k)[）)\\]]"
            + "|[（(][^）)]*(?:主题曲|片尾曲|插曲|片头曲|广告曲|推广曲)[^）)]*[）)]"
            + "|[（(][^）)]*[\\uff1a:][^）)]*[）)]"
            + "|(?i)\\s*-\\s*topic$";
    public static final StringSetting LYRICS_CUSTOM_REGEX = new StringSetting("morphe_music_lyrics_custom_regex", DEFAULT_LYRICS_REGEX, true, parent(LYRICS_ENABLED));
    public static final String DEFAULT_LYRICS_TEXT_FILTER =
            ".*?(?:"
            + "未经.*?(?:不得|禁止)"
            + "|本作品声明.*?著作权权利保留.*?不得"
            + "|本字幕由TME AI技术生成"
            + "|部分素材源自网络"
            + "|酷我音乐.*?特别出品"
            + "|酷狗.*?星曜计划"
            + "|酷狗.*?国潮"
            + "|酷狗音乐.*?就是歌多"
            + "|听国潮.*?酷狗"
            + "|未经许可.*?(?:翻唱|盗版)"
            + "|本作品.*?授权"
            + "|已获得.*?授权"
            + "|星曜计划.*?企划|黑胶复刻"
            + "|此歌曲为没有填词的纯音乐"
            + "|纯音乐，请欣赏"
            + "|此歌曲由Vemus未音APP\\.制作 音乐创作如此简单！"
            + "|酷狗音乐『万物皆可dj』企划"
            + "|『听dj, 到中国酷狗』"
            + "|本歌曲来自〖飓风计划〗"
            + "|10亿现金激励，千亿流量扶持！"
            + "|版权所有\\s*未经\\s*许可\\s*请勿\\s*使用"
            + "|想听的歌在评\\s*论区"
            + ").*";
    public static final StringSetting LYRICS_TEXT_FILTER = new StringSetting("morphe_music_lyrics_text_filter", DEFAULT_LYRICS_TEXT_FILTER, true, parent(LYRICS_ENABLED));
    public static final String DEFAULT_LYRICS_CREDIT_LINE_REGEX =
           "A&R,A.Guita,AU,Additional Drums Engineering,Administer,Administered,Administered By,Administering,Administers,"
            + "Agency,Album,All Instruments,Arranged,Arranged By,Arranger,Arrangers,Arranging,"
            + "Artist,Artists,Assistant Engineer,Assistant Engineers,Assistant Mix Engineer,"
            + "Assistant Mix Engineers,Author,Authoring,Authors,Autotune,Backed,Background,"
            + "Background Vocal,Background Vocals,Backing,Bass,Bass Guitar,COS,CV,Child,Child Choir,"
            + "Child Choir Instruction,Child Lead,Children,Composed By,Composer,Composers,"
            + "Composing,Copyright,Cover,Credit,DJ,Digital Edited,Digital Edited By,"
            + "Digital Editing,Directed,Directed By,Directing,Director,Directors,Drum,Drums,"
            + "Duration,E.Guitar,Edited,Edited By,Editing Engineer,Editing Engineers,Editor,"
            + "Editors,Engineer,Engineered,Engineered By,Engineering,Engineers,Executed,Executing,"
            + "Executive,Guitar,Group,Harmony,ISRC,Keyboard,LA,Lang,Language,Lead,Leader,Leaders,"
            + "Length,Lyric,Lyricist,Lyricists,Lyrics,Lyrics By,MV,Main Sample,Manufactory,"
            + "Manufactured,Manufactured By,Manufacturing,Master,Mastered,Mastered By,Mastering,"
            + "Mastering Engineer,Mastering Engineers,Masters,Mix Engineer,Mix Engineered by,Mixed,"
            + "Mixed By,Mixer,Mixers,Mixing,Mixing Engineer,Mixing Studio,Music,OA,OC,OP,OT,Original Lyrics by,"
            + "Original Title,Original Publisher,Original Writer,PGM,PV,Percussion,Performed,"
            + "Performed By,Performer,Performers,Performing,Pro-Tools Editing,Produced,Produced By,"
            + "Producer,Producers,Producing,Program,Programming by,Published,Published By,Publisher,"
            + "Publishers,Publishing,Publishing Group,Publishing Group Administered By,QQ,RE,Rap,"
            + "Record,Recorded,Recorded At,Recorded By,Recorder,Recorders,Recording,Recording Engineer,Recordings,"
            + "Records,SP,Sample,Sampled,Samples,Sampling,Singer,Singers,Singing,Song,Strings,"
            + "Studio,Sub,Sub Publisher,Subs,Subscribe,Subscribed,Subscriber,Subscribers,Surround,"
            + "Synthesizer,Synthesizers,TA,Title,VE,Ver,Version,Vocal,Vocal Arrangement,"
            + "Vocal Directed,Vocal Directed By,Vocal Director,Vocal Engineer,Vocal Engineering,"
            + "Vocal Produced,Vocal Produced By,Vocal Producer,Vocal Producers,Vocals,"
            + "Vocals Arrangement,Voice,Written,Written By,Writter,"
            + "专辑,业务联系,业务邮箱,中提,中提琴,中提琴手,中文,中文词SA,主催,主唱,乐器,乐团,乐队,书法,竖琴吉他,二胡,人声,"
            + "企业宣传,企划,企宣,伴唱,伴奏,伴舞,低音提琴,低音吉他,作曲,作画,作者,作词,修音,修音师,公司,出品,出品人,创作,"
            + "创作者,指挥,创意,制作,制作人,前置混音,剧情,剪纸艺术家,助力推广,助理,协力,厂牌,原唱,原曲,原歌名,"
            + "原版,原画,原编曲,原翻,原著,原词,原词曲,发型,发布,发布者,发行,口琴,口风琴,古筝,合作,合作伙伴,合作者,"
            + "合声,合成,合成器,合音,合唱,吉他,后期,吟唱,和声,和音,唢呐,商务,团队,图,图片,图画,地址,场景,场景提供,"
            + "填词,声乐,声音,处理,大提,大提琴,大提琴手,女声,妆造,官方,官方指定音乐合作伙伴,宣传,宣发,宣推,"
            + "富鲁格,导演,封设,封面,小号,小提,小提琴,小提琴手,第一小提琴,第二小提琴,工作室,工程,工程师,平面设计,平台,弦乐,弦乐团,录音,"
            + "录音室,录音师,录音棚,录制,微信,微博,快手,念白,总企划,总监,总监制,总策划,总顾问,手碟,手风琴,打击乐,"
            + "执行,抖音,短视频平台总统筹,短视频平台宣推,短视频宣推,短视频统筹,拍摄,指定音乐合作伙伴,指导,指导老师,推广,摄影,改编,改编词,文案,时长,曲,曲Composer,"
            + "曲Music,曲名,曲绘,曲编,曲协力,木吉他,木管,杜比全景声,板胡,校准,次中音萨克斯,歌名,歌声,歌手,歌曲,"
            + "歌词改编,歌唱指导,母带,海外配唱执行,海报,混缩,混缩室,混音,混音室,混音师,混音棚,滤镜,演唱,演奏,"
            + "漫画,灯光,版本,版权,版权归属,特别鸣谢,班卓琴,琵琶,电吉他,电脑工程,电钢琴,男声,画,画师,监制,监唱,私人,"
            + "童声,笛,笛子,笛萧,策划,管乐,管弦,管弦乐,箫,粤语,经纪,统筹,编,编写,编剧,编唱,编导,编曲,编舞,"
            + "编舞师,编著,编辑,缩混,网易音乐人商务合作,美工,美术,美术设计,翻唱,翻译,翻译者,联合,联系,联系方式,"
            + "舞台,舞团,舞曲,舞蹈,艺人制作统筹,艺人经纪,艺人经纪公司,艺人统筹,艺术家,艺术指导,艺术指导老师,"
            + "艺统,花脸,英文,营销,萧笛,萨克斯,视觉,记,设计,词,词Lyricist,词Lyrics,词曲作者,词曲提供,词协力,译者,"
            + "语言,语言代码,说唱,说唱词,调校,调音,谱曲,贝斯,贴唱,造型,邮件,邮箱地址,配唱,采样,钢琴,铜管,键盘,"
            + "键盘手,长号,长笛,队长,附加,音乐,音乐人,音准调校,音响,音效,音编,音频,项目企划,项目协力,"
            + "项目总企划,项目总监,项目统筹,项目营销,顾问,领唱,领舞,题字,题记,飓风计划商务合作,马头琴,鸣谢,"
            + "鼓,鼓手,鼓录音,鼓录音室,鼓录制,鼓机,鼓组编程Drums Arrangement,鼓组音频编辑,运营";
    public static final StringSetting LYRICS_CREDIT_LINE_REGEX = new StringSetting("morphe_music_lyrics_credit_line_regex", DEFAULT_LYRICS_CREDIT_LINE_REGEX, true, parent(LYRICS_ENABLED));

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
    public static final BooleanSetting DISABLE_MUSIC_VIDEO_IN_ALBUM = new BooleanSetting("revanced_disable_music_video_in_album", FALSE, true);
    public static final EnumSetting<RedirectType> DISABLE_MUSIC_VIDEO_IN_ALBUM_REDIRECT_TYPE = new EnumSetting<>("revanced_disable_music_video_in_album_redirect_type", RedirectType.REDIRECT, true);
    public static final BooleanSetting EXTERNAL_DOWNLOADER_ACTION_BUTTON = new BooleanSetting("revanced_external_downloader_action", FALSE, true);
    public static final BooleanSetting EXTERNAL_DOWNLOADER_FLYOUT_MENU = new BooleanSetting("revanced_external_downloader_flyout_menu", FALSE, true, parent(EXTERNAL_DOWNLOADER_ACTION_BUTTON));
    public static final StringSetting EXTERNAL_DOWNLOADER_PACKAGE_NAME = new StringSetting("revanced_external_downloader_package_name", "com.deniscerri.ytdl");
    public static final BooleanSetting SETTINGS_IMPORT_EXPORT = new BooleanSetting("revanced_settings_import_export", FALSE, false);
    public static final BooleanSetting SPOOF_VIDEO_STREAMS_SIGN_IN_ANDROID_VR_ABOUT =
            new BooleanSetting("morphe_spoof_video_streams_sign_in_android_vr_about", FALSE, false);
    public static final BooleanSetting APP_INFO = new BooleanSetting("revanced_app_info", FALSE, false);

    // PreferenceScreen: Return YouTube Dislike
    public static final BooleanSetting RYD_ENABLED = new BooleanSetting("revanced_ryd_enabled", TRUE);
    public static final StringSetting RYD_USER_ID = new StringSetting("revanced_ryd_user_id", "");
    public static final BooleanSetting RYD_DISLIKE_PERCENTAGE = new BooleanSetting("revanced_ryd_dislike_percentage", FALSE);
    public static final BooleanSetting RYD_COMPACT_LAYOUT = new BooleanSetting("revanced_ryd_compact_layout", FALSE);
    public static final BooleanSetting RYD_ESTIMATED_LIKE = new BooleanSetting("revanced_ryd_estimated_like", FALSE, true);
    public static final BooleanSetting RYD_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("revanced_ryd_toast_on_connection_error", TRUE);

    // PreferenceScreen: Return YouTube Username
    public static final BooleanSetting RETURN_YOUTUBE_USERNAME_ABOUT = new BooleanSetting("revanced_return_youtube_username_youtube_data_api_v3_about", FALSE, false);


    // PreferenceScreen: SponsorBlock
    public static final BooleanSetting SB_ENABLED = new BooleanSetting("sb_enabled", TRUE);
    public static final BooleanSetting SB_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("sb_toast_on_connection_error", TRUE);
    public static final BooleanSetting SB_TOAST_ON_SKIP = new BooleanSetting("sb_toast_on_skip", TRUE);
    public static final StringSetting SB_API_URL = new StringSetting("sb_api_url", "https://sponsor.ajay.app");
    public static final StringSetting SB_PRIVATE_USER_ID = new StringSetting("sb_private_user_id", "");
    public static final BooleanSetting SB_USER_IS_VIP = new BooleanSetting("sb_user_is_vip", FALSE);

    public static final StringSetting SB_CATEGORY_SPONSOR = new StringSetting("sb_sponsor", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_SPONSOR_COLOR = new StringSetting("sb_sponsor_color", "#FF00D400");
    public static final StringSetting SB_CATEGORY_SELF_PROMO = new StringSetting("sb_selfpromo", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_SELF_PROMO_COLOR = new StringSetting("sb_selfpromo_color", "#FFFFFF00");
    public static final StringSetting SB_CATEGORY_INTERACTION = new StringSetting("sb_interaction", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_INTERACTION_COLOR = new StringSetting("sb_interaction_color", "#FFCC00FF");
    public static final StringSetting SB_CATEGORY_HOOK = new StringSetting("sb_hook", IGNORE.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_HOOK_COLOR = new StringSetting("sb_hook_color", "#FF395699");
    public static final StringSetting SB_CATEGORY_INTRO = new StringSetting("sb_intro", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_INTRO_COLOR = new StringSetting("sb_intro_color", "#FF00FFFF");
    public static final StringSetting SB_CATEGORY_OUTRO = new StringSetting("sb_outro", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_OUTRO_COLOR = new StringSetting("sb_outro_color", "#FF0202ED");
    public static final StringSetting SB_CATEGORY_PREVIEW = new StringSetting("sb_preview", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_PREVIEW_COLOR = new StringSetting("sb_preview_color", "#FF008FD6");
    public static final StringSetting SB_CATEGORY_FILLER = new StringSetting("sb_filler", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_FILLER_COLOR = new StringSetting("sb_filler_color", "#FF7300FF");
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC = new StringSetting("sb_music_offtopic", SKIP_AUTOMATICALLY.reVancedKeyValue);
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC_COLOR = new StringSetting("sb_music_offtopic_color", "#FFFF9900");

    // SB settings not exported
    public static final LongSetting SB_LAST_VIP_CHECK = new LongSetting("sb_last_vip_check", 0L, false, false);

    static {
        // region Migration

        // Old spoof versions that no longer work reliably.
        String spoofAppVersionTarget = SPOOF_APP_VERSION_TARGET.get();
        if (spoofAppVersionTarget.compareTo(SPOOF_APP_VERSION_TARGET.defaultValue) < 0) {
            Utils.showToastShort(str("revanced_spoof_app_version_target_invalid_toast", spoofAppVersionTarget));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            Logger.printInfo(() -> "Resetting spoof app version target");
            SPOOF_APP_VERSION_TARGET.resetToDefault();
        }
        String spoofAppVersionLyricsTarget = SPOOF_APP_VERSION_FOR_LYRICS_TARGET.get();
        if (spoofAppVersionLyricsTarget.compareTo(SPOOF_APP_VERSION_FOR_LYRICS_TARGET.defaultValue) < 0) {
            Utils.showToastShort(str("revanced_spoof_app_version_target_invalid_toast", spoofAppVersionTarget));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            Logger.printInfo(() -> "Resetting spoof app version for lyrics target");
            SPOOF_APP_VERSION_FOR_LYRICS_TARGET.resetToDefault();
        }

        // endregion

        // region SB import/export callbacks

        Setting.addImportExportCallback(SponsorBlockSettings.SB_IMPORT_EXPORT_CALLBACK);

        // endregion

    }

    public static final String OPEN_DEFAULT_APP_SETTINGS = "revanced_default_app_settings";

    /**
     * Array of settings using intent
     */
    private static final String[] intentSettingArray = new String[]{
            APP_INFO.key,
            BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN.key,
            CHANGE_START_PAGE.key,
            CROSSFADE_CURVE.key,
            CROSSFADE_DURATION.key,
            CUSTOM_FILTER_STRINGS.key,
            CUSTOM_PLAYBACK_SPEEDS.key,
            CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY.key,
            CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY.key,
            DISABLE_MUSIC_VIDEO_IN_ALBUM_REDIRECT_TYPE.key,
            ENABLE_CUSTOM_NAVIGATION_BAR_COLOR_VALUE.key,
            EXTERNAL_DOWNLOADER_PACKAGE_NAME.key,
            HIDE_ACCOUNT_MENU_FILTER_STRINGS.key,
            OPEN_DEFAULT_APP_SETTINGS,
            REPLACE_NAVIGATION_BUTTON_ABOUT.key,
            RETURN_YOUTUBE_USERNAME_ABOUT.key,
            RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT.key,
            RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY.key,
            SB_API_URL.key,
            SETTINGS_IMPORT_EXPORT.key,
            SPOOF_APP_VERSION_FOR_LYRICS_TARGET.key,
            SPOOF_APP_VERSION_TARGET.key,
            SPOOF_VIDEO_STREAMS_CLIENT_TYPE.key,
            SPOOF_VIDEO_STREAMS_PLAYER_JS_HASH_VALUE.key,
            SPOOF_VIDEO_STREAMS_SIGN_IN_ANDROID_VR_ABOUT.key,
            WATCH_HISTORY_TYPE.key,
    };

    /**
     * @return whether dataString contains settings that use Intent
     */
    public static boolean includeWithIntent(@NonNull String dataString) {
        return Utils.containsAny(dataString, intentSettingArray);
    }
}
