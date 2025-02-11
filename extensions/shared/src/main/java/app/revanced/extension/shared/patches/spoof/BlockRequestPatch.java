package app.revanced.extension.shared.patches.spoof;

import android.net.Uri;

import app.revanced.extension.shared.patches.PatchStatus;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.PackageUtils;

@SuppressWarnings("unused")
public class BlockRequestPatch {
    /**
     * Used in YouTube.
     */
    public static final boolean SPOOF_STREAMING_DATA =
            BaseSettings.SPOOF_STREAMING_DATA.get() && PatchStatus.SpoofStreamingData();

    /**
     * Used in YouTube Music.
     */
    public static final boolean SPOOF_CLIENT =
            BaseSettings.SPOOF_CLIENT.get() && PatchStatus.SpoofClient();

    /**
     * In order to load the action bar normally,
     * Some versions must block the initplayback request.
     */
    private static final boolean IS_7_17_OR_GREATER =
            PackageUtils.getAppVersionName().compareTo("7.17.00") >= 0;

    private static final boolean IS_YOUTUBE_BUTTON =
            BaseSettings.SPOOF_CLIENT_TYPE.get().isYouTubeButton();

    private static final boolean BLOCK_REQUEST;

    static {
        if (SPOOF_STREAMING_DATA) {
            BLOCK_REQUEST = true;
        } else {
            if (!SPOOF_CLIENT) {
                BLOCK_REQUEST = false;
            } else {
                if (!IS_7_17_OR_GREATER && !IS_YOUTUBE_BUTTON) {
                    // If the current version is lower than 7.16 and the client action button type is not YouTubeButton,
                    // the initplayback request must be blocked.
                    BLOCK_REQUEST = true;
                } else {
                    // If the current version is higher than 7.17,
                    // the initplayback request must always be blocked.
                    BLOCK_REQUEST = IS_7_17_OR_GREATER;
                }
            }
        }
    }

    /**
     * Any unreachable ip address.  Used to intentionally fail requests.
     */
    private static final String UNREACHABLE_HOST_URI_STRING = "https://127.0.0.0";
    private static final Uri UNREACHABLE_HOST_URI = Uri.parse(UNREACHABLE_HOST_URI_STRING);

    /**
     * Injection point.
     * Blocks /get_watch requests by returning an unreachable URI.
     *
     * @param playerRequestUri The URI of the player request.
     * @return An unreachable URI if the request is a /get_watch request, otherwise the original URI.
     */
    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (BLOCK_REQUEST) {
            try {
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    Logger.printDebug(() -> "Blocking 'get_watch' by returning unreachable uri");

                    return UNREACHABLE_HOST_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }

        return playerRequestUri;
    }

    /**
     * Injection point.
     * <p>
     * Blocks /initplayback requests.
     */
    public static Uri blockInitPlaybackRequest(Uri initPlaybackRequestUri) {
        if (BLOCK_REQUEST) {
            try {
                String path = initPlaybackRequestUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    Logger.printDebug(() -> "Blocking 'initplayback' by clearing query");

                    return initPlaybackRequestUri.buildUpon().clearQuery().build();
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }

        return initPlaybackRequestUri;
    }
}
