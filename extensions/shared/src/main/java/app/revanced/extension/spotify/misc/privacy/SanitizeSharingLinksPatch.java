package app.revanced.extension.spotify.misc.privacy;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {

    private static final List<String> PARAMETERS_TO_REMOVE = Arrays.asList("context", "pi", "si", "utm_source");

    /**
     * Removes specified tracking parameters and anything after them from a URL string.
     *
     * @param url The original URL string.
     * @return The URL string with the tracking parameters removed, or the original URL if none found or if input is null.
     */
    public static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        for (String param : PARAMETERS_TO_REMOVE) {
            // Consider everything after "?" as a tracking parameter,
            // since nothing is actually needed after the artist/album/song id
            // to work properly.
            // It helps us avoid unknown tracking parameters left in the URL.
            String paramPattern = "?" + param + "=";
            int paramIndex = url.indexOf(paramPattern);
            if (paramIndex >= 0) {
                // Found the parameter, return the substring before it
                return url.substring(0, paramIndex);
            }
        }
        // No parameters found, return the original URL
        return url;
    }
}
