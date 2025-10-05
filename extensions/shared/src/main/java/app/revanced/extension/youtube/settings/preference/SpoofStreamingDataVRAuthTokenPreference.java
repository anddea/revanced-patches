package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;

import app.revanced.extension.shared.patches.auth.AuthPatch;
import app.revanced.extension.shared.ui.CustomDialog;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"FieldCanBeLocal", "deprecation", "unused"})
public class SpoofStreamingDataVRAuthTokenPreference extends Preference implements Preference.OnPreferenceClickListener {

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
        setEnabled(Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("ANDROID_VR"));
    }

    public SpoofStreamingDataVRAuthTokenPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SpoofStreamingDataVRAuthTokenPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SpoofStreamingDataVRAuthTokenPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpoofStreamingDataVRAuthTokenPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Context context = getContext();
        Pair<Dialog, LinearLayout> dialogPair;
        String dialogTitle = str("revanced_spoof_streaming_data_vr_auth_token_dialog_title");
        String dialogMessage = str("revanced_spoof_streaming_data_vr_auth_token_dialog_message");
        String resetButtonText = str("revanced_spoof_streaming_data_vr_auth_token_dialog_reset_text");
        if (AuthPatch.isDeviceCodeAvailable()) {
            dialogPair = CustomDialog.create(
                    context,
                    // Title.
                    dialogTitle,
                    // Message.
                    dialogMessage,
                    // No EditText.
                    null,
                    // OK button text.
                    str("revanced_spoof_streaming_data_vr_auth_token_dialog_get_authorization_token_text"),
                    // OK button action.
                    () -> {
                        AuthPatch.setRefreshToken();
                        AuthPatch.setAccessToken(true);
                    },
                    // Cancel button action.
                    null,
                    // Neutral button text.
                    resetButtonText,
                    // Neutral button action.
                    AuthPatch::clearAll,
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
                    str("revanced_spoof_streaming_data_vr_auth_token_dialog_get_activation_code_text"),
                    // OK button action.
                    () -> AuthPatch.setActivationCode(context),
                    // Cancel button action.
                    null,
                    // Neutral button text.
                    resetButtonText,
                    // Neutral button action.
                    AuthPatch::clearAll,
                    // Dismiss dialog when onNeutralClick.
                    true
            );
        }
        dialogPair.first.show();
        return true;
    }
}