package app.revanced.extension.shared.patches.spans;

import androidx.annotation.NonNull;

public enum SpanType {
    CLICKABLE("ClickableSpan"),
    FOREGROUND_COLOR("ForegroundColorSpan"),
    ABSOLUTE_SIZE("AbsoluteSizeSpan"),
    TYPEFACE("TypefaceSpan"),
    IMAGE("ImageSpan"),
    CUSTOM_CHARACTER_STYLE("CustomCharacterStyle"),
    UNKNOWN("Unknown");

    @NonNull
    public final String type;

    SpanType(@NonNull String type) {
        this.type = type;
    }
}
