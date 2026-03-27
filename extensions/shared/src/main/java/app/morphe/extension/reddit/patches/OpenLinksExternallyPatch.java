package app.morphe.extension.reddit.patches;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import app.morphe.extension.reddit.settings.Settings;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class OpenLinksExternallyPatch {

    /**
     * Override 'CustomTabsIntent', in order to open links in the default browser.
     * Instead of doing CustomTabsActivity,
     *
     * @param activity The activity, to start an Intent.
     * @param uri      The URL to be opened in the default browser.
     */
    public static boolean openLinksExternally(Activity activity, Uri uri) {
        try {
            if (activity != null && uri != null && Settings.OPEN_LINKS_EXTERNALLY.get()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                activity.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Logger.printException(() -> "Can not open URL: " + uri, e);
        }
        return false;
    }
}
