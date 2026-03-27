package app.morphe.extension.shared.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;

import java.util.Locale;

import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Used by YouTube and YouTube Music.
 */
public class YouTubeDataAPIDialogBuilder {
    private static final String URL_CREATE_PROJECT = "https://console.cloud.google.com/projectcreate";
    private static final String URL_MARKET_PLACE = "https://console.cloud.google.com/marketplace/product/google/youtube.googleapis.com";

    public static void showDialog(Activity mActivity) {
        try {
            final String backgroundColorHex = BaseThemeUtils.getBackgroundColorHexString();
            final String foregroundColorHex = BaseThemeUtils.getForegroundColorHexString();

            final String htmlDialog = "<html>" +
                    "<body style=\"padding: 15px;\"><p>" +
                    String.format(
                            Locale.ENGLISH,
                            "<style> body { background-color: %s; color: %s; line-height: 20px; } a { color: %s; } </style>",
                            backgroundColorHex, foregroundColorHex, foregroundColorHex) +
                    "<h2>" +
                    str("revanced_return_youtube_username_youtube_data_api_v3_dialog_title") +
                    "</h2>" +
                    String.format(
                            str("revanced_return_youtube_username_youtube_data_api_v3_dialog_message"),
                            URL_CREATE_PROJECT,
                            URL_MARKET_PLACE
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