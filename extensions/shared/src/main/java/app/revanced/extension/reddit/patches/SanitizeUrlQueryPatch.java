package app.revanced.extension.reddit.patches;

import app.revanced.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class SanitizeUrlQueryPatch {

    public static boolean stripQueryParameters() {
        return Settings.SANITIZE_URL_QUERY.get();
    }

}
