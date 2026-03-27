package app.morphe.extension.music.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import app.morphe.extension.music.settings.preference.ReVancedPreferenceFragment;
import app.morphe.extension.shared.utils.Logger;

/**
 * @noinspection ALL
 */
public class ActivityHook {
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    public static Activity getActivity() {
        return activityRef.get();
    }

    /**
     * Injection point.
     *
     * @param object object is usually Activity, but sometimes object cannot be cast to Activity.
     *               Check whether object can be cast as Activity for a safe hook.
     */
    public static void setActivity(@NonNull Object object) {
        if (object instanceof Activity mActivity) {
            activityRef = new WeakReference<>(mActivity);
        }
    }

    /**
     * Injection point.
     *
     * @param baseActivity Activity containing intent data.
     *                     It should be finished immediately after obtaining the dataString.
     * @return Whether or not dataString is included.
     */
    public static boolean initialize(@NonNull Activity baseActivity) {
        try {
            final Intent baseActivityIntent = baseActivity.getIntent();
            if (baseActivityIntent == null)
                return false;

            // If we do not finish the activity immediately, the YT Music logo will remain on the screen.
            baseActivity.finish();

            String dataString = baseActivityIntent.getDataString();
            if (dataString == null || dataString.isEmpty())
                return false;

            // Checks whether dataString contains settings that use Intent.
            if (!Settings.includeWithIntent(dataString))
                return false;


            // Save intent data in settings activity.
            Activity mActivity = activityRef.get();
            Intent intent = mActivity.getIntent();
            intent.setData(Uri.parse(dataString));
            mActivity.setIntent(intent);

            // Starts a new PreferenceFragment to handle activities freely.
            mActivity.getFragmentManager()
                    .beginTransaction()
                    .add(new ReVancedPreferenceFragment(), "")
                    .commit();

            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "initializeSettings failure", ex);
        }
        return false;
    }

}
