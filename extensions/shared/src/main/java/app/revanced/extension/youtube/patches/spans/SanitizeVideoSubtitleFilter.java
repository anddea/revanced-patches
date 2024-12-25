package app.revanced.extension.youtube.patches.spans;

import android.text.SpannableString;

import app.revanced.extension.shared.patches.spans.Filter;
import app.revanced.extension.shared.patches.spans.SpanType;
import app.revanced.extension.shared.patches.spans.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "ConstantValue", "FieldCanBeLocal"})
public final class SanitizeVideoSubtitleFilter extends Filter {

    public SanitizeVideoSubtitleFilter() {
        addCallbacks(
                new StringFilterGroup(
                        Settings.SANITIZE_VIDEO_SUBTITLE,
                        "|video_subtitle.eml|"
                )
        );
    }

    @Override
    public boolean skip(String conversionContext, SpannableString spannableString, Object span,
                        int start, int end, int flags, boolean isWord, SpanType spanType, StringFilterGroup matchedGroup) {
        if (isWord) {
            if (spanType == SpanType.IMAGE) {
                hideImageSpan(spannableString, start, end, flags);
                return super.skip(conversionContext, spannableString, span, start, end, flags, isWord, spanType, matchedGroup);
            } else if (spanType == SpanType.CUSTOM_CHARACTER_STYLE) {
                hideSpan(spannableString, start, end, flags);
                return super.skip(conversionContext, spannableString, span, start, end, flags, isWord, spanType, matchedGroup);
            }
        }
        return false;
    }
}
