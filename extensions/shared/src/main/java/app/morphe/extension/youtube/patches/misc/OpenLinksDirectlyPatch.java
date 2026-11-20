package app.morphe.extension.youtube.patches.misc;

import android.net.Uri;

import java.util.Objects;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksDirectlyPatch {
    /**
     * Convert the YouTube redirect URI string to the redirect query URI.
     *
     * @param uri The YouTube redirect URI string.
     * @return The redirect query URI.
     */
    public static Uri parseRedirectUri(String uri) {
        final var parsed = Uri.parse(uri);

        if (Settings.BYPASS_URL_REDIRECTS.get() && Objects.equals(parsed.getPath(), "/redirect")) {
            String queryParam = parsed.getQueryParameter("q");
            if (queryParam != null) {
                if (Uri.parse(queryParam).getScheme() == null) {
                    // Channel links can omit the scheme (q=twitch.tv/name) and no app opens them.
                    queryParam = "http://" + queryParam;
                }
                var query = Uri.parse(queryParam);
                Logger.printDebug(() -> "Bypassing YouTube redirect URI: " + query);
                return query;
            }
        }
        return parsed;
    }
}
