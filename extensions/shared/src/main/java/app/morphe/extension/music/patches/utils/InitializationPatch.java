package app.morphe.extension.music.patches.utils;

import static app.morphe.extension.music.utils.RestartUtils.showRestartDialog;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class InitializationPatch {
    private static final BooleanSetting SETTINGS_INITIALIZED =
            BaseSettings.SETTINGS_INITIALIZED;
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    public static void onCreate(@NonNull Activity mActivity) {
        if (!SETTINGS_INITIALIZED.get()) {
            activityRef = new WeakReference<>(mActivity);
        }
    }

    /**
     * The new layout is not loaded normally when the app is first installed.
     * (Also reproduced on unPatched YouTube Music)
     * <p>
     * To fix this, show the reboot dialog when the app is installed for the first time.
     */
    public static void onLoggedIn(@Nullable String dataSyncId) {
        if (!SETTINGS_INITIALIZED.get()) {
            // User logged in.
            if (dataSyncId != null && dataSyncId.contains("||")) {
                SETTINGS_INITIALIZED.save(true);
                Utils.runOnMainThreadDelayed(() ->
                                showRestartDialog(activityRef.get(), "revanced_restart_first_run", true),
                        2000
                );
            }
        }
    }
}
