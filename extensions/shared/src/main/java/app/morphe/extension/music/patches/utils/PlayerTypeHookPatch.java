package app.morphe.extension.music.patches.utils;

import androidx.annotation.Nullable;

import app.morphe.extension.music.shared.PlayerType;

@SuppressWarnings("unused")
public class PlayerTypeHookPatch {
    /**
     * Injection point.
     */
    public static void setPlayerType(@Nullable Enum<?> musicPlayerType) {
        if (musicPlayerType == null)
            return;

        PlayerType.setFromString(musicPlayerType.name());
    }
}

