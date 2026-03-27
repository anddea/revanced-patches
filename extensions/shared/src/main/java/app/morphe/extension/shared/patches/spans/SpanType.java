package app.morphe.extension.shared.patches.spans;

import androidx.annotation.NonNull;

public enum SpanType {
    ABSOLUTE_SIZE("AbsoluteSizeSpan"),
    CLICKABLE("ClickableSpan"),
    CUSTOM_CHARACTER_STYLE("CustomCharacterStyle"),
    FOREGROUND_COLOR("ForegroundColorSpan"),
    IMAGE("ImageSpan"),
    LINE_HEIGHT("LineHeightSpan"),
    TYPEFACE("TypefaceSpan"),
    UNKNOWN("Unknown");

    @NonNull
    public final String type;

    SpanType(@NonNull String type) {
        this.type = type;
    }
}
