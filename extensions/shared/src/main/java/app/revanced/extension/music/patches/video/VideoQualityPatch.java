package app.revanced.extension.music.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class VideoQualityPatch {
    /**
     * Video resolution of the automatic quality option..
     */
    private static final int AUTOMATIC_VIDEO_QUALITY_VALUE = -2;
    private static final IntegerSetting mobileQualitySetting = Settings.DEFAULT_VIDEO_QUALITY_MOBILE;
    private static final IntegerSetting wifiQualitySetting = Settings.DEFAULT_VIDEO_QUALITY_WIFI;

    private static boolean formatNeedsUpdating;

    /**
     * The preferred video format.
     */
    @Nullable
    private static Object preferredVideoFormat;

    /**
     * The available video formats of the current video.
     */
    @Nullable
    private static List<Object> videoFormats;

    /**
     * The available qualities of the current video in human readable form: [1080, 720, 480]
     */
    @Nullable
    private static Object[] videoQualities;

    /**
     * Injection point.
     *
     * @param formatStreams Format streams available, ordered from largest to smallest.
     */
    public static void setVideoFormat(List<Object> formatStreams) {
        try {
            if (!formatNeedsUpdating || CollectionUtils.isEmpty(formatStreams)) {
                return;
            }
            final int preferredQuality = Utils.getNetworkType() == Utils.NetworkType.MOBILE
                    ? mobileQualitySetting.get()
                    : wifiQualitySetting.get();
            if (preferredQuality == AUTOMATIC_VIDEO_QUALITY_VALUE) {
                formatNeedsUpdating = false;
                return;
            }
            if (videoFormats != null && CollectionUtils.isEqualCollection(videoFormats, formatStreams)) {
                return;
            }
            videoFormats = formatStreams;
            for (Object stream : formatStreams) {
                if (stream != null) {
                    final int qualityResolution = getFormatStreamResolution(stream);
                    if (qualityResolution <= preferredQuality) {
                        preferredVideoFormat = stream;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setVideoFormat failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * Overrides the initial video quality to not follow the 'Video quality preferences' in YouTube settings.
     * (e.g. 'Auto (recommended)' - 360p/480p, 'Higher picture quality' - 720p/1080p...)
     */
    public static Object getVideoFormat(Object formatStreamModel) {
        if (formatNeedsUpdating && preferredVideoFormat != null && formatStreamModel != null) {
            formatNeedsUpdating = false;
            return preferredVideoFormat;
        }
        return formatStreamModel;
    }

    /**
     * Injection point.
     *
     * @param qualities Video qualities available, ordered from largest to smallest, with index 0 being the 'automatic' value of -2
     */
    public static void setVideoQualities(Object[] qualities) {
        if (qualities != null && (videoQualities == null || !Arrays.equals(qualities, videoQualities))) {
            videoQualities = qualities;
            Logger.printDebug(() -> "videoQualities: " + Arrays.toString(qualities));
        }
    }

    /**
     * Injection point.
     */
    public static void userSelectedVideoQuality(int userSelectedQualityIndex) {
        if (Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED.get() && videoQualities != null) {
            try {
                Object streamQuality = videoQualities[userSelectedQualityIndex];
                if (streamQuality == null) {
                    return;
                }
                int quality = getVideoQualityResolution(streamQuality);
                final Utils.NetworkType networkType = Utils.getNetworkType();

                switch (networkType) {
                    case NONE -> {
                        Utils.showToastShort(str("revanced_remember_video_quality_none"));
                        return;
                    }
                    case MOBILE -> mobileQualitySetting.save(quality);
                    default -> wifiQualitySetting.save(quality);
                }

                if (Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST.get()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("revanced_remember_video_quality_");
                    if (quality == AUTOMATIC_VIDEO_QUALITY_VALUE) {
                        sb.append("auto_");
                    }
                    sb.append(networkType.getName());
                    Utils.showToastShort(str(sb.toString(), quality + "p"));
                }
            } catch (Exception ex) {
                Logger.printException(() -> "userSelectedVideoQuality failed", ex);
            }
        }
    }

    /**
     * Injection point.
     * <p>
     * Sometimes, the int value and the string value for the video quality are different:
     * Label: 360p, Value: 480
     * Label: 480p, Value: 720
     * Label: 1080p, Value: 1440
     * Label: 1440p, Value: 2160
     * <p>
     * The easiest way to solve this is to parse the quality label.
     */
    public static int fixVideoQualityResolution(int quality, String label) {
        if (quality > 0 && label != null && !label.startsWith(String.valueOf(quality))) {
            try {
                int suffixIndex = StringUtils.indexOfAny(label, "p", "s");
                if (suffixIndex > -1) {
                    int fixedQuality = Integer.parseInt(StringUtils.substring(label, 0, suffixIndex));
                    Logger.printDebug(() -> "Changing wrong quality resolution from: " +
                            quality + " (" + label + ") to: " + fixedQuality+ " (" + label + ")");
                    return fixedQuality;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "fixVideoQualityResolution failed", ex);
            }
        }

        return quality;
    }

    /**
     * Injection point.
     */
    public static void newVideoStarted() {
        Logger.printDebug(() -> "newVideoStarted");
        formatNeedsUpdating = true;
        preferredVideoFormat = null;
        videoFormats = null;
        videoQualities = null;
    }

    private static int getFormatStreamResolution(@NonNull Object formatStreamClass) {
        // This instruction is ignored by patch.
        return -2;
    }

    private static int getVideoQualityResolution(@NonNull Object qualityClass) {
        // This instruction is ignored by patch.
        return -2;
    }
}