/*
 * Copyright (C) 2022-2026 anddea
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

/*
 * Portions of this file are adapted from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.settings.preference;

import static app.morphe.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Pair;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public abstract class AbstractPreferenceFragment extends PreferenceFragment {

    public static boolean settingExportInProgress;

    /**
     * Indicates that if a preference changes,
     * to apply the change from the Setting to the UI component.
     */
    public static boolean settingImportInProgress;

    /**
     * Prevents recursive calls during preference <-> UI syncing from showing extra dialogs.
     */
    private static boolean updatingPreference;

    /**
     * Prevents a slider's per-step SharedPreferences commits from showing a restart dialog.
     * SliderPreference shows the dialog once when the touch interaction ends.
     */
    private static boolean sliderInteractionInProgress;

    /**
     * Used to prevent showing reboot dialog.
     */
    private static boolean showingRestartDialog;

    /**
     * Used to prevent showing reboot dialog, if user cancels a setting user dialog.
     */
    private boolean showingUserDialogMessage;

    /**
     * Confirm and restart dialog button text and title.
     * Set by subclasses if Strings cannot be added as a resource.
     */
    @Nullable
    protected static String restartDialogButtonText, restartDialogTitle, confirmDialogTitle, restartDialogMessage;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (updatingPreference) {
                Logger.printDebug(() -> "Ignoring preference change as sync is in progress");
                return;
            }
            if (str == null) {
                return;
            }
            if (sliderInteractionInProgress) {
                Logger.printDebug(() -> "Ignoring preference change while slider is being moved");
                return;
            }
            Setting<?> setting = Setting.getSettingFromPath(str);
            if (setting == null) {
                return;
            }
            Preference pref = findPreference(str);
            if (pref == null) {
                return;
            }
            Logger.printDebug(() -> "Preference changed: " + setting.key);

            if (!settingImportInProgress && !showingUserDialogMessage) {
                if (setting.userDialogMessage != null && !prefIsSetToDefault(pref, setting)) {
                    // Do not change the setting yet, to allow preserving whatever
                    // list/text value was previously set if it needs to be reverted.
                    showSettingUserDialogConfirmation(pref, setting);
                    return;
                } else if (setting.rebootApp) {
                    showRestartDialog(getActivity());
                }
            }

            updatingPreference = true;
            if (settingImportInProgress) {
                // Apply 'Setting -> Preference'.
                updatePreferencesWithKey(getPreferenceScreen(), str, setting);
            } else {
                // Apply 'Setting <- SharedPreferences -> Preference'. Reading from SharedPreferences
                // avoids stale in-memory values when the same key appears in multiple Preferences.
                Setting.privateSyncValueFromPreferences(setting);
                updatePreferencesWithKey(getPreferenceScreen(), str, setting);
            }
            // Update any other preference availability that may now be different.
            updateUIAvailability();
            if (BaseSettings.SHOW_SLIDER_SUMMARIES.key.equals(str)) {
                refreshSliderSummaries(getPreferenceScreen());
            }
            updatingPreference = false;
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    static void setSliderInteractionInProgress(boolean inProgress) {
        sliderInteractionInProgress = inProgress;
    }

    /**
     * Initialize this instance, and do any custom behavior.
     * <p>
     * To ensure all {@link Setting} instances are correctly synced to the UI,
     * it is important that subclasses make a call or otherwise reference their Settings class bundle
     * so all app specific {@link Setting} instances are loaded before this method returns.
     */
    protected void initialize() {
        final int identifier = getXmlIdentifier("revanced_prefs");
        if (identifier == 0) return;
        addPreferencesFromResource(identifier);

        PreferenceScreen screen = getPreferenceScreen();
        Utils.sortPreferenceGroups(screen);
        Utils.setPreferenceTitlesToMultiLineIfNeeded(screen);
    }

    private void showSettingUserDialogConfirmation(Preference pref, Setting<?> setting) {
        Utils.verifyOnMainThread();

        final var context = getActivity();
        if (confirmDialogTitle == null) {
            confirmDialogTitle = str("revanced_confirm_user_dialog_title");
        }

        showingUserDialogMessage = true;

        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                // Title.
                confirmDialogTitle,
                // Message.
                Objects.requireNonNull(setting.userDialogMessage).toString(),
                // EditText.
                null,
                // OK button text.
                null,
                // OK button action.
                () -> {
                    // User confirmed, save to the Setting.
                    updatePreference(pref, setting, true, false);

                    // Update availability of other preferences that may be changed.
                    updateUIAvailability();

                    if (setting.rebootApp) {
                        showRestartDialog(context);
                    }
                },
                // Cancel button action.
                () -> {
                    // Restore whatever the setting was before the change.
                    updatePreference(pref, setting, true, true);
                },
                // Neutral button.
                null,
                // Neutral button action.
                null,
                // Dismiss dialog when onNeutralClick.
                true
        );

        dialogPair.first.setOnDismissListener(d -> showingUserDialogMessage = false);

        // Show the dialog.
        dialogPair.first.show();
    }

    /**
     * Updates all Preferences values and their availability using the current values in {@link Setting}.
     */
    protected void updateUIToSettingValues() {
        updatePreferenceScreen(getPreferenceScreen(), true, true);
    }

    /**
     * Updates Preferences availability only using the status of {@link Setting}.
     */
    protected void updateUIAvailability() {
        updatePreferenceScreen(getPreferenceScreen(), false, false);
    }

    /** Rebinds every inline slider after the global summary visibility setting changes. */
    private void refreshSliderSummaries(@NonNull PreferenceGroup group) {
        for (int i = 0, count = group.getPreferenceCount(); i < count; i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof SliderPreference sliderPreference) {
                sliderPreference.refreshSummaryVisibility();
            } else if (preference instanceof RangeSliderPreference rangeSliderPreference) {
                rangeSliderPreference.refreshSummaryVisibility();
            } else if (preference instanceof PreferenceGroup subgroup) {
                refreshSliderSummaries(subgroup);
            }
        }
    }

    /**
     * @return If the preference is currently set to the default value of the Setting.
     */
    protected boolean prefIsSetToDefault(Preference pref, Setting<?> setting) {
        Object defaultValue = setting.defaultValue;
        if (pref instanceof SwitchPreference switchPref) {
            return switchPref.isChecked() == (Boolean) defaultValue;
        }
        String defaultValueString = defaultValue.toString();
        if (pref instanceof EditTextPreference editPreference) {
            return editPreference.getText().equals(defaultValueString);
        }
        if (pref instanceof ListPreference listPref) {
            return listPref.getValue().equals(defaultValueString);
        }

        throw new IllegalStateException("Must override method to handle "
                + "preference type: " + pref.getClass());
    }

    /**
     * Syncs all UI Preferences to any {@link Setting} they represent.
     */
    private void updatePreferenceScreen(@NonNull PreferenceGroup group,
                                        boolean syncSettingValue,
                                        boolean applySettingToPreference) {
        // Alternatively this could iterate through all Settings and check for any matching Preferences,
        // but there are many more Settings than UI preferences so it's more efficient to only check
        // the Preferences.
        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup subGroup) {
                updatePreferenceScreen(subGroup, syncSettingValue, applySettingToPreference);
            } else if (pref.hasKey()) {
                String key = pref.getKey();
                Setting<?> setting = Setting.getSettingFromPath(key);

                if (setting != null) {
                    updatePreference(pref, setting, syncSettingValue, applySettingToPreference);
                } else if (BaseSettings.DEBUG.get() && (pref instanceof SwitchPreference
                        || pref instanceof EditTextPreference || pref instanceof ListPreference)) {
                    // Probably a typo in the patches preference declaration.
                    Logger.printException(() -> "Preference key has no setting: " + key);
                }
            }
        }
    }

    /**
     * Recursively searches a preference group to update a specific preference matching the given key.
     * <p>
     * This method traverses the provided {@link PreferenceGroup}. If a nested {@code PreferenceGroup}
     * is encountered, it recursively searches that subgroup. When a standard {@link Preference}
     * with a matching key is found, it updates the preference using the provided setting configuration.
     * </p>
     *
     * @param group   the root preference group or subgroup to search through, cannot be null
     * @param key     the unique string identifier of the target preference to update, cannot be null
     * @param setting the new setting configuration to apply to the matching preference, cannot be null
     */
    private void updatePreferencesWithKey(@NonNull PreferenceGroup group,
                                          @NonNull String key,
                                          @NonNull Setting<?> setting) {
        for (int i = 0, prefCount = group.getPreferenceCount(); i < prefCount; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup subGroup) {
                updatePreferencesWithKey(subGroup, key, setting);
            } else if (pref.hasKey() && key.equals(pref.getKey())) {
                updatePreference(pref, setting, true, true);
            }
        }
    }

    /**
     * Handles syncing a UI Preference with the {@link Setting} that backs it.
     * If needed, subclasses can override this to handle additional UI Preference types.
     *
     * @param applySettingToPreference If true, then apply {@link Setting} -> Preference.
     *                                 If false, then apply {@link Setting} <- Preference.
     */
    protected void syncSettingWithPreference(@NonNull Preference pref,
                                             @NonNull Setting<?> setting,
                                             boolean applySettingToPreference) {
        if (pref instanceof SwitchPreference switchPref) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            if (applySettingToPreference) {
                switchPref.setChecked(boolSetting.get());
            } else {
                BooleanSetting.privateSetValue(boolSetting, switchPref.isChecked());
            }
        } else if (pref instanceof EditTextPreference editPreference) {
            if (applySettingToPreference) {
                editPreference.setText(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, editPreference.getText());
            }
        } else if (pref instanceof ListPreference listPref) {
            if (applySettingToPreference) {
                listPref.setValue(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, listPref.getValue());
            }
            updateListPreferenceSummary(listPref, setting);
        } else if (pref.getClass().equals(Preference.class)) {
            updatePreferenceSummary(pref, setting);
        } else {
            // Ignore root preference class because there is no data to sync.
            Logger.printException(() -> "Setting cannot be handled: " + pref.getClass() + ": " + pref);
        }
    }

    /**
     * Updates a UI Preference with the {@link Setting} that backs it.
     *
     * @param syncSetting              If the UI should be synced {@link Setting} <-> Preference
     * @param applySettingToPreference If true, then apply {@link Setting} -> Preference.
     *                                 If false, then apply {@link Setting} <- Preference.
     */
    private void updatePreference(@NonNull Preference pref, @NonNull Setting<?> setting,
                                  boolean syncSetting, boolean applySettingToPreference) {
        if (!syncSetting && applySettingToPreference) {
            throw new IllegalArgumentException();
        }

        if (syncSetting) {
            syncSettingWithPreference(pref, setting, applySettingToPreference);
        }

        updatePreferenceAvailability(pref, setting);
    }

    protected void updatePreferenceAvailability(@NonNull Preference pref, @NonNull Setting<?> setting) {
        pref.setEnabled(setting.isAvailable());
    }

    public static void updateListPreferenceSummary(ListPreference listPreference, Setting<?> setting) {
        String objectStringValue = setting.get().toString();
        int entryIndex = listPreference.findIndexOfValue(objectStringValue);
        if (entryIndex < 0) {
            listPreference.setSummary(objectStringValue);
        } else {
            listPreference.setValue(objectStringValue);
            listPreference.setSummary(listPreference.getEntries()[entryIndex]);
        }
    }

    public static void updatePreferenceSummary(Preference preference, Setting<?> setting) {
        try {
            final String settingsKey = setting.key;
            final String entryKey = settingsKey + "_entries";
            final String entryValueKey = settingsKey + "_entry_values";
            final String[] mEntries = app.morphe.extension.shared.utils.ResourceUtils.getStringArray(entryKey);
            final String[] mEntryValues = app.morphe.extension.shared.utils.ResourceUtils.getStringArray(entryValueKey);

            final String valueStr = setting.get().toString();
            int index = org.apache.commons.lang3.ArrayUtils.indexOf(mEntryValues, valueStr);
            if (index < 0) {
                index = org.apache.commons.lang3.ArrayUtils.indexOf(mEntryValues, valueStr.toUpperCase(java.util.Locale.ENGLISH));
            }
            if (index >= 0 && index < mEntries.length) {
                preference.setSummary(mEntries[index]);
            }
        } catch (Exception ignored) {}
    }

    public static void showRestartDialog(@NonNull Context context) {
        showRestartDialog(context, null);
    }

    public static void showRestartDialog(@NonNull Context context, String message) {
        showRestartDialog(context, message, 0);
    }

    public static void showRestartDialog(@NonNull Context context, String message, long delay) {
        showRestartDialog(context, message, delay, true);
    }

    public static void showRestartDialog(@NonNull Context context, String message, long delay, boolean cancelable) {
        Utils.verifyOnMainThread();
        if (showingRestartDialog) {
            Logger.printDebug(() -> "Ignoring show restart dialog as restart dialog is already shown");
            return;
        }
        if (restartDialogTitle == null) {
            restartDialogTitle = str("revanced_restart_title");
        }
        if (restartDialogMessage == null) {
            restartDialogMessage = str("revanced_restart_dialog_message");
        }
        if (restartDialogButtonText == null) {
            restartDialogButtonText = str("revanced_restart");
        }

        showingRestartDialog = true;

        if (BaseThemeUtils.isSupportModernDialog) {
            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                    context,
                    // Title.
                    restartDialogTitle,
                    // Message.
                    message == null ? restartDialogMessage : message,
                    // EditText.
                    null,
                    // OK button text.
                    restartDialogButtonText,
                    // OK button action.
                    () -> Utils.runOnMainThreadDelayed(() -> Utils.restartApp(context), delay),
                    // Cancel button action.
                    cancelable ? () -> {
                    } : null,
                    // Neutral button text.
                    null,
                    // Neutral button action.
                    null,
                    // Dismiss dialog when onNeutralClick.
                    true
            );

            dialogPair.first.setOnDismissListener(d -> showingRestartDialog = false);
            if (!cancelable) {
                dialogPair.first.setCancelable(false);
                dialogPair.first.setCanceledOnTouchOutside(false);
            }

            // Show the dialog.
            dialogPair.first.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(restartDialogTitle)
                    .setMessage(message == null ? restartDialogMessage : message)
                    .setPositiveButton(android.R.string.ok, (dialog, id)
                            -> Utils.runOnMainThreadDelayed(() -> Utils.restartApp(context), delay));
            if (cancelable) {
                builder.setNegativeButton(android.R.string.cancel, null);
            } else {
                builder.setCancelable(false);
            }
            builder.setOnDismissListener(d -> showingRestartDialog = false)
                    .show();
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PreferenceManager preferenceManager = getPreferenceManager();
            preferenceManager.setSharedPreferencesName(Setting.preferences.name);

            // Must initialize before adding change listener,
            // otherwise the syncing of Setting -> UI
            // causes a callback to the listener even though nothing changed.
            initialize();
            updateUIToSettingValues();

            preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate() failure", ex);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preferenceScreen != null) {
            attachPreferenceLongClickListener(preferenceScreen.getDialog());
        }
        boolean handled = super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference instanceof PreferenceScreen subScreen) {
            attachPreferenceLongClickListener(subScreen.getDialog());
        }
        return handled;
    }

    private void attachPreferenceLongClickListener(@Nullable Dialog dialog) {
        if (dialog == null) return;
        ListView listView = dialog.findViewById(android.R.id.list);
        if (listView != null && listView.getOnItemLongClickListener() == null) {
            listView.setOnItemLongClickListener(this::onPreferenceLongClick);
        }
    }

    private boolean onPreferenceLongClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Object item = parent.getAdapter().getItem(position);
            if (!(item instanceof Preference preference)) return false;

            List<CharSequence> path = findPreferencePath(preference);
            if (path == null || path.isEmpty()) return false;

            String text = TextUtils.join(" > ", path);
            Utils.setClipboard(text);
            Utils.showToastShort(str("morphe_settings_menu_copy_path", text));
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "onPreferenceLongClick failure", ex);
            return false;
        }
    }

    @Nullable
    private List<CharSequence> findPreferencePath(Preference target) {
        PreferenceScreen root = getPreferenceScreen();
        if (root == null) return null;
        List<CharSequence> path = new ArrayList<>();
        path.add(str("revanced_settings_title"));
        if (target == root) return path;
        return searchPreferencePath(root, target, path) ? path : null;
    }

    private static boolean searchPreferencePath(PreferenceGroup group, Preference target, List<CharSequence> path) {
        for (int i = 0, n = group.getPreferenceCount(); i < n; i++) {
            Preference p = group.getPreference(i);
            // NoTitlePreferenceCategory reports its first child title so it can be sorted,
            // but that title is never shown to the user and must not appear in the path.
            CharSequence title = (p instanceof NoTitlePreferenceCategory) ? null : p.getTitle();
            if (p == target) {
                if (!TextUtils.isEmpty(title)) path.add(title);
                return true;
            }
            if (p instanceof PreferenceGroup subGroup) {
                int sizeBefore = path.size();
                if (!TextUtils.isEmpty(title)) path.add(title);
                if (searchPreferencePath(subGroup, target, path)) return true;
                while (path.size() > sizeBefore) path.remove(path.size() - 1);
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            ListView listView = view.findViewById(android.R.id.list);
            if (listView != null && listView.getOnItemLongClickListener() == null) {
                listView.setOnItemLongClickListener(this::onPreferenceLongClick);
            }
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        final View rootView = getView();
        if (rootView == null) return;
        ListView listView = getView().findViewById(android.R.id.list);
        if (listView == null) return;
        listView.setDivider(null);
        listView.setDividerHeight(0);
        if (listView.getOnItemLongClickListener() == null) {
            listView.setOnItemLongClickListener(this::onPreferenceLongClick);
        }
    }

    @Override
    public void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }
}
