package app.revanced.extension.music.utils;

import static app.revanced.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.Objects;

public class RestartUtils {

    public static void restartApp(@NonNull Activity activity) {
        final Intent intent = Objects.requireNonNull(activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName()));
        final Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());

        activity.finishAffinity();
        activity.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void showRestartDialog(@NonNull Activity activity) {
        showRestartDialog(activity, "revanced_extended_restart_message", 0);
    }

    public static void showRestartDialog(@NonNull Activity activity, @NonNull String message, long delay) {
        getDialogBuilder(activity)
                .setMessage(str(message))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> runOnMainThreadDelayed(() -> restartApp(activity), delay))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}