package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.patches.PatchStatus.PatchVersion;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.showToastShort;
import static app.revanced.extension.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED;
import static app.revanced.extension.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED_SHORTS;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT_TYPE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.LogBufferManager;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.StringRef;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.settings.ReVancedSettingsHostActivity;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends PreferenceFragment {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    boolean settingExportInProgress = false;
    static boolean settingImportInProgress = false;
    static PreferenceManager mPreferenceManager;

    public String rvxSettingsLabel;
    public String searchLabel;
    public Stack<PreferenceScreen> preferenceScreenStack = new Stack<>();
    public PreferenceScreen rootPreferenceScreen;
    private boolean showingUserDialogMessage = false;

    private PreferenceScreen originalPreferenceScreen;
    private SharedPreferences mSharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (str == null) {
                return;
            }

            Setting<?> setting = Setting.getSettingFromPath(str);
            if (setting == null) {
                return;
            }

            Preference mPreference = findPreference(str);
            if (mPreference == null) {
                return;
            }

            if (mPreference instanceof SwitchPreference switchPreference) {
                BooleanSetting boolSetting = (BooleanSetting) setting;
                Logger.printDebug(() -> "SwitchPreference: " + str + ", checked: " + switchPreference.isChecked());
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
                if (setting.equals(DEFAULT_PLAYBACK_SPEED) || setting.equals(DEFAULT_PLAYBACK_SPEED_SHORTS)) {
                    listPreference.setEntries(CustomPlaybackSpeedPatch.getEntries());
                    listPreference.setEntryValues(CustomPlaybackSpeedPatch.getEntryValues());
                }
                updateListPreferenceSummary(listPreference, setting);
            }

            ReVancedSettingsPreference.initializeReVancedSettings();

            if (!settingImportInProgress && !showingUserDialogMessage) {
                final Context context = getActivity();
                if (setting.userDialogMessage != null && !prefIsSetToDefault(mPreference, setting)) {
                    showSettingUserDialogConfirmation(context, mPreference, setting);
                } else if (setting.rebootApp) {
                    showRestartDialog(context);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            mPreferenceManager = getPreferenceManager();
            mPreferenceManager.setSharedPreferencesName(Setting.preferences.name);
            mSharedPreferences = mPreferenceManager.getSharedPreferences();
            addPreferencesFromResource(getXmlIdentifier("revanced_prefs"));

            rootPreferenceScreen = getPreferenceScreen();

            rvxSettingsLabel = str("revanced_extended_settings_title");
            searchLabel = str("revanced_extended_settings_search_title");

            setPreferenceScreenToolbar();
            ReVancedSettingsPreference.initializeReVancedSettings();
            setBackupRestorePreference();
            setDebugLogPreference();

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
                    if (setting.equals(DEFAULT_PLAYBACK_SPEED) || setting.equals(DEFAULT_PLAYBACK_SPEED_SHORTS)) {
                        listPreference.setEntries(CustomPlaybackSpeedPatch.getEntries());
                        listPreference.setEntryValues(CustomPlaybackSpeedPatch.getEntryValues());
                    }
                    updateListPreferenceSummary(listPreference, setting);
                }
            }

            mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
            originalPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            copyPreferences(getPreferenceScreen(), originalPreferenceScreen);
            sortPreferenceListMenu(Settings.CHANGE_START_PAGE);
            sortPreferenceListMenu(BaseSettings.REVANCED_LANGUAGE);
        } catch (Exception th) {
            Logger.printException(() -> "Error during onCreate()", th);
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (getPreferenceScreen() != null && getPreferenceScreen() != preferenceScreen) {
            preferenceScreenStack.push(getPreferenceScreen());
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            Activity activity = getActivity();
            if (activity != null) {
                String title = preferenceScreen.getTitle() != null
                        ? preferenceScreen.getTitle().toString()
                        : rvxSettingsLabel;
                ReVancedSettingsHostActivity.updateToolbarTitle(title);
            }
        }
    }

    public boolean handleOnBackPressed(String currentQuery) {
        if (getPreferenceScreen() != rootPreferenceScreen) {
            if (!preferenceScreenStack.isEmpty()) {
                setPreferenceScreen(preferenceScreenStack.pop());
                return false;
            } else {
                return true;
            }
        }

        SearchView searchView = getActivity().findViewById(ResourceUtils.getIdIdentifier("search_view"));
        if ((searchView != null && searchView.hasFocus()) || !currentQuery.isEmpty()) {
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.clearFocus();
            }
            resetPreferences();
            return false;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            exportText(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importText(data.getData());
        }
    }

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        mPreferenceManager = null;
        super.onDestroy();
    }

    /**
     * Resets the {@link PreferenceScreen} to its original state, restoring all preferences
     * from the {@link #originalPreferenceScreen} and clearing search results.
     * Updates the main activity's toolbar title to the original settings label.
     */
    public void resetPreferences() {
        PreferenceScreen current = getPreferenceScreen();
        if (current != rootPreferenceScreen) {
            super.setPreferenceScreen(rootPreferenceScreen);
        }
        rootPreferenceScreen.removeAll();
        getAllPreferencesBy(originalPreferenceScreen).forEach(rootPreferenceScreen::addPreference);
        if (getActivity() != null) {
            ReVancedSettingsHostActivity.updateToolbarTitle(rvxSettingsLabel);
        }
    }

    private List<Preference> getAllPreferencesBy(PreferenceGroup preferenceGroup) {
        List<Preference> preferences = new ArrayList<>();
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            preferences.add(preferenceGroup.getPreference(i));
        }
        return preferences;
    }

    private void copyPreferences(PreferenceScreen source, PreferenceScreen destination) {
        getAllPreferencesBy(source).forEach(destination::addPreference);
    }

    public String getFullPath(PreferenceGroup group) {
        Deque<String> titles = new ArrayDeque<>();
        for (PreferenceGroup current = group; current != null; current = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? current.getParent() : null) {
            CharSequence title = current.getTitle();
            if (title != null) {
                String t = title.toString().trim();
                if (!t.isEmpty()) titles.addFirst(t);
            }
        }
        return String.join(" > ", titles);
    }

    public PreferenceScreen findClosestPreferenceScreen(PreferenceGroup group) {
        PreferenceGroup current = group;
        while (current != null) {
            if (current instanceof PreferenceScreen) {
                return (PreferenceScreen) current;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                current = current.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    private void setPreferenceScreenToolbar() {
        SortedMap<String, PreferenceScreen> map = new TreeMap<>();
        for (Preference pref : getAllPreferencesBy(getPreferenceScreen())) {
            if (pref instanceof PreferenceGroup pg) {
                if (pg instanceof PreferenceScreen ps) map.put(ps.getKey(), ps);
                for (Preference child : getAllPreferencesBy(pg))
                    if (child instanceof PreferenceGroup && child instanceof PreferenceScreen ps)
                        map.put(ps.getKey(), ps);
            }
        }

        Integer targetSDKVersion = ExtendedUtils.getTargetSDKVersion(getContext().getPackageName());
        boolean isEdgeToEdge = isSDKAbove(35) && targetSDKVersion != null && targetSDKVersion >= 35;

        for (PreferenceScreen ps : map.values()) {
            ps.setOnPreferenceClickListener(screen -> {
                Dialog dialog = ps.getDialog();
                if (dialog == null) return false;
                ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content).getParent();

                if (isEdgeToEdge) {
                    root.setOnApplyWindowInsetsListener((v, insets) -> {
                        Insets statusInsets = insets.getInsets(WindowInsets.Type.statusBars());
                        Insets navInsets = insets.getInsets(WindowInsets.Type.navigationBars());
                        v.setPadding(0, statusInsets.top, 0, navInsets.bottom);
                        return insets;
                    });
                }

                Toolbar toolbar = new Toolbar(screen.getContext());
                toolbar.setTitle(screen.getTitle());
                toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
                toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                toolbar.setTitleMargin(margin, 0, margin, 0);

                if (getChildView(toolbar, TextView.class::isInstance) instanceof TextView tv)
                    tv.setTextColor(ThemeUtils.getAppForegroundColor());

                ReVancedSettingsHostActivity.setToolbarLayoutParams(toolbar);
                root.addView(toolbar, 0);
                return false;
            });
        }
    }

    private void sortPreferenceListMenu(EnumSetting<?> setting) {
        // Sorting logic can be added here if needed, like in the original code.
    }

    private boolean prefIsSetToDefault(Preference pref, Setting<?> setting) {
        Object defaultValue = setting.defaultValue;
        if (pref instanceof SwitchPreference sp) return sp.isChecked() == (Boolean) defaultValue;
        String defaultValueString = defaultValue.toString();
        if (pref instanceof EditTextPreference etp) return etp.getText().equals(defaultValueString);
        if (pref instanceof ListPreference lp) return lp.getValue().equals(defaultValueString);
        return false;
    }

    private void showSettingUserDialogConfirmation(Context context, Preference pref, Setting<?> setting) {
        Utils.verifyOnMainThread();
        final StringRef userDialogMessage = setting.userDialogMessage;
        if (context == null || userDialogMessage == null) return;

        showingUserDialogMessage = true;
        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(context, str("revanced_extended_confirm_user_dialog_title"), userDialogMessage.toString(), null, null,
                () -> {
                    if (setting.rebootApp) showRestartDialog(context);
                },
                () -> {
                    if (setting instanceof BooleanSetting bs && pref instanceof SwitchPreference sp)
                        sp.setChecked(bs.defaultValue);
                    else if (setting instanceof EnumSetting<?> es && pref instanceof ListPreference lp) {
                        lp.setValue(es.defaultValue.toString());
                        updateListPreferenceSummary(lp, setting);
                    }
                }, null, null, true);
        dialogPair.first.setOnShowListener(d -> showingUserDialogMessage = false);
        dialogPair.first.show();
    }

    private void setBackupRestorePreference() {
        findPreference("revanced_extended_settings_import").setOnPreferenceClickListener(p -> {
            importActivity();
            return false;
        });
        findPreference("revanced_extended_settings_export").setOnPreferenceClickListener(p -> {
            settingExportInProgress = true;
            exportActivity();
            return false;
        });
    }

    private void setDebugLogPreference() {
        findPreference("revanced_debug_logs_clear_buffer").setOnPreferenceClickListener(p -> {
            LogBufferManager.clearLogBuffer();
            return false;
        });
        findPreference("revanced_debug_export_logs_to_clipboard").setOnPreferenceClickListener(p -> {
            LogBufferManager.exportToClipboard();
            return false;
        });
        findPreference("revanced_debug_export_logs_to_file").setOnPreferenceClickListener(p -> {
            exportActivity();
            return false;
        });
    }

    private void exportActivity() {
        if (!settingExportInProgress && !BaseSettings.DEBUG.get()) {
            Utils.showToastShort(str("revanced_debug_logs_disabled"));
            return;
        }
        @SuppressLint("SimpleDateFormat") String formatDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String fileName = String.format("%s_v%s_rvp_v%s_%s_%s", ExtendedUtils.getAppLabel(), ExtendedUtils.getAppVersionName(), PatchVersion(), settingExportInProgress ? "settings" : "log", formatDate);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void exportText(Uri uri) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(Objects.requireNonNull(getActivity().getContentResolver().openFileDescriptor(uri, "w")).getFileDescriptor()))) {
            if (settingExportInProgress) {
                writer.write(Setting.exportToJson(getActivity()));
                showToastShort(str("revanced_extended_settings_export_success"));
            } else {
                String message = LogBufferManager.exportToString();
                if (message != null) writer.write(message);
                showToastShort(str("revanced_debug_logs_export_success"));
            }
        } catch (IOException e) {
            showToastShort(settingExportInProgress ? str("revanced_extended_settings_export_failed") : String.format(str("revanced_debug_logs_failed_to_export"), e.getMessage()));
        } finally {
            settingExportInProgress = false;
        }
    }

    private void importText(Uri uri) {
        try {
            settingImportInProgress = true;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(Objects.requireNonNull(getActivity().getContentResolver().openFileDescriptor(uri, "r")).getFileDescriptor()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            }
            if (Setting.importFromJSON(getActivity(), sb.toString())) showRestartDialog(getActivity());
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_import_failed"));
        } finally {
            settingImportInProgress = false;
        }
    }

    /** A helper class to store metadata about a Preference. */
    public static class PreferenceInfo {
        public final Preference preference;
        @Nullable public CharSequence originalTitle, originalSummary, originalSummaryOn, originalSummaryOff;
        @Nullable public CharSequence[] originalEntries;
        public boolean highlightingApplied;
        public PreferenceInfo(Preference preference, String groupTitle) {
            this.preference = preference;
            this.originalTitle = preference.getTitle();
            this.originalSummary = preference.getSummary();
            if (preference instanceof SwitchPreference sp) {
                this.originalSummaryOn = sp.getSummaryOn();
                this.originalSummaryOff = sp.getSummaryOff();
            }
            if (preference instanceof ListPreference lp) {
                this.originalEntries = lp.getEntries() != null ? Arrays.copyOf(lp.getEntries(), lp.getEntries().length) : null;
            }
        }
    }
}
