package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;

import app.morphe.extension.shared.patches.auth.YouTubeAuthPatch;
import app.morphe.extension.shared.patches.auth.YouTubeVRAuthPatch;
import app.morphe.extension.shared.utils.IntentUtils;
import app.morphe.extension.shared.utils.Utils;

public class SpoofStreamingDataSignInDialogBuilder {

    public static void showNoSDKDialog(Activity mActivity) {
        AlertDialog.Builder builder = Utils.getDialogBuilder(mActivity);

        String dialogTitle =
                str("revanced_spoof_streaming_data_sign_in_android_no_sdk_dialog_title");
        String dialogMessage =
                str("revanced_spoof_streaming_data_sign_in_android_no_sdk_dialog_message");
        String resetButtonText =
                str("revanced_spoof_streaming_data_sign_in_android_no_sdk_dialog_reset_text");
        String okButtonText =
                str("revanced_spoof_streaming_data_sign_in_android_no_sdk_dialog_get_authorization_token_text");

        builder.setTitle(dialogTitle);
        builder.setMessage(dialogMessage);
        builder.setNeutralButton(resetButtonText, (dialog, id) -> YouTubeAuthPatch.clearAll());
        builder.setPositiveButton(okButtonText, (dialog, id) -> IntentUtils.launchWebViewActivity(
                mActivity,
                true,
                true,
                false,
                "https://accounts.google.com/EmbeddedSetup"
        ));
        builder.show();
    }

    public static void showVRDialog(Activity mActivity) {
        AlertDialog.Builder builder = Utils.getDialogBuilder(mActivity);

        String dialogTitle =
                str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_title");
        String dialogMessage =
                str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_message");
        String resetButtonText =
                str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_reset_text");

        builder.setTitle(dialogTitle);
        builder.setMessage(dialogMessage);
        builder.setNeutralButton(resetButtonText, (dialog, id) -> YouTubeVRAuthPatch.clearAll());

        if (YouTubeVRAuthPatch.isDeviceCodeAvailable()) {
            String okButtonText =
                    str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_get_authorization_token_text");
            builder.setPositiveButton(okButtonText, (dialog, id) -> {
                YouTubeVRAuthPatch.setRefreshToken();
                YouTubeVRAuthPatch.setAccessToken(mActivity);
            });
        } else {
            String okButtonText =
                    str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_get_activation_code_text");
            builder.setPositiveButton(okButtonText, (dialog, id) -> {
                YouTubeVRAuthPatch.setActivationCode(mActivity);
            });
        }
        builder.show();
    }
}