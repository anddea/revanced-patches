package app.revanced.extension.youtube.patches.spans;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import app.revanced.extension.shared.patches.spans.Filter;
import app.revanced.extension.shared.patches.spans.SpanType;
import app.revanced.extension.shared.patches.spans.StringFilterGroup;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public final class SnackBarFilter extends Filter {
    private static final boolean HIDE_SNACK_BAR =
            Settings.HIDE_SNACK_BAR.get() || Settings.HIDE_SERVER_SIDE_SNACK_BAR.get();
    private static final boolean CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND =
            !HIDE_SNACK_BAR && Settings.CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND.get();
    private static final boolean INVERT_SNACK_BAR_THEME =
            !HIDE_SNACK_BAR && Settings.INVERT_SNACK_BAR_THEME.get();

    private final ForegroundColorSpan foregroundColorSpanBlack =
            new ForegroundColorSpan(ResourceUtils.getColor("yt_black1"));
    private final ForegroundColorSpan foregroundColorSpanWhite =
            new ForegroundColorSpan(ResourceUtils.getColor("yt_white1"));

    public SnackBarFilter() {
        addCallbacks(
                new StringFilterGroup(
                        Settings.CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND,
                        "snackbar.eml|"
                )
        );
    }

    private ForegroundColorSpan getForegroundColorSpan() {
        if (INVERT_SNACK_BAR_THEME) {
            return ThemeUtils.isDarkTheme()
                    ? foregroundColorSpanWhite
                    : foregroundColorSpanBlack;
        }
        return ThemeUtils.isDarkTheme()
                ? foregroundColorSpanBlack
                : foregroundColorSpanWhite;
    }

    @Override
    public boolean skip(String conversionContext, SpannableString spannableString, Object span,
                        int start, int end, int flags, boolean isWord, SpanType spanType, StringFilterGroup matchedGroup) {
        if (CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND && spanType == SpanType.FOREGROUND_COLOR) {
            spannableString.setSpan(
                    getForegroundColorSpan(),
                    start,
                    end,
                    flags
            );
            return super.skip(conversionContext, spannableString, span, start, end, flags, isWord, spanType, matchedGroup);
        }

        return false;
    }
}
