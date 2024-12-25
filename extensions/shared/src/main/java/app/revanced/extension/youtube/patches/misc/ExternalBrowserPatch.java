package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ExternalBrowserPatch {

    public static String enableExternalBrowser(final String original) {
        if (!Settings.ENABLE_EXTERNAL_BROWSER.get())
            return original;

        return "";
    }
}
