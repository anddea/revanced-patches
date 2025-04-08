package app.revanced.extension.spotify.misc;

@SuppressWarnings("unused")
public class UrlCleaner {
    /**
     * Removes the "?si=" parameter and anything after it from a URL string.
     *
     * @param url The original URL string.
     * @return The URL string with the "?si=" parameter removed, or the original URL if not found or if input is null.
     */
    public static String removeSiParameter(String url) {
        if (url == null) {
            return null;
        }
        int siIndex = url.indexOf("?si=");
        if (siIndex >= 0) {
            // Found "?si=", return the substring before it
            return url.substring(0, siIndex);
        } else {
            // "?si=" not found, return the original URL
            return url;
        }
    }
}
