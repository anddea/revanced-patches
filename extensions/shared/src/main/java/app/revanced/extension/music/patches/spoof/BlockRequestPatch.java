package app.revanced.extension.music.patches.spoof;

import android.net.Uri;

import app.revanced.extension.music.patches.utils.PatchStatus;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class BlockRequestPatch {
    public static final boolean SPOOF_CLIENT =
            Settings.SPOOF_CLIENT.get() && PatchStatus.SpoofClient();
    public static final ClientType CLIENT_TYPE =
            Settings.SPOOF_CLIENT_TYPE.get();
    public static final boolean SPOOF_CLIENT_BLOCK_REQUEST =
            SPOOF_CLIENT && CLIENT_TYPE.blockRequest;
    public static final boolean SPOOF_VIDEO_STREAMS =
            Settings.SPOOF_VIDEO_STREAMS.get() && PatchStatus.SpoofVideoStreams();

    private static final boolean BLOCK_REQUEST =
            SPOOF_CLIENT_BLOCK_REQUEST || SPOOF_VIDEO_STREAMS;

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
    public static String blockInitPlaybackRequest(String originalUrlString) {
        if (BLOCK_REQUEST) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    Logger.printDebug(() -> "Blocking 'initplayback' by clearing query");

                    return originalUri.buildUpon().clearQuery().build().toString();
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }

        return originalUrlString;
    }
}
