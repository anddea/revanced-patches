package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {

    public static boolean allowBackgroundPlayback(boolean original) {
        return original || ShortsPlayerState.getCurrent().isClosed();
    }

}
