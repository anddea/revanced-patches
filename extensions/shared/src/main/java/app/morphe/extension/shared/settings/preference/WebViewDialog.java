package app.morphe.extension.shared.settings.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Displays html content as a dialog. Any links a user taps on are opened in an external browser.
 */
@SuppressWarnings("deprecation")
public class WebViewDialog extends Dialog {

    private final String htmlContent;

    public WebViewDialog(@NonNull Context context, @NonNull String htmlContent) {
        super(context);
        this.htmlContent = htmlContent;
    }

    // JS required to hide any broken images. No remote javascript is ever loaded.
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default title bar.

        // Create WebView.
        WebView webView = new WebView(getContext());
        webView.setVerticalScrollBarEnabled(false); // Disable the vertical scrollbar.
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new OpenLinksExternallyWebClient());
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);

        if (!BaseThemeUtils.isSupportModernDialog) {
            // Add WebView to layout.
            setContentView(webView);
            return;
        }

        // Create main layout.
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        final int padding = Utils.dipToPixels(10);
        mainLayout.setPadding(padding, padding, padding, padding);
        // Set rounded rectangle background.
        ShapeDrawable mainBackground = new ShapeDrawable(new RoundRectShape(
                Utils.createCornerRadii(28), null, null));
        mainBackground.getPaint().setColor(BaseThemeUtils.getDialogBackgroundColor());
        mainLayout.setBackground(mainBackground);

        // Add WebView to layout.
        mainLayout.addView(webView);

        setContentView(mainLayout);

        // Set dialog window attributes
        Window window = getWindow();
        if (window != null) {
            Utils.setDialogWindowParameters(window, Gravity.CENTER, 0, 90, false);
        }
    }

    private class OpenLinksExternallyWebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(intent);
            } catch (Exception ex) {
                Logger.printException(() -> "Open link failure", ex);
            }
            // Dismiss the about dialog using a delay,
            // otherwise without a delay the UI looks hectic with the dialog dismissing
            // to show the settings while simultaneously a web browser is opening.
            Utils.runOnMainThreadDelayed(WebViewDialog.this::dismiss, 500);
            return true;
        }
    }
}