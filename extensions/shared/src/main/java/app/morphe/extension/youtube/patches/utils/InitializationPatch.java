package app.morphe.extension.youtube.patches.utils;

import static app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class InitializationPatch {
    private static final BooleanSetting SETTINGS_INITIALIZED = BaseSettings.SETTINGS_INITIALIZED;

    /**
     * Some layouts do not load until YouTube fetches its global configuration.
     * <p>
     * Show the first-run restart prompt only after that update, then allow another second for
     * YouTube to persist it before the user can restart.
     */
    public static void onGlobalConfigUpdated() {
        if (SETTINGS_INITIALIZED.get()) {
            return;
        }
        SETTINGS_INITIALIZED.save(true);

        runOnMainThreadDelayed(() -> {
            Activity activity = Utils.getActivity();
            if (activity == null || activity.isFinishing()) {
                Logger.printInfo(() -> "Activity is unavailable, skipping first-run restart dialog");
                return;
            }
            showRestartDialog(activity, str("revanced_restart_first_run"), 500, false);
        }, 1000);
    }
}
