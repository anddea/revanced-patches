/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.videoplayer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.util.Objects;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.patches.SaveToWatchLaterPatch;
import app.morphe.extension.youtube.patches.utils.PlaylistPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerControlButton;
import app.morphe.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class SaveToWatchLaterButton {

    public static final int saveToWatchLaterResourceId =
            ResourceUtils.getIdentifier(
                    "yt_outline_experimental_playlist_add_vd_theme_24",
                    ResourceType.DRAWABLE
            );

    @Nullable
    private static PlayerControlButton instance;

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            final boolean swapSaveAndQueue = Settings.SWAP_SAVE_AND_QUEUE_ACTIONS.get();

            instance = new PlayerControlButton(
                    controlsView,
                    "morphe_save_to_watch_later_button",
                    false,
                    Settings.SAVE_TO_WATCH_LATER_BUTTON::get,
                    v -> {
                        if (swapSaveAndQueue) {
                            PlaylistPatch.prepareDialogBuilder(VideoInformation.getVideoId());
                        } else {
                            SaveToWatchLaterPatch.saveVideo(VideoInformation.getVideoId());
                        }
                    },
                    v -> {
                        if (swapSaveAndQueue) {
                            SaveToWatchLaterPatch.saveVideo(VideoInformation.getVideoId());
                        } else {
                            PlaylistPatch.prepareDialogBuilder(VideoInformation.getVideoId());
                        }
                        return true;
                    }
            );

            final ImageView imageView = Objects.requireNonNull(instance.imageView());
            if (swapSaveAndQueue && saveToWatchLaterResourceId != 0) {
                imageView.setImageResource(saveToWatchLaterResourceId);
            } else {
                instance.setIcon("morphe_save_to_watch_later_button");
            }

            // The host playlist drawable uses colorControlNormal, which can be black in light mode.
            // Player overlay controls must remain white over the video.
            imageView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void setVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

    /**
     * Injection point.
     */
    public static void setVisibilityImmediate(boolean visible) {
        if (instance != null) instance.setVisibilityImmediate(visible);
    }

    /**
     * Injection point.
     */
    public static void setVisibility(boolean visible, boolean animated) {
        if (instance != null) instance.setVisibility(visible, animated);
    }
}
