/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.shared.utils.ResourceUtils.getStringArray;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toolbar;

import app.morphe.extension.music.settings.ActivityHook;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.settings.BaseActivityHook;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.ToolbarPreferenceFragment;
import app.morphe.extension.shared.ui.CustomDialog;
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

            Pair<Dialog, LinearLayout> dialog = CustomDialog.create(
                    activity,
                    str("revanced_settings_import_export_title"),
                    null,
                    textView,
                    str("revanced_settings_import"),
                    () -> importSettings(activity, textView.getText().toString()),
                    () -> {
                    },
                    str("revanced_settings_import_copy"),
                    () -> Utils.setClipboard(textView.getText().toString(),
                            str("revanced_share_copy_settings_success")),
                    true
            );
            dialog.first.show();
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
