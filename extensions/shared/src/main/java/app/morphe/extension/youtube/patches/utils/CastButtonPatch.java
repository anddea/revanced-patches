package app.morphe.extension.youtube.patches.utils;

import android.view.View;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class CastButtonPatch {

    /**
     * The [Hide cast button] setting is separated into the [Hide cast button in player] setting and the [Hide cast button in toolbar] setting.
     * Always hide the cast button when both settings are true.
     * <p>
     * These two settings belong to different patches, and since the default value for this setting is true,
     * it is essential to ensure that each patch is included to ensure independent operation.
     */
    public static int hideCastButton(int original) {
        return Settings.HIDE_TOOLBAR_CAST_BUTTON.get()
                && PatchStatus.ToolBarComponents()
                && Settings.HIDE_PLAYER_CAST_BUTTON.get()
                && PatchStatus.PlayerButtons()
                ? View.GONE
                : original;
    }
}
