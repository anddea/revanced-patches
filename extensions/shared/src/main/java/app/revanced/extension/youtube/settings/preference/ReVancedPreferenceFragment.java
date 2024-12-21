package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.showToastShort;
import static app.revanced.extension.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT_TYPE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends PreferenceFragment {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    static boolean settingImportInProgress = false;
    static boolean showingUserDialogMessage;

    @SuppressLint("SuspiciousIndentation")
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (str == null) return;
            Setting<?> setting = Setting.getSettingFromPath(str);

            if (setting == null) return;

            Preference mPreference = findPreference(str);

            if (mPreference == null) return;

            if (mPreference instanceof SwitchPreference switchPreference) {
                BooleanSetting boolSetting = (BooleanSetting) setting;
                if (settingImportInProgress) {
                    switchPreference.setChecked(boolSetting.get());
                } else {
                    BooleanSetting.privateSetValue(boolSetting, switchPreference.isChecked());
                }

                if (ExtendedUtils.anyMatchSetting(setting)) {
                    ExtendedUtils.setPlayerFlyoutMenuAdditionalSettings();
                } else if (setting.equals(HIDE_PREVIEW_COMMENT) || setting.equals(HIDE_PREVIEW_COMMENT_TYPE)) {
                    ExtendedUtils.setCommentPreviewSettings();
                }
            } else if (mPreference instanceof EditTextPreference editTextPreference) {
                if (settingImportInProgress) {
                    editTextPreference.setText(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, editTextPreference.getText());
                }
            } else if (mPreference instanceof ListPreference listPreference) {
                if (settingImportInProgress) {
                    listPreference.setValue(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, listPreference.getValue());
                }
                if (setting.equals(DEFAULT_PLAYBACK_SPEED)) {
                    listPreference.setEntries(CustomPlaybackSpeedPatch.getListEntries());
                    listPreference.setEntryValues(CustomPlaybackSpeedPatch.getListEntryValues());
                }
                if (!(mPreference instanceof app.revanced.extension.youtube.settings.preference.SegmentCategoryListPreference)) {
                    updateListPreferenceSummary(listPreference, setting);
                }
            } else {
                Logger.printException(() -> "Setting cannot be handled: " + mPreference.getClass() + " " + mPreference);
                return;
            }

            ReVancedSettingsPreference.initializeReVancedSettings();

            if (settingImportInProgress) {
                return;
            }

            if (!showingUserDialogMessage) {
                final Context context = getActivity();

                if (setting.userDialogMessage != null
                        && mPreference instanceof SwitchPreference switchPreference
                        && setting.defaultValue instanceof Boolean defaultValue
                        && switchPreference.isChecked() != defaultValue) {
                    showSettingUserDialogConfirmation(context, switchPreference, (BooleanSetting) setting);
                } else if (setting.rebootApp) {
                    showRestartDialog(context);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    private void showSettingUserDialogConfirmation(Context context, SwitchPreference switchPreference, BooleanSetting setting) {
        Utils.verifyOnMainThread();

        showingUserDialogMessage = true;
        assert setting.userDialogMessage != null;
        new AlertDialog.Builder(context)
                .setTitle(str("revanced_extended_confirm_user_dialog_title"))
                .setMessage(setting.userDialogMessage.toString())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (setting.rebootApp) {
                        showRestartDialog(context);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    switchPreference.setChecked(setting.defaultValue); // Recursive call that resets the Setting value.
                })
                .setOnDismissListener(dialog -> showingUserDialogMessage = false)
                .setCancelable(false)
                .show();
    }

    static PreferenceManager mPreferenceManager;
    private SharedPreferences mSharedPreferences;

    private PreferenceScreen originalPreferenceScreen;

    public ReVancedPreferenceFragment() {
        // Required empty public constructor
    }

    private void putPreferenceScreenMap(SortedMap<String, PreferenceScreen> preferenceScreenMap, PreferenceGroup preferenceGroup) {
        if (preferenceGroup instanceof PreferenceScreen mPreferenceScreen) {
            preferenceScreenMap.put(mPreferenceScreen.getKey(), mPreferenceScreen);
        }
    }

    private void setPreferenceScreenToolbar() {
        SortedMap<String, PreferenceScreen> preferenceScreenMap = new TreeMap<>();

        PreferenceScreen rootPreferenceScreen = getPreferenceScreen();
        for (Preference preference : getAllPreferencesBy(rootPreferenceScreen)) {
            if (!(preference instanceof PreferenceGroup preferenceGroup)) continue;
            putPreferenceScreenMap(preferenceScreenMap, preferenceGroup);
            for (Preference childPreference : getAllPreferencesBy(preferenceGroup)) {
                if (!(childPreference instanceof PreferenceGroup nestedPreferenceGroup)) continue;
                putPreferenceScreenMap(preferenceScreenMap, nestedPreferenceGroup);
                for (Preference nestedPreference : getAllPreferencesBy(nestedPreferenceGroup)) {
                    if (!(nestedPreference instanceof PreferenceGroup childPreferenceGroup))
                        continue;
                    putPreferenceScreenMap(preferenceScreenMap, childPreferenceGroup);
                }
            }
        }

        for (PreferenceScreen mPreferenceScreen : preferenceScreenMap.values()) {
            mPreferenceScreen.setOnPreferenceClickListener(
                    preferenceScreen -> {
                        Dialog preferenceScreenDialog = mPreferenceScreen.getDialog();
                        ViewGroup rootView = (ViewGroup) preferenceScreenDialog
                                .findViewById(android.R.id.content)
                                .getParent();

                        Toolbar toolbar = new Toolbar(preferenceScreen.getContext());

                        toolbar.setTitle(preferenceScreen.getTitle());
                        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
                        toolbar.setNavigationOnClickListener(view -> preferenceScreenDialog.dismiss());

                        int margin = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()
                        );

                        toolbar.setTitleMargin(margin, 0, margin, 0);

                        TextView toolbarTextView = getChildView(toolbar, TextView.class::isInstance);
                        if (toolbarTextView != null) {
                            toolbarTextView.setTextColor(ThemeUtils.getForegroundColor());
                        }

                        rootView.addView(toolbar, 0);
                        return false;
                    }
            );
        }
    }

    // Map to store dependencies: key is the preference key, value is a list of dependent preferences
    private final Map<String, List<Preference>> dependencyMap = new HashMap<>();
    // Set to track already added preferences to avoid duplicates
    private final Set<String> addedPreferences = new HashSet<>();
    // Map to store preferences grouped by their parent PreferenceGroup
    private final Map<PreferenceGroup, List<Preference>> groupedPreferences = new LinkedHashMap<>();

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            mPreferenceManager = getPreferenceManager();
            mPreferenceManager.setSharedPreferencesName(Setting.preferences.name);
            mSharedPreferences = mPreferenceManager.getSharedPreferences();
            addPreferencesFromResource(getXmlIdentifier("revanced_prefs"));

            // Initialize toolbars and other UI elements
            setPreferenceScreenToolbar();

            // Initialize ReVanced settings
            ReVancedSettingsPreference.initializeReVancedSettings();
            SponsorBlockSettingsPreference.init(getActivity());

            // Import/export
            setBackupRestorePreference();

            // Store all preferences and their dependencies
            storeAllPreferences(getPreferenceScreen());

            // Load and set initial preferences states
            for (Setting<?> setting : Setting.allLoadedSettings()) {
                final Preference preference = mPreferenceManager.findPreference(setting.key);
                if (preference != null && isSDKAbove(26)) {
                    preference.setSingleLineTitle(false);
                }

                if (preference instanceof SwitchPreference switchPreference) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    switchPreference.setChecked(boolSetting.get());
                } else if (preference instanceof EditTextPreference editTextPreference) {
                    editTextPreference.setText(setting.get().toString());
                } else if (preference instanceof ListPreference listPreference) {
                    if (setting.equals(DEFAULT_PLAYBACK_SPEED)) {
                        listPreference.setEntries(CustomPlaybackSpeedPatch.getListEntries());
                        listPreference.setEntryValues(CustomPlaybackSpeedPatch.getListEntryValues());
                    }
                    if (!(preference instanceof app.revanced.extension.youtube.settings.preference.SegmentCategoryListPreference)) {
                        updateListPreferenceSummary(listPreference, setting);
                    }
                }
            }

            // Register preference change listener
            mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);

            originalPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            copyPreferences(getPreferenceScreen(), originalPreferenceScreen);
        } catch (Exception th) {
            Logger.printException(() -> "Error during onCreate()", th);
        }
    }

    private void copyPreferences(PreferenceScreen source, PreferenceScreen destination) {
        for (Preference preference : getAllPreferencesBy(source)) {
            destination.addPreference(preference);
        }
    }

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    /**
     * Recursively stores all preferences and their dependencies grouped by their parent PreferenceGroup.
     *
     * @param preferenceGroup The preference group to scan.
     */
    private void storeAllPreferences(PreferenceGroup preferenceGroup) {
        // Check if this is the root PreferenceScreen
        boolean isRootScreen = preferenceGroup == getPreferenceScreen();

        // Use the special top-level group only for the root PreferenceScreen
        PreferenceGroup groupKey = isRootScreen
                ? new PreferenceCategory(preferenceGroup.getContext())
                : preferenceGroup;

        if (isRootScreen) {
            groupKey.setTitle(ResourceUtils.getString("revanced_extended_settings_title"));
        }

        // Initialize a list to hold preferences of the current group
        List<Preference> currentGroupPreferences = groupedPreferences.computeIfAbsent(groupKey, k -> new ArrayList<>());

        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);

            // Add preference to the current group if not already added
            if (!currentGroupPreferences.contains(preference)) {
                currentGroupPreferences.add(preference);
            }

            // Store dependencies
            if (preference.getDependency() != null) {
                String dependencyKey = preference.getDependency();
                dependencyMap.computeIfAbsent(dependencyKey, k -> new ArrayList<>()).add(preference);
            }

            // Recursively handle nested PreferenceGroups
            if (preference instanceof PreferenceGroup nestedGroup) {
                storeAllPreferences(nestedGroup);
            }
        }
    }

    /**
     * Filters preferences based on the search query, displaying grouped results with group titles.
     *
     * @param query The search query.
     */
    public void filterPreferences(String query) {
        // If the query is null or empty, reset preferences to their default state
        if (query == null || query.isEmpty()) {
            resetPreferences();
            return;
        }

        // Convert the query to lowercase for case-insensitive search
        query = query.toLowerCase();

        // Get the preference screen to modify
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        // Remove all current preferences from the screen
        preferenceScreen.removeAll();
        // Clear the list of added preferences to start fresh
        addedPreferences.clear();

        // Create a map to store matched preferences for each group
        Map<PreferenceGroup, List<Preference>> matchedGroupPreferences = new LinkedHashMap<>();

        // Create a set to store all keys that should be included
        Set<String> keysToInclude = new HashSet<>();

        // First pass: identify all preferences that match the query and their dependencies
        for (Map.Entry<PreferenceGroup, List<Preference>> entry : groupedPreferences.entrySet()) {
            List<Preference> preferences = entry.getValue();
            for (Preference preference : preferences) {
                if (preferenceMatches(preference, query)) {
                    addPreferenceAndDependencies(preference, keysToInclude);
                }
            }
        }

        // Second pass: add all identified preferences to matchedGroupPreferences
        for (Map.Entry<PreferenceGroup, List<Preference>> entry : groupedPreferences.entrySet()) {
            PreferenceGroup group = entry.getKey();
            List<Preference> preferences = entry.getValue();
            List<Preference> matchedPreferences = new ArrayList<>();

            for (Preference preference : preferences) {
                if (keysToInclude.contains(preference.getKey())) {
                    matchedPreferences.add(preference);
                }
            }

            if (!matchedPreferences.isEmpty()) {
                matchedGroupPreferences.put(group, matchedPreferences);
            }
        }

        // Add matched preferences to the screen, maintaining the original order
        for (Map.Entry<PreferenceGroup, List<Preference>> entry : matchedGroupPreferences.entrySet()) {
            PreferenceGroup group = entry.getKey();
            List<Preference> matchedPreferences = entry.getValue();

            // Add the category for this group
            PreferenceCategory category = new PreferenceCategory(preferenceScreen.getContext());
            category.setTitle(group.getTitle());
            preferenceScreen.addPreference(category);

            // Add matched preferences for this group
            for (Preference preference : matchedPreferences) {
                if (preference.isSelectable()) {
                    addPreferenceWithDependencies(category, preference);
                } else {
                    // For non-selectable preferences, just add them directly
                    category.addPreference(preference);
                }
            }
        }
    }

    /**
     * Checks if a preference matches the given query.
     *
     * @param preference The preference to check.
     * @param query      The search query.
     * @return True if the preference matches the query, false otherwise.
     */
    private boolean preferenceMatches(Preference preference, String query) {
        // Check if the title contains the query string
        if (preference.getTitle().toString().toLowerCase().contains(query)) {
            return true;
        }

        // Check if the summary contains the query string
        if (preference.getSummary() != null && preference.getSummary().toString().toLowerCase().contains(query)) {
            return true;
        }

        // Additional checks for SwitchPreference
        if (preference instanceof SwitchPreference switchPreference) {
            CharSequence summaryOn = switchPreference.getSummaryOn();
            CharSequence summaryOff = switchPreference.getSummaryOff();

            if ((summaryOn != null && summaryOn.toString().toLowerCase().contains(query)) ||
                    (summaryOff != null && summaryOff.toString().toLowerCase().contains(query))) {
                return true;
            }
        }

        // Additional checks for ListPreference
        if (preference instanceof ListPreference listPreference) {
            CharSequence[] entries = listPreference.getEntries();
            if (entries != null) {
                for (CharSequence entry : entries) {
                    if (entry.toString().toLowerCase().contains(query)) {
                        return true;
                    }
                }
            }

            CharSequence[] entryValues = listPreference.getEntryValues();
            if (entryValues != null) {
                for (CharSequence entryValue : entryValues) {
                    if (entryValue.toString().toLowerCase().contains(query)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Recursively adds a preference and its dependencies to the set of keys to include.
     *
     * @param preference    The preference to add.
     * @param keysToInclude The set of keys to include.
     */
    private void addPreferenceAndDependencies(Preference preference, Set<String> keysToInclude) {
        String key = preference.getKey();
        if (key != null && !keysToInclude.contains(key)) {
            keysToInclude.add(key);

            // Add the preference this one depends on
            String dependencyKey = preference.getDependency();
            if (dependencyKey != null) {
                Preference dependency = findPreferenceInAllGroups(dependencyKey);
                if (dependency != null) {
                    addPreferenceAndDependencies(dependency, keysToInclude);
                }
            }

            // Add preferences that depend on this one
            if (dependencyMap.containsKey(key)) {
                for (Preference dependentPreference : Objects.requireNonNull(dependencyMap.get(key))) {
                    addPreferenceAndDependencies(dependentPreference, keysToInclude);
                }
            }
        }
    }

    /**
     * Recursively adds a preference along with its dependencies
     * (android:dependency attribute in XML).
     *
     * @param preferenceGroup The preference group to add to.
     * @param preference      The preference to add.
     */
    private void addPreferenceWithDependencies(PreferenceGroup preferenceGroup, Preference preference) {
        String key = preference.getKey();

        // Instead of just using preference keys, we combine the category and key to ensure uniqueness
        if (key != null && !addedPreferences.contains(preferenceGroup.getTitle() + ":" + key)) {
            // Add dependencies first
            if (preference.getDependency() != null) {
                String dependencyKey = preference.getDependency();
                Preference dependency = findPreferenceInAllGroups(dependencyKey);
                if (dependency != null) {
                    addPreferenceWithDependencies(preferenceGroup, dependency);
                } else {
                    return;
                }
            }

            // Add the preference using a combination of the category and the key
            preferenceGroup.addPreference(preference);
            addedPreferences.add(preferenceGroup.getTitle() + ":" + key); // Track based on both category and key

            // Handle dependent preferences
            if (dependencyMap.containsKey(key)) {
                for (Preference dependentPreference : Objects.requireNonNull(dependencyMap.get(key))) {
                    addPreferenceWithDependencies(preferenceGroup, dependentPreference);
                }
            }
        }
    }

    /**
     * Finds a preference in all groups based on its key.
     *
     * @param key The key of the preference to find.
     * @return The found preference, or null if not found.
     */
    private Preference findPreferenceInAllGroups(String key) {
        for (List<Preference> preferences : groupedPreferences.values()) {
            for (Preference preference : preferences) {
                if (preference.getKey() != null && preference.getKey().equals(key)) {
                    return preference;
                }
            }
        }
        return null;
    }

    /**
     * Resets the preference screen to its original state.
     */
    private void resetPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        for (Preference preference : getAllPreferencesBy(originalPreferenceScreen))
            preferenceScreen.addPreference(preference);
    }

    private List<Preference> getAllPreferencesBy(PreferenceGroup preferenceGroup) {
        List<Preference> preferences = new ArrayList<>();
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++)
            preferences.add(preferenceGroup.getPreference(i));
        return preferences;
    }

    /**
     * Add Preference to Import/Export settings submenu
     */
    private void setBackupRestorePreference() {
        findPreference("revanced_extended_settings_import").setOnPreferenceClickListener(pref -> {
            importActivity();
            return false;
        });

        findPreference("revanced_extended_settings_export").setOnPreferenceClickListener(pref -> {
            exportActivity();
            return false;
        });
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    private void exportActivity() {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final String appName = ExtendedUtils.getAppLabel();
        final String versionName = ExtendedUtils.getAppVersionName();
        final String formatDate = dateFormat.format(new Date(System.currentTimeMillis()));
        final String fileName = String.format("%s_v%s_%s.txt", appName, versionName, formatDate);

        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    /**
     * Invoke the SAF(Storage Access Framework) to import settings
     */
    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Activity should be done within the lifecycle of PreferenceFragment
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            exportText(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importText(data.getData());
        }
    }

    private void exportText(Uri uri) {
        final Context context = this.getActivity();

        try {
            @SuppressLint("Recycle")
            FileWriter jsonFileWriter =
                    new FileWriter(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "w"))
                                    .getFileDescriptor()
                    );
            PrintWriter printWriter = new PrintWriter(jsonFileWriter);
            printWriter.write(Setting.exportToJson(context));
            printWriter.close();
            jsonFileWriter.close();

            showToastShort(str("revanced_extended_settings_export_success"));
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_export_failed"));
        }
    }

    private void importText(Uri uri) {
        final Context context = this.getActivity();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            settingImportInProgress = true;

            @SuppressLint("Recycle")
            FileReader fileReader =
                    new FileReader(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "r"))
                                    .getFileDescriptor()
                    );
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
            fileReader.close();

            final boolean restartNeeded = Setting.importFromJSON(context, sb.toString());
            if (restartNeeded) {
                showRestartDialog(getActivity());
            }
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_import_failed"));
            throw new RuntimeException(e);
        } finally {
            settingImportInProgress = false;
        }
    }
}
