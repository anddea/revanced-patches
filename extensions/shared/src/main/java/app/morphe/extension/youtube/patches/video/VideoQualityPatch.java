package app.morphe.extension.youtube.patches.video;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.shared.RootView.isShortsActive;

import androidx.annotation.Nullable;

import com.google.android.libraries.youtube.innertube.model.media.FormatStreamModel;
import com.google.android.libraries.youtube.innertube.model.media.VideoQuality;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.PatchStatus;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.VideoUtils;
import j$.util.Optional;

@SuppressWarnings({"rawtypes", "unused", "UnnecessaryBoxing"})
public class VideoQualityPatch {

    /**
     * Interface to use obfuscated methods.
     */
    public interface VideoQualityMenuInterface {
        void patch_setQuality(VideoQuality quality);
    }

    /**
     * Video resolution of the automatic quality option..
     */
    public static final int AUTOMATIC_VIDEO_QUALITY_VALUE = -2;

    /**
     * The default setting for 'Hide video ads' is ON.
     * Since 'Hide video ads' can only be changed in the settings when the 'Hide ads' patch is included, the patch status is not checked.
     */
    private static final boolean HIDE_VIDEO_ADS = Settings.HIDE_VIDEO_ADS.get();

    private static final IntegerSetting shortsQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE_SHORTS;
    private static final IntegerSetting shortsQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI_SHORTS;
    private static final IntegerSetting videoQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE;
    private static final IntegerSetting videoQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI;

    private static boolean qualityNeedsUpdating;
    private static boolean userChangedQuality;

    /**
     * The available formats of the current video.
     */
    @Nullable
    private static List<FormatStreamModel> currentFormats;

    /**
     * The preferred format.
     */
    @Nullable
    private static FormatStreamModel preferredFormat;

    /**
     * The available qualities of the current video.
     */
    @Nullable
    private static VideoQuality[] currentQualities;

    /**
     * The current quality of the video playing.
     * This is always the actual quality even if Automatic quality is active.
     */
    @Nullable
    private static VideoQuality currentQuality;

    /**
     * The current VideoQualityMenuInterface, set during setVideoQuality.
     */
    @Nullable
    private static VideoQualityMenuInterface currentMenuInterface;

    @Nullable
    public static VideoQuality[] getCurrentQualities() {
        return currentQualities;
    }

    @Nullable
    public static VideoQuality getCurrentQuality() {
        return currentQuality;
    }

    @Nullable
    public static VideoQualityMenuInterface getCurrentMenuInterface() {
        return currentMenuInterface;
    }

    public static void setCurrentQuality(VideoQuality updatedCurrentQuality) {
        try {
            if (updatedCurrentQuality.patch_getResolution() != AUTOMATIC_VIDEO_QUALITY_VALUE
                    && (currentQuality == null || currentQuality != updatedCurrentQuality)) {
                currentQuality = updatedCurrentQuality;
                updateQualityString(updatedCurrentQuality.patch_getQualityName());
                Logger.printDebug(() -> "Current quality changed to: " + updatedCurrentQuality);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setCurrentQuality failed", ex);
        }
    }

    public static boolean shouldRememberVideoQuality() {
        userChangedQuality = true;
        if (!PatchStatus.VideoPlayback()) {
            return false;
        }
        BooleanSetting preference = isShortsActive()
                ? Settings.REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED
                : Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED;
        return preference.get();
    }

    public static int getDefaultQualityResolution() {
        if (!PatchStatus.VideoPlayback()) {
            return AUTOMATIC_VIDEO_QUALITY_VALUE;
        }
        final boolean isShorts = isShortsActive();
        IntegerSetting preference = Utils.getNetworkType() == Utils.NetworkType.MOBILE
                ? (isShorts ? shortsQualityMobile : videoQualityMobile)
                : (isShorts ? shortsQualityWifi : videoQualityWifi);
        return preference.get();
    }

    public static void saveDefaultQuality(int qualityResolution) {
        final boolean shortPlayerOpen = isShortsActive();
        String networkTypeMessage;
        IntegerSetting qualitySetting;
        if (Utils.getNetworkType() == Utils.NetworkType.MOBILE) {
            networkTypeMessage = str("revanced_remember_video_quality_mobile");
            qualitySetting = shortPlayerOpen ? shortsQualityMobile : videoQualityMobile;
        } else {
            networkTypeMessage = str("revanced_remember_video_quality_wifi");
            qualitySetting = shortPlayerOpen ? shortsQualityWifi : videoQualityWifi;
        }

        if (qualitySetting.get() == qualityResolution) {
            // User clicked the same video quality as the current video,
            // or changed between 1080p Premium and non-Premium.
            return;
        }
        qualitySetting.save(qualityResolution);

        if (Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST.get()) {
            String qualityLabel = qualityResolution + "p";
            Utils.showToastShort(str(
                    shortPlayerOpen
                            ? "revanced_remember_video_quality_toast_shorts"
                            : "revanced_remember_video_quality_toast",
                    networkTypeMessage,
                    qualityLabel)
            );
        }
    }

    private static String getQualityNameWithITag(String qualityName, int itag) {
        // e.g.: '1080p (itag: 248)', '720p (itag: 247)'
        return String.format(Locale.ENGLISH, "%s (itag: %d)", qualityName, itag);
    }

    /**
     * Injection point.
     * <p>
     * Overrides the initial video quality to not follow the 'Video quality preferences' in YouTube settings.
     * (e.g. 'Auto (recommended)' - 360p/480p, 'Higher picture quality' - 720p/1080p...)
     * If the maximum video quality available is 1080p and the default video quality is 2160p,
     * 1080p is used as a initial video quality.
     * <p>
     * Called before {@link #newVideoStarted()}.
     */
    public static Optional getInitialVideoQuality(Optional optional) {
        if (PatchStatus.VideoPlayback()) {
            final int preferredQuality = getDefaultQualityResolution();
            if (preferredQuality != AUTOMATIC_VIDEO_QUALITY_VALUE) {
                Logger.printDebug(() -> "initialVideoQuality: " + preferredQuality);
                // In IDE, 'Integer.valueOf()' is marked with unnecessary boxing, but unpatched YouTube uses this method.
                return Optional.of(Integer.valueOf(preferredQuality));
            }
        }
        return optional;
    }

    /**
     * Injection point.
     *
     * @param formats Formats available, ordered from largest to smallest.
     */
    public static void setVideoFormat(List<FormatStreamModel> formats) {
        if (PatchStatus.VideoPlayback() && !userChangedQuality && !CollectionUtils.isEmpty(formats) &&
                (currentFormats == null || !CollectionUtils.isEqualCollection(currentFormats, formats))) {
            try {
                // If the video format is overridden while a Shorts is playing or a video ad is playing,
                // the player UI becomes weird.
                if (!HIDE_VIDEO_ADS || isShortsActive()) {
                    userChangedQuality = true;
                    return;
                }
                final int preferredQuality = getDefaultQualityResolution();
                if (preferredQuality == AUTOMATIC_VIDEO_QUALITY_VALUE) {
                    userChangedQuality = true;
                    return;
                }
                currentFormats = formats;
                List<String> videoFormats = new ArrayList<>(formats.size());
                for (FormatStreamModel format : formats) {
                    final int itag = format.patch_getITag();
                    final String qualityName = format.patch_getQualityName();
                    final int qualityResolution = format.patch_getResolution();
                    videoFormats.add(getQualityNameWithITag(qualityName, itag));
                    if (preferredFormat == null && qualityResolution <= preferredQuality) {
                        preferredFormat = format;
                    }
                }
                Logger.printDebug(() -> "VideoFormats: " + videoFormats);
            } catch (Exception ex) {
                Logger.printException(() -> "setVideoFormat failure", ex);
            }
        }
    }

    /**
     * Injection point.
     * <p>
     * Overrides the initial video quality to not follow the 'Video quality preferences' in YouTube settings.
     * (e.g. 'Auto (recommended)' - 360p/480p, 'Higher picture quality' - 720p/1080p...)
     * Called after {@link #setVideoFormat(List)}.
     * Called before {@link #setVideoQuality(VideoQuality[], VideoQualityMenuInterface, int)}.
     */
    public static FormatStreamModel getVideoFormat(FormatStreamModel format) {
        if (PatchStatus.VideoPlayback() && format != null && !userChangedQuality && preferredFormat != null) {
            try {
                final String currentQuality = format.patch_getQualityName();
                final String preferredQuality = preferredFormat.patch_getQualityName();
                final int currentITag = format.patch_getITag();
                final int preferredITag = preferredFormat.patch_getITag();
                final String currentQualityWithITag = getQualityNameWithITag(currentQuality, currentITag);
                final String preferredQualityWithITag = getQualityNameWithITag(preferredQuality, preferredITag);
                final boolean qualityNeedsChange = currentITag != preferredITag;
                Logger.printDebug(() -> qualityNeedsChange
                        ? "Changing video format from: " + currentQualityWithITag + " to: " + preferredQualityWithITag
                        : "Video format already has the preferred quality: " + currentQualityWithITag
                );
                return preferredFormat;
            } catch (Exception ex) {
                Logger.printException(() -> "getVideoFormat failure", ex);
            }
        }
        return format;
    }

    /**
     * Injection point.
     *
     * @param qualities            Video qualities available, ordered from largest to smallest, with index 0 being the 'automatic' value of -2
     * @param originalQualityIndex quality index to use, as chosen by YouTube
     */
    public static int setVideoQuality(VideoQuality[] qualities, VideoQualityMenuInterface menu, int originalQualityIndex) {
        try {
            Utils.verifyOnMainThread();
            currentMenuInterface = menu;

            final boolean availableQualitiesChanged = (currentQualities == null)
                    || !Arrays.equals(currentQualities, qualities);
            if (availableQualitiesChanged) {
                currentQualities = qualities;
                Logger.printDebug(() -> "VideoQualities: " + Arrays.toString(currentQualities));
            }

            // This value may sometimes be -1 if the client is spoofed as a 'TV' or 'MWEB':
            // Exception: length=8; index=-1
            // java.lang.ArrayIndexOutOfBoundsException: length=8; index=-1
            originalQualityIndex = Math.max(originalQualityIndex, 0);

            VideoQuality updatedCurrentQuality = qualities[originalQualityIndex];
            setCurrentQuality(updatedCurrentQuality);

            final int preferredQuality = getDefaultQualityResolution();
            if (preferredQuality == AUTOMATIC_VIDEO_QUALITY_VALUE) {
                return originalQualityIndex; // Nothing to do.
            }

            // After changing videos the qualities can initially be for the prior video.
            // If the qualities have changed and the default is not auto then an update is needed.
            if (!qualityNeedsUpdating && !availableQualitiesChanged) {
                return originalQualityIndex;
            }
            qualityNeedsUpdating = false;

            // Find the highest quality that is equal to or less than the preferred.
            int i = 0;
            for (VideoQuality quality : qualities) {
                final int qualityResolution = quality.patch_getResolution();
                if ((qualityResolution != AUTOMATIC_VIDEO_QUALITY_VALUE && qualityResolution <= preferredQuality)
                        // Use the lowest video quality if the default is lower than all available.
                        || i == qualities.length - 1) {
                    final boolean qualityNeedsChange = (i != originalQualityIndex);
                    Logger.printDebug(() -> qualityNeedsChange
                            ? "Changing video quality from: " + updatedCurrentQuality + " to: " + quality
                            : "Video is already the preferred quality: " + quality
                    );

                    // On first load of a new regular video, if the video is already the
                    // desired quality then the quality flyout will show 'Auto' (ie: Auto (720p)).
                    //
                    // To prevent user confusion, set the video index even if the
                    // quality is already correct so the UI picker will not display "Auto".
                    if (qualityNeedsChange || !isShortsActive()) {
                        updateQualityString(quality.patch_getQualityName());
                        menu.patch_setQuality(qualities[i]);

                        return i;
                    }

                    return originalQualityIndex;
                }
                i++;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setVideoQuality failure", ex);
        }
        return originalQualityIndex;
    }

    /**
     * Injection point.
     * The old flyout is used in Shorts videos, but is sometimes also used in regular videos:
     * The player flyout opened before JavaScript was executed.
     *
     * @param userSelectedQualityIndex Element index of {@link #currentQualities}.
     */
    public static void userChangedQualityInOldFlyout(int userSelectedQualityIndex) {
        if (shouldRememberVideoQuality()) {
            try {
                if (currentQualities == null) {
                    Logger.printDebug(() -> "Cannot save default quality, qualities is null");
                    return;
                }
                VideoQuality quality = currentQualities[userSelectedQualityIndex];
                saveDefaultQuality(quality.patch_getResolution());
            } catch (Exception ex) {
                Logger.printException(() -> "userChangedQualityInOldFlyout failure", ex);
            }
        }
    }

    /**
     * Injection point.
     *
     * @param videoResolution Human readable resolution: 480, 720, 1080.
     */
    public static void userChangedQualityInNewFlyout(int videoResolution) {
        if (shouldRememberVideoQuality()) {
            Utils.verifyOnMainThread();
            saveDefaultQuality(videoResolution);
        }
    }

    /**
     * Injection point.
     */
    public static void newVideoStarted() {
        Utils.verifyOnMainThread();

        Logger.printDebug(() -> "newVideoStarted");
        currentFormats = null;
        currentQualities = null;
        currentQuality = null;
        currentMenuInterface = null;
        preferredFormat = null;
        qualityNeedsUpdating = true;
        userChangedQuality = false;

        // Hide the quality until playback starts and the qualities are available.
        updateQualityString(null);
    }

    /**
     * Injection point.
     */
    public static void updateQualityString(@Nullable String qualityName) {
        VideoUtils.updateQualityString(qualityName);
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
    public static int fixVideoQualityResolution(String label, int quality) {
        if (quality > 0 && label != null && !label.startsWith(String.valueOf(quality))) {
            try {
                int suffixIndex = StringUtils.indexOfAny(label, "p", "s");
                if (suffixIndex > -1) {
                    int fixedQuality = Integer.parseInt(StringUtils.substring(label, 0, suffixIndex));
                    Logger.printDebug(() -> "Changing wrong quality resolution from: " +
                            quality + " (" + label + ") to: " + fixedQuality + " (" + label + ")");
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
     * <p>
     * When streaming data is spoofed as 'TV' or 'MWEB' by the 'Spoof streaming data' patch,
     * both '1080p' and '1080p60' appear in the video quality flyout menu.
     * To avoid user confusion, '1080p' is removed when both '1080p' and '1080p60' are available.
     * <p>
     *
     * @param streams Format streams available, ordered from largest to smallest.
     * @return Patched format streams.
     */
    public static List<FormatStreamModel> removeLowFpsVideoQualities(List<FormatStreamModel> streams) {
        if (streams != null && streams.size() > 2) {
            try {
                int previousQualityFps = -1;
                int previousQualityResolution = -1;

                for (int i = streams.size() - 1; i >= 0; i--) {
                    var stream = streams.get(i);

                    // 1080, 720, 360..
                    int qualityResolution = stream.patch_getResolution();
                    // Skip 'Auto' (-2)
                    if (qualityResolution < 0) continue;

                    // 1080p, 1080p60, 1080p HDR...
                    String qualityName = stream.patch_getQualityName();
                    if (qualityName == null) continue;
                    if (qualityName.contains("Premium")) {
                        // Skip '1080p Premium'
                        continue;
                    }

                    // 1 .. 60
                    final int qualityFps = stream.patch_getFps();

                    // If the resolution of the previous index and the resolution of the current index are the same,
                    // the resolution with the lower fps is removed.
                    if (previousQualityResolution == qualityResolution) {
                        int finalPreviousQualityResolution = previousQualityResolution;
                        int finalPreviousQualityFps = previousQualityFps;
                        if (previousQualityFps > qualityFps) {
                            streams.remove(i);
                            Logger.printDebug(() -> "Higher fps video quality already exists: " + finalPreviousQualityResolution + " (" + finalPreviousQualityFps + "fps), removes lower fps video quality: " + qualityResolution + " (" + qualityFps + "fps)");
                        } else if (previousQualityFps < qualityFps) {
                            streams.remove(i + 1);
                            Logger.printDebug(() -> "Higher fps video quality already exists: " + qualityResolution + " (" + qualityFps + "fps), removes lower fps video quality: " + finalPreviousQualityResolution + " (" + finalPreviousQualityFps + "fps)");
                        }
                    } else { // Otherwise, save fps and resolution in fields.
                        previousQualityFps = qualityFps;
                        previousQualityResolution = qualityResolution;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "removeLowFpsVideoQualities failure", ex);
            }
        }

        return streams;
    }
}