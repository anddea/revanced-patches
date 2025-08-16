package app.revanced.extension.music.patches.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import androidx.annotation.NonNull;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Utils;

import static app.revanced.extension.music.utils.RestartUtils.showRestartDialog;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getActivity;

@SuppressWarnings("unused")
public class InitializationPatch {

    /**
     * The new layout is not loaded normally when the app is first installed.
     * (Also reproduced on unPatched YouTube Music)
     * <p>
     * To fix this, show the reboot dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (BaseSettings.SETTINGS_INITIALIZED.get())
            return;

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
        }, 1000);

        showRestartDialog(mActivity, "revanced_extended_restart_first_run", 3000);
        Utils.runOnMainThreadDelayed(() -> BaseSettings.SETTINGS_INITIALIZED.save(true), 3000);
    }
}
