package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.music.utils.ExtendedUtils.getLayoutParams;
import static app.morphe.extension.shared.utils.ResourceUtils.getStringArray;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;

import app.morphe.extension.music.settings.ActivityHook;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.settings.BaseActivityHook;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Preference fragment for ReVanced settings.
 */
@SuppressWarnings("deprecation")
public class YouTubeMusicPreferenceFragment extends ToolbarPreferenceFragment {
    private static final String IMPORT_EXPORT_SETTINGS_ENTRY_KEY = "revanced_settings_import_export_entries";

    /**
     * The main PreferenceScreen used to display the current set of preferences.
     */
    private PreferenceScreen preferenceScreen;
    private String existingSettings;

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

        String dataString = intent.getDataString();
        if (Settings.SETTINGS_IMPORT_EXPORT.key.equals(dataString)) {
            importExportListDialogBuilder();
            return true;
        }

        return ReVancedPreferenceFragment.handlePreferenceIntent(
                activity, activity, dataString, null);
    }

    protected boolean shouldHandlePreferenceIntent(Intent intent) {
        String dataString = intent.getDataString();
        return dataString != null
                && !dataString.isEmpty()
                && Settings.includeWithIntent(dataString);
    }

    /**
     * Build a ListDialog for Import / Export settings.
     */
    private void importExportListDialogBuilder() {
        try {
            final Activity activity = getActivity();
            final String[] entries = getStringArray(IMPORT_EXPORT_SETTINGS_ENTRY_KEY);

            getDialogBuilder(activity)
                    .setTitle(str("revanced_settings_import_export_title"))
                    .setItems(entries, (dialog, index) -> {
                        switch (index) {
                            case 0 -> {
                                settingExportInProgress = true;
                                exportActivity();
                            }
                            case 1 -> importActivity();
                            case 2 -> importExportEditTextDialogBuilder(activity);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "importExportListDialogBuilder failure", ex);
        }
    }

    /**
     * Build an EditTextDialog for Import / Export settings.
     */
    private void importExportEditTextDialogBuilder(Activity activity) {
        try {
            final EditText textView = new EditText(activity);
            existingSettings = Setting.exportToJson(activity);
            textView.setText(existingSettings);
            textView.setInputType(textView.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8);

            TextInputLayout textInputLayout = new TextInputLayout(activity);
            textInputLayout.setLayoutParams(getLayoutParams());
            textInputLayout.addView(textView);

            FrameLayout container = new FrameLayout(activity);
            container.addView(textInputLayout);

            getDialogBuilder(activity)
                    .setTitle(str("revanced_settings_import_export_title"))
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_settings_import_copy"), (dialog, which) ->
                            Utils.setClipboard(textView.getText().toString(), str("revanced_share_copy_settings_success")))
                    .setPositiveButton(str("revanced_settings_import"), (dialog, which) ->
                            importSettings(activity, textView.getText().toString()))
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "importExportEditTextDialogBuilder failure", ex);
        }
    }

    private void importSettings(Activity activity, String replacementSettings) {
        try {
            if (replacementSettings.equals(existingSettings)) {
                return;
            }
            settingImportInProgress = true;
            final boolean restartNeeded = Setting.importFromJSON(activity, replacementSettings);
            if (restartNeeded) {
                showRestartDialog(activity);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "importSettings failure", ex);
        } finally {
            settingImportInProgress = false;
        }
    }
}
