package app.revanced.extension.youtube.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toolbar;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.preference.ReVancedPreferenceFragment;
import app.revanced.extension.youtube.utils.ThemeUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import static app.revanced.extension.shared.utils.StringRef.str;

@SuppressWarnings({"unused", "deprecation"})
public class LicenseActivityHook {

    public static String rvxSettingsLabel;
    public static String searchLabel;
    public static WeakReference<SearchView> searchViewRef = new WeakReference<>(null);
    public static WeakReference<ReVancedPreferenceFragment> fragmentRef = new WeakReference<>(null);
    @SuppressLint("StaticFieldLeak")
    private static Toolbar mainActivityToolbar;
    private static ViewGroup.LayoutParams toolbarLayoutParams;

    /**
     * Initializes the LicenseActivity with the ReVanced settings UI, including theme, layout, toolbar, and search view.
     * <p>
     * This method sets up the activity by applying the appropriate theme, setting the layout, initializing labels,
     * configuring the intent, adding the {@link ReVancedPreferenceFragment}, and setting up the toolbar and search view.
     * Any errors during initialization are logged using {@link Logger}.
     *
     * @param licenseActivity The {@link Activity} to initialize with the ReVanced settings UI.
     */
    public static void initialize(Activity licenseActivity) {
        try {
            // Set theme for consistent styling
            licenseActivity.setTheme(ThemeUtils.getThemeId());

            // Set the correct layout
            licenseActivity.setContentView(ResourceUtils.getLayoutIdentifier("revanced_settings_with_toolbar"));

            // Set labels
            rvxSettingsLabel = str("revanced_extended_settings_title");
            searchLabel = str("revanced_extended_settings_search_title");

            // Ensure intent data is set correctly if launched via intent
            Intent intent = licenseActivity.getIntent();
            if (intent != null && !"revanced_extended_settings_intent".equals(intent.getDataString())) {
                intent.setData(Uri.parse("revanced_extended_settings_intent"));
            }

            // Initialize and add the fragment
            ReVancedPreferenceFragment fragment = new ReVancedPreferenceFragment();
            fragmentRef = new WeakReference<>(fragment);
            licenseActivity.getFragmentManager()
                    .beginTransaction()
                    .replace(ResourceUtils.getIdIdentifier("revanced_settings_fragments"), fragment)
                    .commitNow();

            // Set up toolbar
            createToolbar(licenseActivity, fragment);

            // Set up search view
            setSearchView(licenseActivity, fragment);
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

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
     * Creates and configures the toolbar for the LicenseActivity.
     * <p>
     * This method sets up the toolbar by removing any existing dummy toolbar, applying the theme, setting navigation
     * behavior, and configuring the title and margins. The toolbar's back button triggers the {@link ReVancedPreferenceFragment#handleOnBackPressed(String)}
     * method to handle navigation. If the toolbar parent or layout resources are not found, an error is logged.
     *
     * @param activity The {@link Activity} where the toolbar is created.
     * @param fragment The {@link ReVancedPreferenceFragment} used for handling back navigation.
     */
    private static void createToolbar(Activity activity, ReVancedPreferenceFragment fragment) {
        ViewGroup toolBarParent = activity.findViewById(ResourceUtils.getIdIdentifier("revanced_toolbar_parent"));
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
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, activity.getResources().getDisplayMetrics())
            );
        }

        Toolbar toolbar = new Toolbar(activity);
        toolbar.setId(ResourceUtils.getIdIdentifier("revanced_toolbar"));
        mainActivityToolbar = toolbar;

        toolbar.setBackgroundColor(ThemeUtils.getToolbarBackgroundColor());
        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
        toolbar.setNavigationOnClickListener(view -> {
            SearchView searchView = searchViewRef.get();
            String currentQuery = (searchView != null) ? searchView.getQuery().toString() : "";

            boolean shouldCallActivityBackPressed = fragment.handleOnBackPressed(currentQuery);
            if (shouldCallActivityBackPressed) activity.onBackPressed();
        });

        toolbar.setTitle(rvxSettingsLabel);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
        toolbar.setTitleMarginStart(margin);
        toolbar.setTitleMarginEnd(margin);

        TextView toolbarTextView = Utils.getChildView(toolbar, view -> view instanceof TextView);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(ThemeUtils.getAppForegroundColor());
        }

        toolbar.setLayoutParams(toolbarLayoutParams);
        toolBarParent.addView(toolbar, 0);
    }

    /**
     * Configures the SearchView for the LicenseActivity.
     * <p>
     * This method sets up the {@link SearchView} by applying a query hint, adjusting font size via reflection,
     * setting margins, applying a themed background, and configuring query text and focus listeners. The listeners
     * handle search queries, filter preferences, and manage search history. The keyboard is hidden after query submission.
     * If the SearchView or required resources are not found, an error is logged.
     *
     * @param activity The {@link Activity} containing the SearchView.
     * @param fragment The {@link ReVancedPreferenceFragment} used for filtering preferences and managing search history.
     */
    private static void setSearchView(Activity activity, ReVancedPreferenceFragment fragment) {
        SearchView searchView = activity.findViewById(ResourceUtils.getIdIdentifier("search_view"));
        if (searchView == null) {
            Logger.printException(() -> "SearchView not found in layout");
            return;
        }

        // Set query hint
        String finalSearchHint = String.format(searchLabel, rvxSettingsLabel);
        searchView.setQueryHint(finalSearchHint);

        // Set font size via reflection
        try {
            Field field = searchView.getClass().getDeclaredField("mSearchSrcTextView");
            field.setAccessible(true);
            if (field.get(searchView) instanceof EditText searchEditText) {
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.printException(() -> "Reflection error accessing mSearchSrcTextView", ex);
        }

        // Set SearchView dimensions
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) searchView.getLayoutParams();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, activity.getResources().getDisplayMetrics());
        layoutParams.setMargins(margin, layoutParams.topMargin, margin, layoutParams.bottomMargin);
        searchView.setLayoutParams(layoutParams);

        // Set SearchView color
        searchView.setBackground(ThemeUtils.getSearchViewShape());

        // Set query text listener
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Logger.printDebug(() -> "onQueryTextSubmit called with: " + query);
                String queryTrimmed = query.trim();
                if (!queryTrimmed.isEmpty()) {
                    fragment.setPreferenceScreen(fragment.rootPreferenceScreen);
                    fragment.filterPreferences(queryTrimmed);
                    fragment.addToSearchHistory(queryTrimmed);
                    Logger.printDebug(() -> "Added to search history: " + queryTrimmed);
                }

                // Hide keyboard and remove focus
                searchView.clearFocus();
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                fragment.setPreferenceScreen(fragment.rootPreferenceScreen);
                fragment.filterPreferences(newText);
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            Logger.printDebug(() -> "SearchView focus changed: " + hasFocus);
            if (hasFocus) {
                fragment.filterPreferences(""); // Show history when focused
            }
        });

        // Store SearchView reference
        searchViewRef = new WeakReference<>(searchView);
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
     * @return A localized {@link Context} based on the original context.
     */
    public static Context getAttachBaseContext(Context original) {
        return Utils.getLocalizedContext(original);
    }
}
