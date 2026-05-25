package app.morphe.extension.music.settings.preference;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Toolbar;

import app.morphe.extension.music.settings.ActivityHook;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.settings.BaseActivityHook;
import app.morphe.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Preference fragment for ReVanced settings.
 */
@SuppressWarnings("deprecation")
public class YouTubeMusicPreferenceFragment extends ToolbarPreferenceFragment {
    /**
     * The main PreferenceScreen used to display the current set of preferences.
     */
    private PreferenceScreen preferenceScreen;

    /**
     * Initializes the preference fragment.
     */
    @Override
    protected void initialize() {
        super.initialize();

        try {
            preferenceScreen = getPreferenceScreen();
            Utils.sortPreferenceGroups(preferenceScreen);
            setPreferenceScreenToolbar(preferenceScreen);
            installPreferenceIntentHandlers(preferenceScreen);
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Called when the fragment starts.
     */
    @Override
    public void onStart() {
        super.onStart();
        try {
            // Initialize search controller if needed
            if (ActivityHook.searchViewController != null) {
                // Trigger search data collection after fragment is ready.
                ActivityHook.searchViewController.initializeSearchData();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onStart failure", ex);
        }
    }

    /**
     * Sets toolbar for all nested preference screens.
     */
    @Override
    protected void customizeToolbar(Toolbar toolbar) {
        BaseActivityHook.setToolbarLayoutParams(toolbar);
    }

    /**
     * Perform actions after toolbar setup.
     */
    @Override
    protected void onPostToolbarSetup(Toolbar toolbar, Dialog preferenceScreenDialog) {
        if (ActivityHook.searchViewController != null
                && ActivityHook.searchViewController.isSearchActive()) {
            toolbar.post(() -> ActivityHook.searchViewController.closeSearch());
        }
    }

    /**
     * Returns the preference screen for external access by SearchViewController.
     */
    public PreferenceScreen getPreferenceScreenForSearch() {
        return preferenceScreen;
    }

    protected void installPreferenceIntentHandlers(PreferenceScreen parentScreen) {
        for (int i = 0, count = parentScreen.getPreferenceCount(); i < count; i++) {
            Preference childPreference = parentScreen.getPreference(i);
            if (childPreference instanceof PreferenceScreen screen) {
                installPreferenceIntentHandlers(screen);
                continue;
            }

            Intent intent = childPreference.getIntent();
            if (intent == null || !shouldHandlePreferenceIntent(intent)) {
                continue;
            }

            childPreference.setOnPreferenceClickListener(
                    preference -> handlePreferenceIntent(preference.getIntent()));
        }
    }

    protected boolean handlePreferenceIntent(Intent intent) {
        Activity activity = getActivity();
        if (activity == null || intent == null) {
            return false;
        }

        return ReVancedPreferenceFragment.handlePreferenceIntent(
                activity, activity, intent.getDataString(), null);
    }

    protected boolean shouldHandlePreferenceIntent(Intent intent) {
        String dataString = intent.getDataString();
        return dataString != null
                && !dataString.isEmpty()
                && Settings.includeWithIntent(dataString);
    }
}
