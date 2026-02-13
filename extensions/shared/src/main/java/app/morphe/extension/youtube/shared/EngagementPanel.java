package app.morphe.extension.youtube.shared;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public final class EngagementPanel {
    private static final AtomicReference<String> engagementPanelId = new AtomicReference<>("");

    /**
     * Injection point.
     */
    public static void setId(@Nullable String panelId) {
        if (panelId != null) {
            Logger.printDebug(() -> "engagementPanel open\npanelId: " + panelId);
            engagementPanelId.set(panelId);
        }
    }

    /**
     * Injection point.
     */
    public static void hide() {
        String panelId = getId();
        if (!panelId.isEmpty()) {
            Logger.printDebug(() -> "engagementPanel closed\npanelId: " + panelId);
            engagementPanelId.set("");
        }
    }

    public static boolean isOpen() {
        return !getId().isEmpty();
    }

    public static boolean isDescription() {
        return getId().equals("video-description-ep-identifier");
    }

    public static String getId() {
        return engagementPanelId.get();
    }

}
