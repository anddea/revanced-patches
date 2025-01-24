package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpenLinksExternallyPatch {

    // renamed from 'enableExternalBrowser'
    public static String openLinksExternally(final String original) {
        if (!Settings.OPEN_LINKS_EXTERNALLY.get())
            return original;

        return "";
    }
}
