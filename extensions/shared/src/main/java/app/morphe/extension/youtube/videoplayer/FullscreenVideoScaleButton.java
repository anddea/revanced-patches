/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2616
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.videoplayer;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.FullscreenVideoScalePatch;
import app.morphe.extension.youtube.patches.FullscreenVideoScalePatch.VideoScaleMode;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerControlButton;

@SuppressWarnings("unused")
public class FullscreenVideoScaleButton {

    @Nullable
    private static PlayerControlButton instance;

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            instance = new PlayerControlButton(
                    controlsView,
                    "fullscreen_video_scale_button",
                    true,
                    Settings.FULLSCREEN_VIDEO_SCALE_BUTTON::get,
                    view -> cycleScaleMode(),
                    null
            );
            updateButtonIcon(Settings.FULLSCREEN_VIDEO_SCALE.get());
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
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

    private static void cycleScaleMode() {
        try {
            VideoScaleMode current = Settings.FULLSCREEN_VIDEO_SCALE.get();
            VideoScaleMode next = switch (current) {
                case DEFAULT -> VideoScaleMode.STRETCH;
                case STRETCH -> VideoScaleMode.ZOOM;
                case ZOOM -> VideoScaleMode.DEFAULT;
            };
            Settings.FULLSCREEN_VIDEO_SCALE.save(next);
            updateButtonIcon(next);
            FullscreenVideoScalePatch.applyScale();
        } catch (Exception ex) {
            Logger.printException(() -> "cycleScaleMode failure", ex);
        }
    }

    private static void updateButtonIcon(VideoScaleMode mode) {
        Utils.verifyOnMainThread();

        PlayerControlButton button = instance;
        if (button == null) {
            return;
        }
        ImageView imageView = button.imageView();
        if (imageView != null) {
            imageView.setImageResource(ResourceUtils.getIdentifierOrThrow(
                    getIconName(mode),
                    ResourceType.DRAWABLE
            ));
        }
    }

    private static String getIconName(VideoScaleMode mode) {
        return mode.iconBaseName;
    }
}
