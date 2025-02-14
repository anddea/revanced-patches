package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public class PlaybackSpeedWhilePlayingPatch {
    private static final float DEFAULT_YOUTUBE_PLAYBACK_SPEED = 1.0f;

    public static boolean playbackSpeedChanged(float playbackSpeed) {
        PlayerType playerType = PlayerType.getCurrent();
        if (playbackSpeed == DEFAULT_YOUTUBE_PLAYBACK_SPEED &&
                playerType.isMaximizedOrFullscreenOrPiP()) {

            Logger.printDebug(() -> "Ignore changing playback speed, as it is invalid request: " + playerType.name());

            return true;
        }

        return false;
    }

}


