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

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Locale;

import app.morphe.extension.shared.settings.EnumSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.settings.LongSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.Logger;

public class ResettableListPreference {
    private static int mClickedDialogEntryIndex;

    public static void showDialog(Activity mActivity, @NonNull Setting<?> setting, int defaultIndex) {
        try {
            final String settingsKey = setting.key;

            final String entryKey = settingsKey + "_entries";
            final String entryValueKey = settingsKey + "_entry_values";
            final String[] mEntries = getStringArray(entryKey);
            final String[] mEntryValues = getStringArray(entryValueKey);

            final int findIndex = ArrayUtils.indexOf(mEntryValues, setting.get().toString());
            mClickedDialogEntryIndex = findIndex >= 0 ? findIndex : defaultIndex;

            getDialogBuilder(mActivity)
                    .setTitle(str(settingsKey + "_title"))
                    .setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                            (dialog, id) -> mClickedDialogEntryIndex = id)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_settings_reset"), (dialog, which) -> {
                        setting.resetToDefault();
                        if (setting.rebootApp) ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        String value = mEntryValues[mClickedDialogEntryIndex];
                        if (setting instanceof StringSetting stringSetting) {
                            stringSetting.save(value);
                        } else if (setting instanceof IntegerSetting integerSetting) {
                            integerSetting.save(Integer.parseInt(value));
                        } else if (setting instanceof LongSetting longSetting) {
                            longSetting.save(Long.parseLong(value));
                        }
                        if (setting.rebootApp) ReVancedPreferenceFragment.showRebootDialog();
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
                    .setNeutralButton(str("revanced_settings_reset"), (dialog, which) -> {
                        setting.resetToDefault();
                        if (setting.rebootApp) ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        setting.saveValueFromString(mEntryValues[mClickedDialogEntryIndex]);
                        if (setting.rebootApp) ReVancedPreferenceFragment.showRebootDialog();
                    })
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }
}
