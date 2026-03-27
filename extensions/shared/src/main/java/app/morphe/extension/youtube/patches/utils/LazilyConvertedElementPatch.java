package app.morphe.extension.youtube.patches.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@SuppressWarnings("unused")
public class LazilyConvertedElementPatch {
    private static final String LAZILY_CONVERTED_ELEMENT = "LazilyConvertedElement";

    public static void hookElements(@Nullable List<Object> list, @Nullable String identifier) {
        if (StringUtils.isNotEmpty(identifier) &&
                CollectionUtils.isNotEmpty(list) &&
                LAZILY_CONVERTED_ELEMENT.equals(list.get(0).toString())
        ) {
            hookElementList(list, identifier);
        }
    }

    private static void hookElementList(@NonNull List<Object> list, @NonNull String identifier) {
    }
}


