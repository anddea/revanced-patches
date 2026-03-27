package app.morphe.extension.shared.patches;

import android.content.Intent;

import app.morphe.extension.shared.settings.BaseSettings;

@SuppressWarnings("all")
public final class SanitizeUrlQueryPatch {
    /**
     * This tracking parameter is mainly used.
     */
    private static final String NEW_TRACKING_REGEX = ".si=.+";
    /**
     * This tracking parameter is outdated.
     * Used when patching old versions or enabling spoof app version.
     */
    private static final String OLD_TRACKING_REGEX = ".feature=.+";
    private static final String URL_PROTOCOL = "http";

    /**
     * Strip query parameters from a given URL string.
     * <p>
     * URL example containing tracking parameter:
     * https://youtu.be/ZWgr7qP6yhY?si=kKA_-9cygieuFY7R
     * https://youtu.be/ZWgr7qP6yhY?feature=shared
     * https://youtube.com/watch?v=ZWgr7qP6yhY&si=s_PZAxnJHKX1Mc8C
     * https://youtube.com/watch?v=ZWgr7qP6yhY&feature=shared
     * https://youtube.com/playlist?list=PLBsP89CPrMeO7uztAu6YxSB10cRMpjgiY&si=N0U8xncY2ZmQoSMp
     * https://youtube.com/playlist?list=PLBsP89CPrMeO7uztAu6YxSB10cRMpjgiY&feature=shared
     * <p>
     * Since we need to support support all these examples,
     * We cannot use [URL.getpath()] or [Uri.getQueryParameter()].
     *
     * @param urlString URL string to strip query parameters from.
     * @return URL string without query parameters if possible, otherwise the original string.
     */
    public static String stripQueryParameters(final String urlString) {
        if (!BaseSettings.SANITIZE_SHARING_LINKS.get())
            return urlString;

        return urlString.replaceAll(NEW_TRACKING_REGEX, "").replaceAll(OLD_TRACKING_REGEX, "");
    }

    public static void stripQueryParameters(final Intent intent, final String extraName, final String extraValue) {
        intent.putExtra(extraName, extraValue.startsWith(URL_PROTOCOL)
                ? stripQueryParameters(extraValue)
                : extraValue
        );
    }
}