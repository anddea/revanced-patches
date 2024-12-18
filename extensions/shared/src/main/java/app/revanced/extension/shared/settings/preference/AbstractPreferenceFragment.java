package app.revanced.extension.shared.settings.preference;

import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public abstract class AbstractPreferenceFragment extends PreferenceFragment {
    /**
     * Indicates that if a preference changes,
     * to apply the change from the Setting to the UI component.
     */
    public static boolean settingImportInProgress;

    /**
     * Confirm and restart dialog button text and title.
     * Set by subclasses if Strings cannot be added as a resource.
     */
    @Nullable
    protected static String restartDialogMessage;

    /**
     * Used to prevent showing reboot dialog, if user cancels a setting user dialog.
     */
    private boolean showingUserDialogMessage;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (str == null) {
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

            // Apply 'Setting <- Preference', unless during importing when it needs to be 'Setting -> Preference'.
            updatePreference(pref, setting, true, settingImportInProgress);
            // Update any other preference availability that may now be different.
            updateUIAvailability();

            if (settingImportInProgress) {
                return;
            }

            if (!showingUserDialogMessage) {
                if (setting.userDialogMessage != null && ((SwitchPreference) pref).isChecked() != (Boolean) setting.defaultValue) {
                    showSettingUserDialogConfirmation((SwitchPreference) pref, (BooleanSetting) setting);
                } else if (setting.rebootApp) {
                    showRestartDialog(getActivity());
                }
            }

        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    /**
     * Initialize this instance, and do any custom behavior.
     * <p>
     * To ensure all {@link Setting} instances are correctly synced to the UI,
     * it is important that subclasses make a call or otherwise reference their Settings class bundle
     * so all app specific {@link Setting} instances are loaded before this method returns.
     */
    protected void initialize() {
        final int id = getXmlIdentifier("revanced_prefs");

        if (id == 0) return;
        addPreferencesFromResource(id);
        Utils.sortPreferenceGroups(getPreferenceScreen());
    }

    private void showSettingUserDialogConfirmation(SwitchPreference switchPref, BooleanSetting setting) {
        Utils.verifyOnMainThread();

        final var context = getActivity();
        showingUserDialogMessage = true;
        assert setting.userDialogMessage != null;
        new AlertDialog.Builder(context)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(setting.userDialogMessage.toString())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (setting.rebootApp) {
                        showRestartDialog(context);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    switchPref.setChecked(setting.defaultValue); // Recursive call that resets the Setting value.
                })
                .setOnDismissListener(dialog -> showingUserDialogMessage = false)
                .setCancelable(false)
                .show();
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

    /**
     * Syncs all UI Preferences to any {@link Setting} they represent.
     */
    private void updatePreferenceScreen(@NonNull PreferenceScreen screen,
                                        boolean syncSettingValue,
                                        boolean applySettingToPreference) {
        // Alternatively this could iterate thru all Settings and check for any matching Preferences,
        // but there are many more Settings than UI preferences so it's more efficient to only check
        // the Preferences.
        for (int i = 0, prefCount = screen.getPreferenceCount(); i < prefCount; i++) {
            Preference pref = screen.getPreference(i);
            if (pref instanceof PreferenceScreen preferenceScreen) {
                updatePreferenceScreen(preferenceScreen, syncSettingValue, applySettingToPreference);
            } else if (pref.hasKey()) {
                String key = pref.getKey();
                Setting<?> setting = Setting.getSettingFromPath(key);
                if (setting != null) {
                    updatePreference(pref, setting, syncSettingValue, applySettingToPreference);
                }
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
        if (pref instanceof SwitchPreference switchPreference) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            if (applySettingToPreference) {
                switchPreference.setChecked(boolSetting.get());
            } else {
                BooleanSetting.privateSetValue(boolSetting, switchPreference.isChecked());
            }
        } else if (pref instanceof EditTextPreference editTextPreference) {
            if (applySettingToPreference) {
                editTextPreference.setText(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, editTextPreference.getText());
            }
        } else if (pref instanceof ListPreference listPreference) {
            if (applySettingToPreference) {
                listPreference.setValue(setting.get().toString());
            } else {
                Setting.privateSetValueFromString(setting, listPreference.getValue());
            }
            updateListPreferenceSummary(listPreference, setting);
        } else {
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
        if (entryIndex >= 0) {
            listPreference.setValue(objectStringValue);
            objectStringValue = listPreference.getEntries()[entryIndex].toString();
        }
        listPreference.setSummary(objectStringValue);
    }

    public static void showRestartDialog(@NonNull final Context context) {
        if (restartDialogMessage == null) {
            restartDialogMessage = str("revanced_extended_restart_message");
        }
        showRestartDialog(context, restartDialogMessage);
    }

    public static void showRestartDialog(@NonNull final Context context, String message) {
        showRestartDialog(context, message, 0);
    }

    public static void showRestartDialog(@NonNull final Context context, String message, long delay) {
        Utils.verifyOnMainThread();

        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id)
                        -> Utils.runOnMainThreadDelayed(() -> Utils.restartApp(context), delay))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
    public void onResume() {
        super.onResume();

        final View rootView = getView();
        if (rootView == null) return;
        ListView listView = getView().findViewById(android.R.id.list);
        if (listView == null) return;
        listView.setDivider(null);
        listView.setDividerHeight(0);
    }

    @Override
    public void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }
}
