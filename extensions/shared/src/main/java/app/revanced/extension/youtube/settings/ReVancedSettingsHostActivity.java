package app.revanced.extension.youtube.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.preference.ReVancedPreferenceFragment;
import app.revanced.extension.youtube.utils.ThemeUtils;

import static app.revanced.extension.youtube.utils.ThemeUtils.setNavigationBarColor;

@SuppressWarnings({"unused", "deprecation"})
public final class ReVancedSettingsHostActivity extends Activity {
    private static ViewGroup.LayoutParams toolbarLayoutParams;
    private boolean isInitialized = false;

    @SuppressLint("StaticFieldLeak")
    private static Toolbar mainActivityToolbar;
    private ReVancedPreferenceFragment fragment;
    @SuppressLint("StaticFieldLeak")
    private SearchViewController searchViewController;

    /**
     * Applies the standard toolbar layout parameters to a given toolbar.
     * This ensures consistency for toolbars created dynamically in sub-screen dialogs.
     * @param toolbar The toolbar to configure.
     */
    public static void setToolbarLayoutParams(Toolbar toolbar) {
        if (toolbarLayoutParams != null) {
            toolbar.setLayoutParams(toolbarLayoutParams);
        } else {
            Logger.printException(() -> "toolbarLayoutParams is null. It was not captured during initial toolbar creation.");
        }
    }

    /**
     * Updates the title of the main toolbar.
     * <p>
     * This method sets the provided title on the main toolbar if it is initialized. If the toolbar is null,
     * a debug message is logged using {@link Logger}.
     *
     * @param title The new title to set on the toolbar.
     */
    public static void updateToolbarTitle(String title) {
        if (mainActivityToolbar != null) {
            mainActivityToolbar.setTitle(title);
        } else {
            Logger.printDebug(() -> "mainActivityToolbar is null, cannot update title.");
        }
    }

    /**
     * Returns a localized context for the given original context.
     * <p>
     * This method wraps the provided context with a localized context using {@link Utils#getLocalizedContext(Context)}.
     * It is used to ensure proper localization for the activity.
     *
     * @param original The original {@link Context} to be localized.
     */
    @Override
    protected void attachBaseContext(Context original) {
        super.attachBaseContext(Utils.getLocalizedContext(original));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            // Check sanity first
            String dataString = getIntent().getDataString();
            if (!"revanced_extended_settings_intent".equals(dataString)) {
                // User did not open RVX Settings
                // (For example, the user opened the Open source licenses menu)
                isInitialized = false;
                Logger.printDebug(() -> "onCreate ignored");
                return;
            }

            // Set fragment theme
            setTheme(ThemeUtils.getThemeId());

            // Set Navigation bar color
            setNavigationBarColor(getWindow());

            // Set content
            setContentView(ResourceUtils.getLayoutIdentifier("revanced_settings_with_toolbar"));

            // Initialize and add the fragment
            fragment = new ReVancedPreferenceFragment();
            getFragmentManager()
                    .beginTransaction()
                    .replace(ResourceUtils.getIdIdentifier("revanced_settings_fragments"), fragment)
                    .commit();

            // We need to execute the transaction immediately so the fragment is available
            // for the toolbar and search view controller.
            getFragmentManager().executePendingTransactions();


            // Set up toolbar and search view
            createToolbar();
            searchViewController = new SearchViewController(this, fragment);

            isInitialized = true;
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate failure", ex);
        }
    }

    /**
     * Creates and configures the toolbar for the settings activity.
     */
    private void createToolbar() {
        ViewGroup toolBarParent = findViewById(ResourceUtils.getIdIdentifier("revanced_toolbar_parent"));
        if (toolBarParent == null) {
            Logger.printException(() -> "Toolbar parent not found in layout");
            return;
        }

        ViewGroup dummyToolbar = toolBarParent.findViewById(ResourceUtils.getIdIdentifier("revanced_toolbar"));
        if (dummyToolbar != null) {
            toolbarLayoutParams = dummyToolbar.getLayoutParams();
            toolBarParent.removeView(dummyToolbar);
        } else {
            toolbarLayoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics())
            );
        }

        Toolbar toolbar = new Toolbar(this);
        toolbar.setId(ResourceUtils.getIdIdentifier("revanced_toolbar"));
        mainActivityToolbar = toolbar;

        toolbar.setBackgroundColor(ThemeUtils.getToolbarBackgroundColor());
        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        toolbar.setTitle(fragment.rvxSettingsLabel);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        toolbar.setTitleMarginStart(margin);
        toolbar.setTitleMarginEnd(margin);

        TextView toolbarTextView = Utils.getChildView(toolbar, view -> view instanceof TextView);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(ThemeUtils.getAppForegroundColor());
        }

        toolbar.setLayoutParams(toolbarLayoutParams);
        toolBarParent.addView(toolbar, 0);
    }

    @Override
    public void onBackPressed() {
        if (fragment != null) {
            String currentQuery = (searchViewController != null) ? searchViewController.getCurrentQuery() : "";
            boolean shouldExit = fragment.handleOnBackPressed(currentQuery);
            if (shouldExit) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        isInitialized = false;

        if (searchViewController != null) {
            searchViewController.destroy();
        }

        Utils.resetLocalizedContext();
        super.onDestroy();
    }

    /**
     * Injection point.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
