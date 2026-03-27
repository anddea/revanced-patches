package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.music.utils.ExtendedUtils.getLayoutParams;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class SponsorBlockApiUrlPreference {

    public static void showDialog(Activity mActivity) {
        try {
            final StringSetting apiUrl = Settings.SB_API_URL;

            final EditText textView = new EditText(mActivity);
            textView.setText(apiUrl.get());

            TextInputLayout textInputLayout = new TextInputLayout(mActivity);
            textInputLayout.setLayoutParams(getLayoutParams());
            textInputLayout.addView(textView);

            FrameLayout container = new FrameLayout(mActivity);
            container.addView(textInputLayout);

            getDialogBuilder(mActivity)
                    .setTitle(str("revanced_sb_api_url"))
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(str("revanced_settings_reset"), (dialog, which) -> {
                        apiUrl.resetToDefault();
                        Utils.showToastShort(str("revanced_sb_api_url_reset"));
                    })
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        String serverAddress = textView.getText().toString().trim();
                        if (!isValidSBServerAddress(serverAddress)) {
                            Utils.showToastShort(str("revanced_sb_api_url_invalid"));
                        } else if (!serverAddress.equals(Settings.SB_API_URL.get())) {
                            apiUrl.save(serverAddress);
                            Utils.showToastShort(str("revanced_sb_api_url_changed"));
                        }
                    })
                    .show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }

    public static boolean isValidSBServerAddress(@NonNull String serverAddress) {
        if (!Patterns.WEB_URL.matcher(serverAddress).matches()) {
            return false;
        }
        // Verify url is only the server address and does not contain a path such as: "https://sponsor.ajay.app/api/"
        // Could use Patterns.compile, but this is simpler
        final int lastDotIndex = serverAddress.lastIndexOf('.');
        return lastDotIndex == -1 || !serverAddress.substring(lastDotIndex).contains("/");
    }

}
