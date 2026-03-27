package app.morphe.extension.youtube.patches.utils;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class PlaybackSpeedWhilePlayingPatch {
    private static final float DEFAULT_YOUTUBE_PLAYBACK_SPEED = 1.0f;

    public static boolean playbackSpeedChanged(float playbackSpeed) {
        if (playbackSpeed == DEFAULT_YOUTUBE_PLAYBACK_SPEED) {
            if (PlayerType.getCurrent().isMaximizedOrFullscreenOrPiP()
                    // Since RVX has a default playback speed setting for Shorts,
                    // Playback speed reset should also be prevented in Shorts.
                    || ShortsPlayerState.getCurrent().isOpen() && EngagementPanel.isOpen()) {
                Logger.printDebug(() -> "Ignore changing playback speed, as it is invalid request");

                return true;
            }
        }

        return false;
    }

}


