package app.morphe.extension.shared.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.morphe.extension.shared.settings.Setting.migrateOldSettingToNew;
import static app.morphe.extension.shared.settings.Setting.parent;

import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch.JavaScriptClientAvailability;
import app.morphe.extension.shared.spoof.js.JavaScriptVariant;
import app.morphe.extension.shared.patches.CustomBrandingPatch;

/**
 * Settings shared by YouTube and YouTube Music.
 * <p>
 * To ensure this class is loaded when the UI is created, app specific setting bundles should extend
 * or reference this class.
 */
public class SharedYouTubeSettings extends BaseSettings {
    public static final StringSetting CUSTOM_BRANDING_ICON = new StringSetting(
            "morphe_custom_branding_icon", CustomBrandingPatch.getDefaultIconStyle(), true);

    public static final IntegerSetting CUSTOM_BRANDING_NAME = new IntegerSetting(
            "morphe_custom_branding_name", CustomBrandingPatch.getDefaultAppNameIndex(), true);

    public static final BooleanSetting CUSTOM_BRANDING_APPLY_TO_RVX_SETTINGS = new BooleanSetting(
            "morphe_custom_branding_apply_to_rvx_settings", FALSE, true);

    public static final BooleanSetting FORCE_ORIGINAL_AUDIO = new BooleanSetting("morphe_force_original_audio", TRUE, true);

    public static final BooleanSetting LIVESTREAM_DVR = new BooleanSetting("morphe_livestream_dvr", FALSE, true);
    public static final BooleanSetting EXPAND_LIVESTREAM_DVR_DURATION = new BooleanSetting("morphe_expand_livestream_dvr_duration", FALSE, true);

    public static final BooleanSetting SPOOF_VIDEO_STREAMS = new BooleanSetting("morphe_spoof_video_streams", TRUE, true, "morphe_spoof_video_streams_user_dialog_message");
    public static final BooleanSetting SPOOF_VIDEO_STREAMS_STATS_FOR_NERDS = new BooleanSetting("morphe_spoof_video_streams_stats_for_nerds", TRUE, parent(SPOOF_VIDEO_STREAMS));
    public static final EnumSetting<JavaScriptVariant> SPOOF_VIDEO_STREAMS_PLAYER_JS_VARIANT = new EnumSetting<>("morphe_spoof_video_streams_player_js_variant", JavaScriptVariant.HOUSE_BRAND, true,
            new JavaScriptClientAvailability());
    public static final StringSetting SPOOF_VIDEO_STREAMS_PLAYER_JS_HASH_VALUE = new StringSetting("morphe_spoof_video_streams_player_js_hash_value", "", true, false);
    public static final LongSetting SPOOF_VIDEO_STREAMS_PLAYER_JS_SAVED_MILLISECONDS = new LongSetting("morphe_spoof_video_streams_player_js_saved_milliseconds", -1L, false, false);
    public static final StringSetting OAUTH2_REFRESH_TOKEN = new StringSetting("morphe_oauth2_refresh_token", "", false, false);
    public static final StringSetting SPOOF_VIDEO_STREAMS_CLIENT_IDS = new StringSetting("morphe_spoof_video_streams_clent_id", "", false, false);

    public static final BooleanSetting SANITIZE_SHARING_LINKS = new BooleanSetting("morphe_sanitize_sharing_links", TRUE);
    public static final BooleanSetting REPLACE_MUSIC_LINKS_WITH_YOUTUBE = new BooleanSetting("morphe_replace_music_with_youtube", FALSE);
    public static final BooleanSetting REPLACE_LINKS_WITH_SHORTENER = new BooleanSetting("morphe_replace_links_with_shortener", FALSE);

    // Renamed settings
    private static final BooleanSetting DEPRECATED_REVANCED_SANITIZE_SHARING_LINKS = BaseSettings.SANITIZE_SHARING_LINKS;
    private static final BooleanSetting DEPRECATED_SANITIZE_URL_QUERY = new BooleanSetting("morphe_sanitize_url_query", TRUE);

    static {
        // TODO: Eventually remove these migrations
        migrateOldSettingToNew(DEPRECATED_REVANCED_SANITIZE_SHARING_LINKS, SANITIZE_SHARING_LINKS);
        migrateOldSettingToNew(DEPRECATED_SANITIZE_URL_QUERY, SANITIZE_SHARING_LINKS);
    }
}
