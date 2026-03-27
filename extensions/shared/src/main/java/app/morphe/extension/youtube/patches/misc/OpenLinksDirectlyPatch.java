package app.morphe.extension.youtube.patches.misc;

import android.net.Uri;

import java.util.Objects;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksDirectlyPatch {
    private static final String YOUTUBE_REDIRECT_PATH = "/redirect";

    public static Uri parseRedirectUri(String uri) {
        final Uri parsed = Uri.parse(uri);
        if (!Settings.BYPASS_URL_REDIRECTS.get())
            return parsed;

        if (Objects.equals(parsed.getPath(), YOUTUBE_REDIRECT_PATH)) {
            return Uri.parse(Uri.decode(parsed.getQueryParameter("q")));
        }

        return parsed;
    }
}
