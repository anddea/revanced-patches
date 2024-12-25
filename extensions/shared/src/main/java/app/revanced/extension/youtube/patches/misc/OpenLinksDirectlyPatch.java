package app.revanced.extension.youtube.patches.misc;

import android.net.Uri;

import java.util.Objects;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksDirectlyPatch {
    private static final String YOUTUBE_REDIRECT_PATH = "/redirect";

    public static Uri enableBypassRedirect(String uri) {
        final Uri parsed = Uri.parse(uri);
        if (!Settings.ENABLE_OPEN_LINKS_DIRECTLY.get())
            return parsed;

        if (Objects.equals(parsed.getPath(), YOUTUBE_REDIRECT_PATH)) {
            return Uri.parse(Uri.decode(parsed.getQueryParameter("q")));
        }

        return parsed;
    }
}
