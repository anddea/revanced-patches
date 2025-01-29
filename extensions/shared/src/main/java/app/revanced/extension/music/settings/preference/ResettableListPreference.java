package app.revanced.extension.music.settings.preference;

import static app.revanced.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.revanced.extension.shared.utils.ResourceUtils.getStringArray;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Locale;

import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;

public class ResettableListPreference {
    private static int mClickedDialogEntryIndex;

    public static void showDialog(Activity mActivity, @NonNull Setting<String> setting, int defaultIndex) {
        try {
            final String settingsKey = setting.key;

            final String entryKey = settingsKey + "_entries";
            final String entryValueKey = settingsKey + "_entry_values";
            final String[] mEntries = getStringArray(entryKey);
            final String[] mEntryValues = getStringArray(entryValueKey);

            final int findIndex = Arrays.binarySearch(mEntryValues, setting.get());
            mClickedDialogEntryIndex = findIndex >= 0 ? findIndex : defaultIndex;

            getDialogBuilder(mActivity)
                    .setTitle(str(settingsKey + "_title"))
                    .setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                            (dialog, id) -> mClickedDialogEntryIndex = id)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_extended_settings_reset"), (dialog, which) -> {
                        setting.resetToDefault();
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        setting.save(mEntryValues[mClickedDialogEntryIndex]);
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }

    public static void showDialog(Activity mActivity, @NonNull EnumSetting<?> setting, int defaultIndex) {
        try {
            final String settingsKey = setting.key;

            final String entryKey = settingsKey + "_entries";
            final String entryValueKey = settingsKey + "_entry_values";
            final String[] mEntries = getStringArray(entryKey);
            final String[] mEntryValues = getStringArray(entryValueKey);

            final int findIndex = ArrayUtils.indexOf(mEntryValues, setting.get().toString().toUpperCase(Locale.ENGLISH));
            mClickedDialogEntryIndex = findIndex >= 0 ? findIndex : defaultIndex;

            getDialogBuilder(mActivity)
                    .setTitle(str(settingsKey + "_title"))
                    .setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                            (dialog, id) -> mClickedDialogEntryIndex = id)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_extended_settings_reset"), (dialog, which) -> {
                        setting.resetToDefault();
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        setting.saveValueFromString(mEntryValues[mClickedDialogEntryIndex]);
                        ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }
}
