package app.morphe.extension.shared.patches.spans;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LineHeightSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;

import java.util.List;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.StringTrieSearch;


/**
 * Placeholder for actual filters.
 */
final class DummyFilter extends Filter {
}

@SuppressWarnings("unused")
public final class InclusiveSpanPatch {

    /**
     * Simple wrapper to pass the litho parameters through the prefix search.
     */
    private static final class LithoFilterParameters {
        final String conversionContext;
        final SpannableString spannableString;
        final Object span;
        final int start;
        final int end;
        final int flags;
        final String originalString;
        final int originalLength;
        final SpanType spanType;
        final boolean isWord;

        public LithoFilterParameters(String conversionContext, SpannableString spannableString,
                                     Object span, int start, int end, int flags) {
            this.conversionContext = conversionContext;
            this.spannableString = spannableString;
            this.span = span;
            this.start = start;
            this.end = end;
            this.flags = flags;
            this.originalString = spannableString.toString();
            this.originalLength = spannableString.length();
            this.spanType = getSpanType(span);
            this.isWord = !(start == 0 && end == originalLength);
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CharSequence:'")
                    .append(originalString)
                    .append("'\nSpanType:'")
                    .append(getSpanType(spanType, span))
                    .append("'\nLength:'")
                    .append(originalLength)
                    .append("'\nStart:'")
                    .append(start)
                    .append("'\nEnd:'")
                    .append(end)
                    .append("'\nisWord:'")
                    .append(isWord)
                    .append("'");
            if (isWord) {
                builder.append("\nWord:'")
                        .append(originalString.substring(start, end))
                        .append("'");
            }
            return builder.toString();
        }
    }

    private static SpanType getSpanType(Object span) {
        if (span instanceof ClickableSpan) {
            return SpanType.CLICKABLE;
        } else if (span instanceof ForegroundColorSpan) {
            return SpanType.FOREGROUND_COLOR;
        } else if (span instanceof AbsoluteSizeSpan) {
            return SpanType.ABSOLUTE_SIZE;
        } else if (span instanceof TypefaceSpan) {
            return SpanType.TYPEFACE;
        } else if (span instanceof ImageSpan) {
            return SpanType.IMAGE;
        } else if (span instanceof LineHeightSpan) {
            return SpanType.LINE_HEIGHT;
        } else if (span instanceof CharacterStyle) { // Replaced by patch.
            return SpanType.CUSTOM_CHARACTER_STYLE;
        } else {
            return SpanType.UNKNOWN;
        }
    }

    private static String getSpanType(SpanType spanType, Object span) {
        return spanType == SpanType.UNKNOWN
                ? span.getClass().getSimpleName()
                : spanType.type;
    }

    private static final Filter[] filters = new Filter[]{
            new DummyFilter() // Replaced by patch.
    };

    private static final StringTrieSearch searchTree = new StringTrieSearch();


    /**
     * Because litho filtering is multi-threaded and the buffer is passed in from a different injection point,
     * the buffer is saved to a ThreadLocal so each calling thread does not interfere with other threads.
     */
    private static final ThreadLocal<String> conversionContextThreadLocal = new ThreadLocal<>();

    static {
        for (Filter filter : filters) {
            filterUsingCallbacks(filter, filter.callbacks);
        }

        if (BaseSettings.DEBUG_SPANNABLE.get()) {
            Logger.printDebug(() -> "Using: "
                    + searchTree.numberOfPatterns() + " conversion context filters"
                    + " (" + searchTree.getEstimatedMemorySize() + " KB)");
        }
    }

    private static void filterUsingCallbacks(Filter filter, List<StringFilterGroup> groups) {
        String filterSimpleName = filter.getClass().getSimpleName();

        for (StringFilterGroup group : groups) {
            if (!group.includeInSearch()) {
                continue;
            }

            for (String pattern : group.filters) {
                InclusiveSpanPatch.searchTree.addPattern(pattern, (textSearched, matchedStartIndex,
                                                                   matchedLength, callbackParameter) -> {
                            if (!group.isEnabled()) return false;

                            LithoFilterParameters parameters = (LithoFilterParameters) callbackParameter;
                            final boolean isFiltered = filter.skip(parameters.conversionContext, parameters.spannableString,
                                    parameters.span, parameters.start, parameters.end, parameters.flags, parameters.isWord,
                                    parameters.spanType, group);

                            if (isFiltered && BaseSettings.DEBUG_SPANNABLE.get()) {
                                Logger.printDebug(() -> "Removed " + filterSimpleName
                                        + " setSpan: " + parameters.spanType);
                            }

                            return isFiltered;
                        }
                );
            }
        }
    }

    /**
     * Injection point.
     *
     * @param conversionContext ConversionContext is used to identify whether it is a comment thread or not.
     */
    public static CharSequence setConversionContext(@NonNull Object conversionContext,
                                                    @NonNull CharSequence original) {
        conversionContextThreadLocal.set(conversionContext.toString());
        return original;
    }

    private static boolean returnEarly(SpannableString spannableString, Object span, int start, int end, int flags) {
        try {
            final String conversionContext = conversionContextThreadLocal.get();
            if (conversionContext == null || conversionContext.isEmpty()) {
                return false;
            }

            LithoFilterParameters parameter =
                    new LithoFilterParameters(conversionContext, spannableString, span, start, end, flags);

            if (BaseSettings.DEBUG_SPANNABLE.get()) {
                Logger.printDebug(() -> "Searching...\n\u200B\n" + parameter);
            }

            return searchTree.matches(parameter.conversionContext, parameter);
        } catch (Exception ex) {
            Logger.printException(() -> "Spans filter failure", ex);
        }

        return false;
    }

    /**
     * Injection point.
     *
     * @param spannableString Original SpannableString.
     * @param span            Span such as {@link ClickableSpan}, {@link ForegroundColorSpan},
     *                        {@link AbsoluteSizeSpan}, {@link TypefaceSpan}, {@link ImageSpan}.
     * @param start           Start index of {@link Spannable#setSpan(Object, int, int, int)}.
     * @param end             End index of {@link Spannable#setSpan(Object, int, int, int)}.
     * @param flags           Flags of {@link Spannable#setSpan(Object, int, int, int)}.
     */
    public static void setSpan(SpannableString spannableString, Object span, int start, int end, int flags) {
        if (returnEarly(spannableString, span, start, end, flags)) {
            return;
        }
        spannableString.setSpan(span, start, end, flags);
    }
}