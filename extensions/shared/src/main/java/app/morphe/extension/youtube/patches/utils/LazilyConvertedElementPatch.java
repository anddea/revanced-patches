package app.morphe.extension.youtube.patches.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@SuppressWarnings("unused")
public class LazilyConvertedElementPatch {
    private static final String COMPONENT_TYPE = "ComponentType";
    private static final String LAZILY_CONVERTED_ELEMENT = "LazilyConvertedElement";

    public static void hookElements(@Nullable List<Object> list, @Nullable String identifier) {
        if (StringUtils.isNotEmpty(identifier) &&
                CollectionUtils.isNotEmpty(list) &&
                LAZILY_CONVERTED_ELEMENT.equals(list.get(0).toString())
        ) {
            hookElementList(list, identifier);
        }
    }

    public static void hookElements(@Nullable List<Object> list,
                                    @Nullable String identifier,
                                    @Nullable StringBuilder pathBuilder) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        String firstElement = list.get(0).toString();
        if (COMPONENT_TYPE.equals(firstElement) && pathBuilder != null && pathBuilder.length() > 0) {
            hookComponentList(pathBuilder.toString(), list);
        } else if (LAZILY_CONVERTED_ELEMENT.equals(firstElement) && StringUtils.isNotEmpty(identifier)) {
            hookElementList(list, identifier);
        }
    }

    private static void hookComponentList(@NonNull String path, @NonNull List<Object> list) {
    }

    private static void hookElementList(@NonNull List<Object> list, @NonNull String identifier) {
    }
}
