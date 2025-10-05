package app.revanced.extension.music.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;

import app.revanced.extension.shared.patches.auth.AuthPatch;
import app.revanced.extension.shared.utils.Utils;

public class SpoofStreamingDataVRAuthTokenDialogBuilder {

    public static void showDialog(Activity mActivity) {
        AlertDialog.Builder builder = Utils.getDialogBuilder(mActivity);

        String dialogTitle =
                str("revanced_spoof_streaming_data_vr_auth_token_dialog_title");
        String dialogMessage =
                str("revanced_spoof_streaming_data_vr_auth_token_dialog_message");
        String resetButtonText =
                str("revanced_spoof_streaming_data_vr_auth_token_dialog_reset_text");

        builder.setTitle(dialogTitle);
        builder.setMessage(dialogMessage);
        builder.setNeutralButton(resetButtonText, (dialog, id) -> AuthPatch.clearAll());

        if (AuthPatch.isDeviceCodeAvailable()) {
            String okButtonText =
                    str("revanced_spoof_streaming_data_vr_auth_token_dialog_get_authorization_token_text");
            builder.setPositiveButton(okButtonText, (dialog, id) -> {
                AuthPatch.setRefreshToken();
                AuthPatch.setAccessToken(true);
            });
        } else {
            String okButtonText =
                    str("revanced_spoof_streaming_data_vr_auth_token_dialog_get_activation_code_text");
            builder.setPositiveButton(okButtonText, (dialog, id) -> {
                AuthPatch.setActivationCode(mActivity);
            });
        }
        builder.show();
    }
}