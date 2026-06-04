package app.morphe.extension.youtube.patches;

import java.util.Map;

import app.morphe.extension.shared.Logger;

@SuppressWarnings("unused")
public class FixContentProviderPatch {

    /**
     * Injection point.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addNullMapEntry(Map map) {
        map.put("revanced_content_provider_null_value_repro", null);
    }

    /**
     * Injection point.
     */
    public static void removeNullMapEntries(Map<?, ?> map) {
        map.entrySet().removeIf(entry -> {
            Object value = entry.getValue();
            if (value == null) {
                Logger.printDebug(() -> "Removing content provider key with null value: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
}
