package app.morphe.extension.youtube.utils;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.text.Annotation;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.morphe.extension.shared.utils.Logger;

/**
 * Keeps ordinary messages native and lazily renders math messages using bundled MathJax SVG.
 * JavaScript and font paths are assets, so the patcher never merges the renderer into DEX.
 * The native text remains visible until typesetting succeeds, including on unsupported WebViews.
 */
final class GeminiMathView extends FrameLayout {
    private static final String ORIGIN = "https://gemini.invalid/";
    private static final String ASSET_DIRECTORY = "morphe/gemini/mathjax/";
    private final TextView textView;
    private final Runnable renderPending = this::renderPending;
    private WebView webView;
    private Spanned pendingText;
    private List<ClickableSpan> links = List.of();
    private boolean ready;
    private boolean failed;
    private int revision;

    GeminiMathView(TextView textView) {
        super(textView.getContext());
        this.textView = textView;
        setBackground(textView.getBackground());
        setClipToOutline(true);
        textView.setBackground(null);
        addView(textView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    /** Coalesce streamed updates; incomplete formulas remain ordinary source text. */
    void render(CharSequence text) {
        revision++;
        removeCallbacks(renderPending);
        if (!(text instanceof Spanned spanned) || !hasMath(spanned)) {
            pendingText = null;
            releaseRenderer();
            return;
        }
        pendingText = spanned;
        if (failed) return;
        if (webView == null) createRenderer();
        if (ready) postDelayed(renderPending, 120);
    }

    private static boolean hasMath(Spanned text) {
        for (Annotation annotation : text.getSpans(0, text.length(), Annotation.class)) {
            if (isMath(annotation)) return true;
        }
        return false;
    }

    private static boolean isMath(Annotation annotation) {
        return annotation.getKey().equals("gemini-math") || annotation.getKey().equals("gemini-math-block");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createRenderer() {
        try {
            WebView view = new WebView(getContext()) {
                /** This renderer must never proxy IME connections belonging to the chat EditText. */
                @Override
                public boolean checkInputConnectionProxy(View view) {
                    return false;
                }
            };
            webView = view;
            view.setFocusable(false);
            view.setFocusableInTouchMode(false);
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setVisibility(INVISIBLE);
            view.setVerticalScrollBarEnabled(false);
            view.setHorizontalScrollBarEnabled(false);
            WebSettings settings = view.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setBlockNetworkLoads(true);
            settings.setTextZoom(100);
            view.addJavascriptInterface(new Bridge(view), "GeminiMath");
            view.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    // Serve only the fixed bundled files. All other requests, including TeX URLs,
                    // are denied; model output never gains network or local-file access.
                    if (request.getMethod().equals("GET") && url.startsWith(ORIGIN)) {
                        String name = url.substring(ORIGIN.length());
                        if (name.equals("index.html") || name.equals("renderer.js") || name.equals("tex-svg-full.js")) {
                            try {
                                return new WebResourceResponse(name.endsWith(".js") ? "application/javascript" : "text/html",
                                        "UTF-8", getContext().getAssets().open(ASSET_DIRECTORY + name));
                            } catch (IOException e) {
                                Logger.printException(() -> "Gemini math asset missing", e);
                            }
                        }
                    }
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if (uri.toString().startsWith(ORIGIN + "click/")) {
                        try {
                            int index = Integer.parseInt(uri.getLastPathSegment());
                            if (index >= 0 && index < links.size()) links.get(index).onClick(textView);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    return true;
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    post(() -> failRenderer(view));
                    return true;
                }
            });
            addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, 1));
            view.loadUrl(ORIGIN + "index.html");
        } catch (RuntimeException e) {
            failed = true;
            releaseRenderer();
            Logger.printException(() -> "Gemini math WebView unavailable", e);
        }
    }

    /** Serialize existing Markdown styles and retain native timestamp callbacks through local links. */
    private void renderPending() {
        if (!ready || webView == null || pendingText == null) return;
        try {
            SpannableStringBuilder htmlText = new SpannableStringBuilder(pendingText);
            String marker = "GEMINIMATHPLACEHOLDER";
            while (htmlText.toString().contains(marker)) marker += "X";
            JSONArray formulas = new JSONArray();
            List<String> tokens = new ArrayList<>();
            for (Annotation annotation : pendingText.getSpans(0, pendingText.length(), Annotation.class)) {
                if (!isMath(annotation)) continue;
                int start = htmlText.getSpanStart(annotation);
                int end = htmlText.getSpanEnd(annotation);
                String token = marker + formulas.length() + "END";
                formulas.put(new JSONObject().put("latex", annotation.getValue())
                        .put("block", annotation.getKey().equals("gemini-math-block"))
                        .put("source", htmlText.subSequence(start, end).toString()));
                tokens.add(token);
                htmlText.replace(start, end, token);
                htmlText.removeSpan(annotation);
            }
            List<ClickableSpan> callbacks = new ArrayList<>();
            for (ClickableSpan span : htmlText.getSpans(0, htmlText.length(), ClickableSpan.class)) {
                int start = htmlText.getSpanStart(span);
                int end = htmlText.getSpanEnd(span);
                htmlText.removeSpan(span);
                htmlText.setSpan(new URLSpan(ORIGIN + "click/" + callbacks.size()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                callbacks.add(span);
            }
            for (MarkdownUtils.InlineCodeSpan span : htmlText.getSpans(0, htmlText.length(), MarkdownUtils.InlineCodeSpan.class)) {
                htmlText.setSpan(new TypefaceSpan("monospace"), htmlText.getSpanStart(span), htmlText.getSpanEnd(span),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (MarkdownUtils.CustomBulletSpan span : htmlText.getSpans(0, htmlText.length(), MarkdownUtils.CustomBulletSpan.class)) {
                htmlText.setSpan(new BulletSpan(), htmlText.getSpanStart(span), htmlText.getSpanEnd(span),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            String html = Html.toHtml(htmlText, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
            for (int i = 0; i < tokens.size(); i++) {
                html = html.replace(tokens.get(i), "<span data-math=\"" + i + "\"></span>");
            }
            links = callbacks;
            float density = getResources().getDisplayMetrics().density;
            JSONObject style = new JSONObject()
                    .put("color", String.format(Locale.ROOT, "#%06x", textView.getCurrentTextColor() & 0xffffff))
                    .put("size", textView.getTextSize() / density)
                    .put("padding", new JSONArray(new float[]{textView.getPaddingTop() / density,
                            textView.getPaddingRight() / density, textView.getPaddingBottom() / density,
                            textView.getPaddingLeft() / density}));
            webView.evaluateJavascript("renderMessage(" + JSONObject.quote(html) + "," + formulas + "," + style + "," + revision + ")", null);
        } catch (JSONException | RuntimeException e) {
            Logger.printException(() -> "Gemini math rendering failed", e);
            failRenderer(webView);
        }
    }

    private void failRenderer(WebView owner) {
        if (webView != owner) return;
        failed = true;
        releaseRenderer();
    }

    private void releaseRenderer() {
        ready = false;
        removeCallbacks(renderPending);
        textView.setVisibility(VISIBLE);
        links = List.of();
        if (webView != null) {
            WebView view = webView;
            webView = null;
            removeView(view);
            view.removeJavascriptInterface("GeminiMath");
            view.destroy();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (webView != null) {
            int width = Math.min(MeasureSpec.getSize(widthMeasureSpec), textView.getMaxWidth());
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        render(textView.getText());
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseRenderer();
        super.onDetachedFromWindow();
    }

    /** Only trusted bundled scripts can access this bridge. All view changes run on the UI thread. */
    private final class Bridge {
        private final WebView owner;

        Bridge(WebView owner) {
            this.owner = owner;
        }

        @JavascriptInterface
        public void ready() {
            post(() -> {
                if (webView != owner) return;
                ready = true;
                renderPending();
            });
        }

        @JavascriptInterface
        public void rendered(int renderedRevision, float height) {
            post(() -> {
                if (webView != owner || revision != renderedRevision || !Float.isFinite(height) || height <= 0) return;
                int pixels = (int) Math.ceil(height * getResources().getDisplayMetrics().density);
                if (owner.getLayoutParams().height != pixels) {
                    owner.getLayoutParams().height = pixels;
                    owner.requestLayout();
                }
                textView.setVisibility(GONE);
                owner.setVisibility(VISIBLE);
            });
        }

        @JavascriptInterface
        public void failed() {
            post(() -> failRenderer(owner));
        }
    }
}
