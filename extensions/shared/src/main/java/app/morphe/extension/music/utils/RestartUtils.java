package app.morphe.extension.music.utils;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;
import android.app.AlertDialog;
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
        showRestartDialog(activity, "revanced_restart_message", false);
    }

    public static void showRestartDialog(@NonNull Activity activity, @NonNull String message,
                                         boolean hideCancelButton) {
        AlertDialog.Builder builder = getDialogBuilder(activity);
        builder.setMessage(str(message));
        builder.setPositiveButton(android.R.string.ok, (dialog, id) ->
                runOnMainThreadDelayed(() -> restartApp(activity), hideCancelButton ? 1000 : 0)
        );
        if (hideCancelButton) {
            builder.setCancelable(false);
        } else {
            builder.setNegativeButton(android.R.string.cancel, null);
        }
        builder.show();
    }
}