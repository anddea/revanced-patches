/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.playback.quality;

import static app.morphe.extension.shared.utils.Utils.isNotEmpty;

import androidx.annotation.Nullable;

import com.google.protobuf.MessageLite;

import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.innertube.FormatOuterClass.Format;
import app.morphe.extension.youtube.patches.FullscreenVideoScalePatch;
import app.morphe.extension.youtube.patches.video.VideoQualityPatch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class PrioritizeVideoQualityPatch {
    /**
     * Generally, height, quality label, and quality ordinal are consistent. Sometimes YouTube
     * supplies inconsistent values, such as height 288 with a 240p label and 360p ordinal; those
     * formats are corrected by {@link VideoQualityPatch#fixVideoQualityResolution(String, int)}.
     * Filtering those malformed heights here prevents SABR from stalling when AVC is the only
     * usable codec. See Morphe issue 2713.
     */
    private static final List<Integer> AVAILABLE_FORMAT_HEIGHT = List.of(
            // YouTube mobile does not support 4320p.
            2160, 1440, 1080, 720,
            480, 360, 240, 144
    );

    private static final boolean PRIORITIZE_VIDEO_QUALITY = Settings.VIDEO_QUALITY_PRIORITIZE.get();

    /**
     * Injection point.
     * <p>
     * Some videos have the following video codecs:
     * 1. 1080p AVC
     * 2. 720p AVC
     * 3. 360p VP9
     * <p>
     * If the device supports VP9, 1080p AVC and 720p AVC are ignored,
     * and 360p VP9 is used as the highest video quality.
     * This is the intended behavior of YouTube,
     * which is why the video quality flyout menu is unavailable for some videos.
     * <p>
     * Although VP9 is a more advanced codec than AVC, using 1080p AVC is better than using 360p VP9.
     * <p>
     * This function removes all VP9 codecs if the highest resolution video codec is AVC.
     */
    public static List<MessageLite> prioritizeVideoQuality(@Nullable String videoId, List<MessageLite> adaptiveFormats) {
        // Stretch maps non-16:9 sources using encoded width/height from the first video format.
        captureVideoAspect(adaptiveFormats);

        if (PRIORITIZE_VIDEO_QUALITY && isNotEmpty(videoId) && !"zzzzzzzzzzz".equals(videoId)) {
            try {
                int maxHeightAVC = -1;
                int maxHeightVP9 = -1;
                for (MessageLite messageLite : adaptiveFormats) {
                    var adaptiveFormat = Format.parseFrom(messageLite.toByteArray());
                    if (adaptiveFormat != null) {
                        String mimeType = adaptiveFormat.getMimeType();
                        if (mimeType == null || !mimeType.contains("video")) {
                            continue;
                        }
                        int height = adaptiveFormat.getHeight();
                        if (!AVAILABLE_FORMAT_HEIGHT.contains(height)) {
                            continue;
                        }
                        if (mimeType.contains("avc")) {
                            maxHeightAVC = Math.max(maxHeightAVC, height);
                        } else if (mimeType.contains("vp9")) {
                            maxHeightVP9 = Math.max(maxHeightVP9, height);
                        }
                        if (maxHeightAVC != -1 && maxHeightVP9 != -1) {
                            break;
                        }
                    }
                }

                final int finalMaxHeightAVC = maxHeightAVC;
                final int finalMaxHeightVP9 = maxHeightVP9;
                final boolean shouldRemoveVP9 = finalMaxHeightVP9 > -1
                        && finalMaxHeightVP9 < finalMaxHeightAVC;
                Logger.printDebug(() -> "videoId: " + videoId
                        + ", maxHeightAVC: " + finalMaxHeightAVC
                        + ", maxHeightVP9: " + finalMaxHeightVP9
                        + ", shouldRemoveVP9: " + shouldRemoveVP9);

                if (shouldRemoveVP9) {
                    ArrayList<MessageLite> newFormats = new ArrayList<>(adaptiveFormats.size());

                    for (MessageLite messageLite : adaptiveFormats) {
                        var parsedAdaptiveFormat = Format.parseFrom(messageLite.toByteArray());
                        if (parsedAdaptiveFormat != null) {
                            String mimeType = parsedAdaptiveFormat.getMimeType();
                            boolean isVideoType = mimeType != null && mimeType.contains("video");

                            if (!isVideoType || !mimeType.contains("vp9")) {
                                newFormats.add(messageLite);
                            }
                        }
                    }

                    return newFormats;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to sort adaptive formats", ex);
            }
        }

        return adaptiveFormats;
    }

    private static void captureVideoAspect(List<MessageLite> adaptiveFormats) {
        try {
            for (MessageLite messageLite : adaptiveFormats) {
                Format format = Format.parseFrom(messageLite.toByteArray());
                if (format == null) {
                    continue;
                }
                String mimeType = format.getMimeType();
                if (mimeType == null || !mimeType.contains("video")) {
                    continue;
                }

                final int width = format.getWidth();
                final int height = format.getHeight();
                if (width > 16 && width < 8192 && height > 16 && height < 8192) {
                    FullscreenVideoScalePatch.setVideoSize(width, height);
                    return;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "captureVideoAspect failure", ex);
        }
    }
}
