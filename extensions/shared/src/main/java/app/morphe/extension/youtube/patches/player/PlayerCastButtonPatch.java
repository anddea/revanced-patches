package app.morphe.extension.youtube.patches.player;

import android.view.View;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class PlayerCastButtonPatch {

    private PlayerCastButtonPatch() {
    }

    /**
     * Injection point.
     */
    public static int hideCastButton(int original) {
        return Settings.HIDE_PLAYER_CAST_BUTTON.get() ? View.GONE : original;
    }

    /**
     * Injection point.
     */
    public static boolean getCastButtonOverride(boolean original) {
        if (Settings.HIDE_PLAYER_CAST_BUTTON.get()) {
            return false;
        }

        return original;
    }
}
