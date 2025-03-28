package app.revanced.extension.shared.patches;

import android.net.Uri;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class WatchHistoryPatch {

    public enum WatchHistoryType {
        ORIGINAL,
        REPLACE,
        BLOCK
    }

    private static final Uri UNREACHABLE_HOST_URI = Uri.parse("https://127.0.0.0");
    private static final String WWW_TRACKING_URL_AUTHORITY = "www.youtube.com";

    public static Uri replaceTrackingUrl(Uri trackingUrl) {
        final WatchHistoryType watchHistoryType = BaseSettings.WATCH_HISTORY_TYPE.get();
        if (watchHistoryType != WatchHistoryType.ORIGINAL) {
            try {
                if (watchHistoryType == WatchHistoryType.REPLACE) {
                    return trackingUrl.buildUpon().authority(WWW_TRACKING_URL_AUTHORITY).build();
                } else if (watchHistoryType == WatchHistoryType.BLOCK) {
                    return UNREACHABLE_HOST_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "replaceTrackingUrl failure", ex);
            }
        }

        return trackingUrl;
    }

}
