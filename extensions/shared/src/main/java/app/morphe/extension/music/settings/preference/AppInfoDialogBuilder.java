package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.shared.patches.PatchStatus.PatchVersion;
import static app.morphe.extension.shared.patches.PatchStatus.PatchedTime;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;

import java.util.Date;
import java.util.Locale;

import app.morphe.extension.shared.settings.preference.WebViewDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.utils.ExtendedUtils;

/**
 * Used by YouTube and YouTube Music.
 */
public class AppInfoDialogBuilder {

    public static void showDialog(Activity mActivity) {
        try {
            final String backgroundColorHex = BaseThemeUtils.getBackgroundColorHexString();
            final String foregroundColorHex = BaseThemeUtils.getForegroundColorHexString();

            long patchedTime = PatchedTime();
            Date date = new Date(patchedTime);

            final String htmlDialog = "<html>" +
                    "<body style=\"padding: 15px;\"><p>" +
                    String.format(
                            Locale.ENGLISH,
                            "<style> body { background-color: %s; color: %s; line-height: 20px; } a { color: %s; } </style>",
                            backgroundColorHex, foregroundColorHex, foregroundColorHex) +
                    "<h2>" +
                    str("revanced_app_info_dialog_title") +
                    "</h2>" +
                    String.format(
                            str("revanced_app_info_dialog_message"),
                            ExtendedUtils.getAppLabel(),
                            ExtendedUtils.getAppVersionName(),
                            PatchVersion(),
                            date.toLocaleString()
                    ) +
                    "</p></body></html>";

            Utils.runOnMainThreadNowOrLater(() -> {
                WebViewDialog webViewDialog = new WebViewDialog(mActivity, htmlDialog);
                webViewDialog.show();
            });
        } catch (Exception ex) {
            Logger.printException(() -> "dialogBuilder failure", ex);
        }
    }
}