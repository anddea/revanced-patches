package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;

import app.morphe.extension.shared.innertube.client.YouTubeClient;
import app.morphe.extension.shared.patches.auth.YouTubeVRAuthPatch;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"FieldCanBeLocal", "deprecation", "unused"})
public class SpoofStreamingDataSignInPreference extends Preference implements Preference.OnPreferenceClickListener {

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
        setEnabled(Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get() == YouTubeClient.ClientType.ANDROID_VR);
    }

    public SpoofStreamingDataSignInPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SpoofStreamingDataSignInPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SpoofStreamingDataSignInPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpoofStreamingDataSignInPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Context context = getContext();
        Pair<Dialog, LinearLayout> dialogPair;
        String dialogTitle = str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_title");
        String dialogMessage = str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_message");
        String resetButtonText = str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_reset_text");
        if (YouTubeVRAuthPatch.isDeviceCodeAvailable()) {
            dialogPair = CustomDialog.create(
                    context,
                    // Title.
                    dialogTitle,
                    // Message.
                    dialogMessage,
                    // No EditText.
                    null,
                    // OK button text.
                    str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_get_authorization_token_text"),
                    // OK button action.
                    () -> {
                        YouTubeVRAuthPatch.setRefreshToken();
                        YouTubeVRAuthPatch.setAccessToken(context);
                    },
                    // Cancel button action.
                    null,
                    // Neutral button text.
                    resetButtonText,
                    // Neutral button action.
                    YouTubeVRAuthPatch::clearAll,
                    // Dismiss dialog when onNeutralClick.
                    true
            );
        } else {
            dialogPair = CustomDialog.create(
                    context,
                    // Title.
                    dialogTitle,
                    // Message.
                    dialogMessage,
                    // No EditText.
                    null,
                    // OK button text.
                    str("revanced_spoof_streaming_data_sign_in_android_vr_dialog_get_activation_code_text"),
                    // OK button action.
                    () -> YouTubeVRAuthPatch.setActivationCode(context),
                    // Cancel button action.
                    null,
                    // Neutral button text.
                    resetButtonText,
                    // Neutral button action.
                    YouTubeVRAuthPatch::clearAll,
                    // Dismiss dialog when onNeutralClick.
                    true
            );
        }
        dialogPair.first.show();
        return true;
    }
}