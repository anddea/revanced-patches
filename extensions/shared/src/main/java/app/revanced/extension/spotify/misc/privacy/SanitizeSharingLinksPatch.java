package app.revanced.extension.spotify.misc.privacy;

import android.net.Uri;
import app.revanced.extension.shared.utils.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {
    /**
     * Injection point.
     */
    public static String sanitizeUrl(String url, String parameters) {
        try {
            Uri uri = Uri.parse(url);
            Uri.Builder builder = uri.buildUpon().clearQuery();

            Set<String> paramsToRemove = new HashSet<>(Arrays.asList(parameters.split(",\\s*")));

            for (String paramName : uri.getQueryParameterNames()) {
                if (!paramsToRemove.contains(paramName)) {
                    for (String value : uri.getQueryParameters(paramName)) {
                        builder.appendQueryParameter(paramName, value);
                    }
                }
            }

            String sanitizedUrl = builder.build().toString();
            Logger.printInfo(() -> "Sanitized url " + url + " to " + sanitizedUrl);
            return sanitizedUrl;
        } catch (Exception ex) {
            Logger.printException(() -> "sanitizeUrl failure with " + url, ex);
            return url;
        }
    }
}
