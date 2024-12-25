package app.revanced.extension.shared.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

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
                    "<body style=\"padding: 10px;\"><p>" +
                    String.format(
                            "<style> body { background-color: %s; color: %s; } a { color: %s; } </style>",
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

                final Window window = webViewDialog.getWindow();
                if (window == null) return;
                Display display = mActivity.getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                WindowManager.LayoutParams params = window.getAttributes();
                params.height = (int) (size.y * 0.6);

                window.setAttributes(params);
            });
        } catch (Exception ex) {
            Logger.printException(() -> "dialogBuilder failure", ex);
        }
    }
}