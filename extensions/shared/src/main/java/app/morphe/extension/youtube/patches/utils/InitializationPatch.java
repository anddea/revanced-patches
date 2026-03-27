package app.morphe.extension.youtube.patches.utils;

import static app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;

import androidx.annotation.NonNull;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;

@SuppressWarnings("unused")
public class InitializationPatch {
    private static final BooleanSetting SETTINGS_INITIALIZED = BaseSettings.SETTINGS_INITIALIZED;

    /**
     * Some layouts that depend on litho do not load when the app is first installed.
     * (Also reproduced on unPatched YouTube)
     * <p>
     * To fix this, show the restart dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (!SETTINGS_INITIALIZED.get()) {
            runOnMainThreadDelayed(() -> showRestartDialog(mActivity, str("revanced_restart_first_run"), 3500), 500);
            runOnMainThreadDelayed(() -> SETTINGS_INITIALIZED.save(true), 1000);
        }
    }
}
