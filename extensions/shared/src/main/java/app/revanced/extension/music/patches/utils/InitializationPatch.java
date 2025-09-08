package app.revanced.extension.music.patches.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Utils;

import java.lang.ref.WeakReference;

import static app.revanced.extension.music.utils.RestartUtils.showRestartDialog;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getActivity;

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

    public static void onLoggedIn(@Nullable String dataSyncId) {
        if (!SETTINGS_INITIALIZED.get()) {
            // User logged in.
            if (dataSyncId != null && dataSyncId.contains("||")) {
                SETTINGS_INITIALIZED.save(true);
                Utils.runOnMainThreadDelayed(() ->
                                showRestartDialog(activityRef.get(), "revanced_extended_restart_first_run", true),
                        2000
                );

                if (PatchStatus.SpoofClient() || PatchStatus.SpoofVideoStreams()) {
                    Activity context = getActivity();

                    String rvxSettingsLabel = str("revanced_extended_settings_title");
                    String spoofMessage = str("revanced_spoof_streaming_data_message");
                    String finalSpoofMessage = String.format(spoofMessage, rvxSettingsLabel);
                    Spanned formattedMessage = Html.fromHtml(finalSpoofMessage, Html.FROM_HTML_MODE_LEGACY);

                    Utils.runOnMainThreadDelayed(() -> {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                                .setIconAttribute(android.R.attr.alertDialogIcon)
                                .setTitle(str("revanced_external_downloader_not_installed_dialog_title"))
                                .setMessage(formattedMessage)
                                .setPositiveButton(android.R.string.ok, null);

                        dialogBuilder.setCancelable(false);
                        dialogBuilder.show();
                    }, 2001);
                }
            }
        }
    }
}
