package app.morphe.extension.music.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toolbar;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import app.morphe.extension.music.settings.preference.ReVancedPreferenceFragment;
import app.morphe.extension.music.settings.preference.YouTubeMusicPreferenceFragment;
import app.morphe.extension.music.settings.search.YouTubeMusicSearchViewController;
import app.morphe.extension.shared.settings.BaseActivityHook;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;

/**
 * @noinspection ALL
 */
public class ActivityHook {
    private static final String REVANCED_SETTINGS_INTENT = "revanced_settings_intent";

    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    @SuppressLint("StaticFieldLeak")
    public static YouTubeMusicSearchViewController searchViewController;

    public static Activity getActivity() {
        return activityRef.get();
    }

    public static boolean isSearchableSettingsIntent(String dataString) {
        return REVANCED_SETTINGS_INTENT.equals(dataString);
    }

    public static boolean handleFinish() {
        return searchViewController != null && YouTubeMusicSearchViewController.handleFinish(searchViewController);
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

            String dataString = baseActivityIntent.getDataString();
            if (isSearchableSettingsIntent(dataString)) {
                BaseActivityHook.initialize(new SearchableSettingsActivityHook(), baseActivity);
                return true;
            }

            // If we do not finish the activity immediately, the YT Music logo will remain on the screen.
            baseActivity.finish();

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

    private static class SearchableSettingsActivityHook extends BaseActivityHook {
        @Override
        protected void customizeActivityTheme(Activity activity) {
            activity.setTheme(ResourceUtils.getStyleIdentifier("Theme.ReVanced.YouTubeMusic.Settings"));
            activity.getWindow().getDecorView().setBackgroundColor(getSettingsBackgroundColor());
        }

        @Override
        protected int getContentViewResourceId() {
            return LAYOUT_REVANCED_SETTINGS_WITH_TOOLBAR;
        }

        @Override
        protected int getToolbarBackgroundColor() {
            return getSettingsBackgroundColor();
        }

        @Override
        protected Drawable getNavigationIcon() {
            return BaseThemeUtils.getBackButtonDrawable();
        }

        @Override
        protected View.OnClickListener getNavigationClickListener(Activity activity) {
            return view -> {
                if (searchViewController != null && searchViewController.isSearchActive()) {
                    searchViewController.closeSearch();
                } else {
                    activity.finish();
                }
            };
        }

        @Override
        protected void onPostToolbarSetup(Activity activity, Toolbar toolbar, PreferenceFragment fragment) {
            if (fragment instanceof YouTubeMusicPreferenceFragment preferenceFragment) {
                searchViewController = YouTubeMusicSearchViewController.addSearchViewComponents(
                        activity, toolbar, preferenceFragment);
            }
        }

        @Override
        protected PreferenceFragment createPreferenceFragment() {
            return new YouTubeMusicPreferenceFragment();
        }

        private int getSettingsBackgroundColor() {
            return ResourceUtils.getColor("yt_black_pure");
        }
    }

}
