/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4896
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.notifications;

import androidx.annotation.Nullable;

import kotlin.Unit;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

/**
 * Adds the videos played in the player to {@link SeenVideos}.
 */
public final class PlayedVideoTracker {

    /**
     * Opening a muted preview of a feed in the player keeps playing the same video,
     * which may not load the video again.
     */
    private static final long OPENED_PREVIEW_CHECK_MILLIS = 1000;

    @Nullable
    private static volatile String previewVideoId;

    static {
        PlayerType.getOnChange().addObserver(playerType -> {
            try {
                if (playerType.isNoneOrHidden()) {
                    previewVideoId = null;
                    return Unit.INSTANCE;
                }
                String videoId = previewVideoId;
                if (videoId == null || !playerType.isMaximizedOrFullscreen()) {
                    return Unit.INSTANCE;
                }
                previewVideoId = null;
                // The player opens before the video of a tapped notification or card is loaded,
                // so the preview counts only if it's still the video of the player.
                Utils.runOnMainThreadDelayed(() -> {
                    if (videoId.equals(VideoInformation.getVideoId())) {
                        SeenVideos.addPlayedInBackground(videoId, false);
                    }
                }, OPENED_PREVIEW_CHECK_MILLIS);
            } catch (Throwable ex) {
                Logger.printException(() -> "Player type observer failure", ex);
            }
            return Unit.INSTANCE;
        });
    }

    public static void onVideoLoaded(String videoId) {
        if (PlayerType.getCurrent() == PlayerType.INLINE_MINIMAL) {
            previewVideoId = videoId;
        } else {
            previewVideoId = null;
            SeenVideos.addPlayedInBackground(videoId, ShortsPlayerState.getCurrent().isOpen());
        }
    }
}
