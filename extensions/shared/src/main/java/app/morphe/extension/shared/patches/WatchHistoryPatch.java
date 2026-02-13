package app.morphe.extension.shared.patches;

import android.net.Uri;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class WatchHistoryPatch {

    public enum WatchHistoryType {
        ORIGINAL,
        REPLACE,
        BLOCK
    }

    private static final Uri INTERNET_CONNECTION_CHECK_URI = Uri.parse("https://www.youtube.com/gen_204");
    private static final String WWW_TRACKING_URL_AUTHORITY = "www.youtube.com";

    public static Uri replaceTrackingUrl(Uri trackingUrl) {
        final WatchHistoryType watchHistoryType = BaseSettings.WATCH_HISTORY_TYPE.get();
        if (watchHistoryType != WatchHistoryType.ORIGINAL) {
            try {
                if (watchHistoryType == WatchHistoryType.REPLACE) {
                    return trackingUrl.buildUpon().authority(WWW_TRACKING_URL_AUTHORITY).build();
                } else if (watchHistoryType == WatchHistoryType.BLOCK) {
                    return INTERNET_CONNECTION_CHECK_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "replaceTrackingUrl failure", ex);
            }
        }

        return trackingUrl;
    }

}
