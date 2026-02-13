package app.morphe.extension.youtube.patches.components;

import java.util.concurrent.atomic.AtomicBoolean;

import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.PlayerType;

/**
 * Here is an unintended behavior:
 * <p>
 * 1. The user does not hide Shorts in the Subscriptions tab, but hides them otherwise.
 * 2. Goes to the Subscriptions tab and scrolls to where Shorts is.
 * 3. Opens a regular video.
 * 4. Minimizes the video and turns off the screen.
 * 5. Turns the screen on and maximizes the video.
 * 6. Shorts belonging to related videos are not hidden.
 * <p>
 * Here is an explanation of this special issue:
 * <p>
 * When the user minimizes the video, turns off the screen, and then turns it back on,
 * the components below the player are reloaded, and at this moment the PlayerType is [WATCH_WHILE_MINIMIZED].
 * (Shorts belonging to related videos are also reloaded)
 * Since the PlayerType is [WATCH_WHILE_MINIMIZED] at this moment, the navigation tab is checked.
 * (Even though PlayerType is [WATCH_WHILE_MINIMIZED], this is a Shorts belonging to a related video)
 * <p>
 * As a workaround for this special issue, if a video actionbar is detected, which is one of the components below the player,
 * it is treated as being in the same state as [WATCH_WHILE_MAXIMIZED].
 */
@SuppressWarnings("unused")
public final class LayoutReloadObserverFilter extends Filter {
    // Must be volatile or synchronized, as litho filtering runs off main thread and this field is then access from the main thread.
    public static final AtomicBoolean isActionBarVisible = new AtomicBoolean(false);

    public LayoutReloadObserverFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        null,
                        "video_action_bar."
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (PlayerType.getCurrent() == PlayerType.WATCH_WHILE_MINIMIZED &&
                isActionBarVisible.compareAndSet(false, true)) {
            Utils.runOnMainThreadDelayed(() -> isActionBarVisible.compareAndSet(true, false), 1000);
        }

        return false;
    }
}
