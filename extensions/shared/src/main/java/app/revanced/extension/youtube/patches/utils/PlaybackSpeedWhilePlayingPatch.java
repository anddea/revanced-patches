package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public class PlaybackSpeedWhilePlayingPatch {
    private static final float DEFAULT_YOUTUBE_PLAYBACK_SPEED = 1.0f;

    public static boolean playbackSpeedChanged(float playbackSpeed) {
        if (playbackSpeed == DEFAULT_YOUTUBE_PLAYBACK_SPEED &&
                PlayerType.getCurrent().isMaximizedOrFullscreen()) {

            Logger.printDebug(() -> "Even though playback has already started and the user has not changed the playback speed, " +
                    "the app attempts to change the playback speed to 1.0x." +
                    "\nIgnore changing playback speed, as it is invalid request.");

            return true;
        }

        return false;
    }

}


