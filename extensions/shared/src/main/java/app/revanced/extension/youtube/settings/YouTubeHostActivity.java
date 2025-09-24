package app.revanced.extension.youtube.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toolbar;

import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.settings.BaseHostActivity;
import app.revanced.extension.youtube.settings.preference.YouTubePreferenceFragment;
import app.revanced.extension.youtube.settings.search.YouTubeSearchViewController;
import app.revanced.extension.youtube.utils.ThemeUtils;

/**
 * Hooks LicenseActivity to inject a custom {@link YouTubePreferenceFragment} with a toolbar and search functionality.
 */
public class YouTubeHostActivity extends BaseHostActivity {

    /**
     * Controller for managing search view components in the toolbar.
     */
    @SuppressLint("StaticFieldLeak")
    public static YouTubeSearchViewController searchViewController;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void finish() {
        if (!handleFinish()) {
            super.finish();
        }
    }

    /**
     * Customizes the activity theme based on dark/light mode.
     */
    @Override
    protected void customizeActivityTheme(Activity activity) {
        activity.setTheme(ThemeUtils.getThemeId());
    }

    /**
     * Returns the resource ID for the YouTube settings layout.
     */
    @Override
    protected int getContentViewResourceId() {
        return LAYOUT_REVANCED_SETTINGS_WITH_TOOLBAR;
    }

    /**
     * Returns the toolbar background color based on dark/light mode.
     */
    @Override
    protected int getToolbarBackgroundColor() {
        return ThemeUtils.getToolbarBackgroundColor();
    }

    /**
     * Returns the navigation icon drawable for the toolbar.
     */
    @Override
    protected Drawable getNavigationIcon() {
        return BaseThemeUtils.getBackButtonDrawable();
    }

    /**
     * Returns the click listener for the navigation icon.
     */
    @Override
    protected View.OnClickListener getNavigationClickListener(Activity activity) {
        return null;
    }

    /**
     * Adds search view components to the toolbar for {@link YouTubePreferenceFragment}.
     *
     * @param activity The activity hosting the toolbar.
     * @param toolbar  The configured toolbar.
     * @param fragment The PreferenceFragment associated with the activity.
     */
    @Override
    protected void onPostToolbarSetup(Activity activity, Toolbar toolbar, PreferenceFragment fragment) {
        if (fragment instanceof YouTubePreferenceFragment preferenceFragment) {
            searchViewController = YouTubeSearchViewController.addSearchViewComponents(
                    activity, toolbar, preferenceFragment);
        }
    }

    /**
     * Creates a new {@link YouTubePreferenceFragment} for the activity.
     */
    @Override
    protected PreferenceFragment createPreferenceFragment() {
        return new YouTubePreferenceFragment();
    }

    /**
     * Injection point.
     * <p>
     * Overrides {@link Activity#finish()} of the injection Activity.
     *
     * @return if the original activity finish method should be allowed to run.
     */
    @Override
    protected boolean handleFinish() {
        return YouTubeSearchViewController.handleFinish(searchViewController);
    }
}
