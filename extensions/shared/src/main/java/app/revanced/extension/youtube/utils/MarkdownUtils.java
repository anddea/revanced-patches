package app.revanced.extension.youtube.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.*;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.ReplacementSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.revanced.extension.shared.utils.Utils.dipToPixels;

/**
 * Utility class to convert Markdown-formatted strings into Android {@link Spanned} objects.
 * <p>
 * Supported syntax:
 * <ul>
 * <li><b>Headers:</b> {@code # Header} (Levels 1-6)</li>
 * <li><b>Bold:</b> {@code **text**}</li>
 * <li><b>Italic:</b> {@code *text*}</li>
 * <li><b>Monospace/Code:</b> {@code `code`} (Rendered as a pill-shaped snippet)</li>
 * <li><b>Lists:</b> {@code - Item} or {@code * Item} at the start of a line</li>
 * </ul>
 */
public class MarkdownUtils {
    // Regex patterns for standard Markdown
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?m)^(#{1,6})\\s+(.+)$"); // headers, 1-6 '#' followed by space
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*"); // **bold**
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"); // *italic*
    private static final Pattern MONOSPACE_PATTERN = Pattern.compile("`(.+?)`"); // `code`
    private static final Pattern LIST_PATTERN = Pattern.compile("(?m)^\\s*[-*]\\s+(.+)$"); // list bullets "- " or "* " at start of line

    /**
     * Parses a Markdown string into an Android Spanned object with enhanced styling.
     *
     * @param text The raw Markdown string.
     * @return A {@link Spanned} object suitable for TextView.setText().
     */
    public static Spanned fromMarkdown(String text) {
        if (TextUtils.isEmpty(text)) return Html.fromHtml("", Html.FROM_HTML_MODE_LEGACY);

        String html = TextUtils.htmlEncode(text);

        // Headers: # Title -> <h1>Title</h1>
        StringBuffer headerBuffer = getStringBuffer(html);
        html = headerBuffer.toString();

        // Bold: **text** -> <b>text</b>
        Matcher boldMatcher = BOLD_PATTERN.matcher(html);
        html = boldMatcher.replaceAll("<b>$1</b>");

        // Italic: *text* -> <i>text</i>
        Matcher italicMatcher = ITALIC_PATTERN.matcher(html);
        html = italicMatcher.replaceAll("<i>$1</i>");

        // Monospace: `text` -> <tt>text</tt> (used as a hook for InlineCodeSpan)
        Matcher monoMatcher = MONOSPACE_PATTERN.matcher(html);
        html = monoMatcher.replaceAll("<tt>$1</tt>");

        // Lists: - Item -> <li>Item</li> (Bullet point)
        Matcher listMatcher = LIST_PATTERN.matcher(html);
        html = listMatcher.replaceAll("<li>$1</li>");

        // Remove newlines after block elements to prevent double padding before converting remaining \n to <br>
        html = html.replaceAll("</li>\\n", "</li>").replaceAll("</h\\d>\\n", "");

        // Newlines to <br>
        html = html.replace("\n", "<br/>");

        Spanned rawSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        return enhanceMarkdownSpans(rawSpanned);
    }

    @NotNull
    private static StringBuffer getStringBuffer(String html) {
        Matcher headerMatcher = HEADER_PATTERN.matcher(html);
        StringBuffer headerBuffer = new StringBuffer();

        while (headerMatcher.find()) {
            int level = Objects.requireNonNull(headerMatcher.group(1)).length();
            String content = headerMatcher.group(2);
            headerMatcher.appendReplacement(headerBuffer, "<h" + level + ">" + content + "</h" + level + ">");
        }
        headerMatcher.appendTail(headerBuffer);

        return headerBuffer;
    }

    /**
     * Post-processes the Spanned object to apply custom styling like pill-shaped code backgrounds
     * and consistent bullet point margins.
     */
    private static Spanned enhanceMarkdownSpans(Spanned original) {
        if (original == null) return null;

        SpannableStringBuilder ssb = new SpannableStringBuilder(original);

        // Enhance monospace tags into pill-shaped code snippets
        TypefaceSpan[] ttSpans = ssb.getSpans(0, ssb.length(), TypefaceSpan.class);
        for (TypefaceSpan span : ttSpans) {
            if ("monospace".equals(span.getFamily())) {
                int start = ssb.getSpanStart(span);
                int end = ssb.getSpanEnd(span);
                ssb.removeSpan(span);
                ssb.setSpan(new InlineCodeSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Enhance bullet spans
        BulletSpan[] bulletSpans = ssb.getSpans(0, ssb.length(), BulletSpan.class);
        int gapWidth = dipToPixels(6);
        int bulletRadius = dipToPixels(3);
        int bulletColor = ThemeUtils.getAppForegroundColor();

        for (BulletSpan span : bulletSpans) {
            int start = ssb.getSpanStart(span);
            int end = ssb.getSpanEnd(span);
            ssb.removeSpan(span);
            ssb.setSpan(new CustomBulletSpan(gapWidth, gapWidth, bulletRadius, bulletColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }

    /**
     * Custom span to render inline code with a rounded background "pill".
     */
    private static class InlineCodeSpan extends ReplacementSpan {
        private static final int BACKGROUND_COLOR = Color.parseColor("#33888888"); // Translucent gray
        private static final int PADDING_HORIZONTAL = dipToPixels(6);
        private static final int CORNER_RADIUS = dipToPixels(4);

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
            return (int) (paint.measureText(text, start, end) + (PADDING_HORIZONTAL * 2));
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            float width = paint.measureText(text, start, end);
            RectF rect = new RectF(x, top, x + width + (PADDING_HORIZONTAL * 2), bottom);

            // Draw pill background
            int originalColor = paint.getColor();
            paint.setColor(BACKGROUND_COLOR);
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint);

            // Draw text
            paint.setColor(originalColor);
            canvas.drawText(text, start, end, x + PADDING_HORIZONTAL, y, paint);
        }
    }

    /**
     * Custom span for bullets with specific start padding and text gap.
     */
    private record CustomBulletSpan(int startPadding, int gapWidth, int bulletRadius,
                                    int color) implements LeadingMarginSpan {
        @Override
        public int getLeadingMargin(boolean first) {
            return startPadding + (2 * bulletRadius) + gapWidth;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout l) {
            if (((Spanned) text).getSpanStart(this) == start) {
                Paint.Style style = p.getStyle();
                int oldColor = p.getColor();

                p.setColor(color);
                p.setStyle(Paint.Style.FILL);

                // Calculate center of the bullet: startPadding + radius
                float cx = x + dir * (startPadding + bulletRadius);
                float cy = (top + bottom) / 2.0f;
                c.drawCircle(cx, cy, bulletRadius, p);

                p.setColor(oldColor);
                p.setStyle(style);
            }
        }
    }
}
