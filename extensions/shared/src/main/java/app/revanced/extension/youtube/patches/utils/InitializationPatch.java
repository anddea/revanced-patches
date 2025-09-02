package app.revanced.extension.youtube.patches.utils;

import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.*;

import android.app.Activity;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.text.Html;
import android.text.Spanned;
import android.util.Pair;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class InitializationPatch {
    private static final BooleanSetting SETTINGS_INITIALIZED = BaseSettings.SETTINGS_INITIALIZED;

    /**
     * Some layouts that depend on litho do not load when the app is first installed.
     * (Also reproduced on unPatched YouTube)
     * <p>
     * To fix this, show the restart dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (!SETTINGS_INITIALIZED.get()) {
            String rvxSettingsLabel = str("revanced_extended_settings_title");
            String spoofMessage = str("revanced_spoof_streaming_data_message");
            String finalSpoofMessage = String.format(spoofMessage, rvxSettingsLabel);

            Spanned formattedMessage = Html.fromHtml(finalSpoofMessage, Html.FROM_HTML_MODE_LEGACY);

            Activity context = getActivity();

            runOnMainThreadDelayed(() -> {
                Pair<Dialog, LinearLayout> dialogPair = createCustomDialog(
                        context,
                        str("revanced_external_downloader_not_installed_dialog_title"), // Title.
                        formattedMessage,             // Message.
                        null,                         // No EditText.
                        null,                         // OK button text.
                        () -> {},                     // OK action
                        null,                         // No Cancel button action.
                        null,                         // No Neutral button text.
                        null,                         // No Neutral button action.
                        true                          // Dismiss dialog when onNeutralClick.
                );

                Dialog dialog = dialogPair.first;
                LinearLayout mainLayout = dialogPair.second;

                // Add icon to the dialog.
                ImageView iconView = new ImageView(context);
                iconView.setImageResource(Utils.getResourceIdentifier("revanced_ic_dialog_alert", "drawable"));
                iconView.setColorFilter(BaseThemeUtils.getAppForegroundColor(), PorterDuff.Mode.SRC_IN);
                iconView.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                iconParams.gravity = Gravity.CENTER;
                mainLayout.addView(iconView, 0);

                dialog.setCanceledOnTouchOutside(false);
                showDialog(context, dialog);
            }, 1000);

            runOnMainThreadDelayed(() -> showRestartDialog(mActivity, str("revanced_extended_restart_first_run"), 3500), 500);
            runOnMainThreadDelayed(() -> SETTINGS_INITIALIZED.save(true), 1000);
        }
    }
}
