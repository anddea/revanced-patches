package app.morphe.extension.youtube.shared;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import app.morphe.extension.shared.utils.Event;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public final class EngagementPanel {
    private static final AtomicReference<String> engagementPanelId = new AtomicReference<>("");
    private static final Event<String> onChange = new Event<>();

    /**
     * Injection point.
     */
    public static void setId(@Nullable String panelId) {
        if (panelId != null && !panelId.equals(getId())) {
            Logger.printDebug(() -> "engagementPanel open\npanelId: " + panelId);
            engagementPanelId.set(panelId);
            // Notify after updating the ID so observers can safely query the new state.
            onChange.invoke(panelId);
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
            onChange.invoke("");
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

    /**
     * Notifies observers after an engagement panel opens, changes, or closes.
     * The value is the active panel ID, or an empty string after the panel closes.
     */
    public static Event<String> getOnChange() {
        return onChange;
    }

}
