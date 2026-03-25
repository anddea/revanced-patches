package app.morphe.extension.youtube.patches.player;

import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public class OpenVideosFullscreenHookPatch {

    @Nullable
    private static volatile Boolean openNextVideoFullscreen;

    public static void setOpenNextVideoFullscreen(@Nullable Boolean forceFullScreen) {
        openNextVideoFullscreen = forceFullScreen;
    }

    /**
     * Injection point.
     *
     * Returns negated value.
     */
    public static boolean doNotOpenVideoFullscreenPortrait(boolean original) {
        Boolean openFullscreen = openNextVideoFullscreen;
        if (openFullscreen != null) {
            openNextVideoFullscreen = null;
            return !openFullscreen;
        }

        return original;
    }
}
