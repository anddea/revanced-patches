package app.morphe.extension.shared.patches;

import android.content.Intent;

@SuppressWarnings("all")
public final class SanitizeUrlQueryPatch {
    private static final String URL_PROTOCOL = "http";

    @Deprecated
    public static String stripQueryParameters(final String urlString) {
        return SanitizeSharingLinksPatch.sanitize(urlString);
    }

    @Deprecated
    public static void stripQueryParameters(final Intent intent, final String extraName, final String extraValue) {
        intent.putExtra(extraName, extraValue.startsWith(URL_PROTOCOL)
                ? stripQueryParameters(extraValue)
                : extraValue
        );
    }
}
