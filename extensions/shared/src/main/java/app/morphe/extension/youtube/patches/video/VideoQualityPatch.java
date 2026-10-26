/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.video;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.shared.RootView.isShortsActive;

import androidx.annotation.Nullable;

import com.google.protobuf.MessageLite;
import java.util.Map;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.innertube.ClientAbrStateOuterClass.ClientAbrState;

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
     * Interface implemented by YouTube's obfuscated video-quality model.
     *
     * The model class is renamed in YouTube 21.04, so extension methods must use this stable
     * interface instead of the pre-21.04 concrete class descriptor.
     */
    public interface VideoQualityInterface {
        String patch_getQualityName();
        int patch_getResolution();
    }

    /**
     * Interface to use obfuscated methods.
     */
    public interface VideoQualityMenuInterface {
        void patch_setQuality(VideoQualityInterface quality);
    }

    /**
     * Video resolution of the automatic quality option.
     */
    public static final int AUTOMATIC_VIDEO_QUALITY_VALUE = -2;

    /**
     * The default setting for 'Hide video ads' is ON.
     * Since 'Hide video ads' can only be changed in the settings when the 'Hide ads' patch is included, the patch status is not checked.
     */
    private static final boolean HIDE_VIDEO_ADS = Settings.HIDE_VIDEO_ADS.get();

    // Set the access modifier to final to override the flag only after the state snapshot is reloaded.
    private static final boolean SETTINGS_INITIALIZED = Settings.SETTINGS_INITIALIZED.get();

    private static final IntegerSetting shortsQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE_SHORTS;
    private static final IntegerSetting shortsQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI_SHORTS;
    private static final IntegerSetting videoQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE;
    private static final IntegerSetting videoQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI;

    private static boolean qualityNeedsUpdating;
    private static boolean userChangedQuality;

    /**
     * 'Advanced' video quality setting, used when the user selects a quality from the quality menu.
     */
    private static final int VIDEO_QUALITY_SETTING_ADVANCED_MENU = 3;

    /**
     * ID of the video whose quality was selected by the user, and the resolution selected.
     * The video ID is used, because a player reload starts the same video again.
     */
    private static volatile String userSelectedQualityVideoId = "";
    private static volatile int userSelectedQualityResolution = AUTOMATIC_VIDEO_QUALITY_VALUE;

    private static void setUserSelectedQuality(int resolution) {
        // Keep the legacy format selector from overriding an explicit menu selection.
        userChangedQuality = true;
        userSelectedQualityResolution = resolution;
        userSelectedQualityVideoId = VideoInformation.getVideoId();
    }

    /**
     * @return If the user selected the quality of the current video.
     */
    private static boolean isQualitySelectedByUser() {
        String videoId = VideoInformation.getVideoId();
        return !videoId.isEmpty() && videoId.equals(userSelectedQualityVideoId);
    }



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
    private static VideoQualityInterface[] currentQualities;

    /**
     * The current quality of the video playing.
     * This is always the actual quality even if Automatic quality is active.
     */
    @Nullable
    private static VideoQualityInterface currentQuality;

    /**
     * The current VideoQualityMenuInterface, set during setVideoQuality.
     */
    @Nullable
    private static VideoQualityMenuInterface currentMenuInterface;

    @Nullable
    public static VideoQualityInterface[] getCurrentQualities() {
        return currentQualities;
    }

    @Nullable
    public static VideoQualityInterface getCurrentQuality() {
        return currentQuality;
    }

    @Nullable
    public static VideoQualityMenuInterface getCurrentMenuInterface() {
        return currentMenuInterface;
    }

    public static void setCurrentQuality(VideoQualityInterface updatedCurrentQuality) {
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
     * Called when a video is opened, before it starts.
     *
     * @return If the video must not be opened, always false.
     */
    public static boolean onVideoIntentLoaded(Map<Object, Object> playbackStartDescriptorMap, String videoId) {
        if (!userSelectedQualityVideoId.equals(videoId)) {
            // Another video is opened. The video ID of the player is updated only once
            // the video starts, so the quality selected for the current video
            // would otherwise be used when the other video starts.
            userSelectedQualityVideoId = "";
        }
        return false;
    }

    /**
     * Injection point.
     * <p>
     * Overrides the initial video quality to not follow the 'Video quality preferences' in YouTube settings.
     * (e.g. 'Auto (recommended)' - 360p/480p, 'Higher picture quality' - 720p/1080p...)
     * If the maximum video quality available is 1080p and the default video quality is 2160p,
     * 1080p is used as an initial video quality.
     * <p>
     * Called before {@link #newVideoStarted()}.
     */
    public static Optional getInitialVideoQuality(Optional optional) {
        // YouTube can reload the video after the user selects a quality, such as when
        // the selected quality needs another codec. The reload has no initial quality,
        // or a copy of the default quality used when the video started.
        // The video ID of the player is updated only once the video starts, so the selected
        // quality is not used once the player response of another video is received,
        // such as the next video of the autoplay.
        if (isQualitySelectedByUser() && VideoInformation.getPlayerResponseVideoId()
                .equals(VideoInformation.getVideoId())) {
            final int selectedResolution = userSelectedQualityResolution;
            Logger.printDebug(() -> "initialVideoQuality selected by the user: " + selectedResolution);
            // With 'Auto' selected, YouTube chooses the quality.
            return selectedResolution > 0
                    ? Optional.of(selectedResolution)
                    : Optional.empty();
        }

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
     * <p>
     * The initial video quality is only used when the video starts. While the video plays,
     * the player and the SABR requests use the quality last selected in the quality menu,
     * and without one the quality is chosen automatically (such as 720p for the size of the player).
     * The default quality is used as the quality last selected, until the user selects a quality.
     * With some accounts the quality last selected is used only for the video it was selected for,
     * so the default quality is then used as if it was selected for the current video.
     *
     * @return The resolution to use as the quality last selected,
     *         or -1 to use the quality last selected in the quality menu.
     */
    public static int getLastSelectedVideoQuality() {
        try {
            if (isQualitySelectedByUser()) {
                return -1;
            }

            final int preferredQuality = getDefaultQualityResolution();
            return preferredQuality == AUTOMATIC_VIDEO_QUALITY_VALUE
                    ? -1
                    : getAvailableQualityResolution(preferredQuality);
        } catch (Exception ex) {
            Logger.printException(() -> "getLastSelectedVideoQuality failure", ex);
            return -1;
        }
    }

    /**
     * @return The resolution the SABR requests must keep: the quality selected by the user
     *         for the current video, otherwise the default quality.
     *         -1 to send the requests unchanged, with 'Auto' selected or no default quality.
     */
    private static int getStickyVideoQuality() {
        if (isQualitySelectedByUser()) {
            final int selectedResolution = userSelectedQualityResolution;
            return selectedResolution > 0 ? selectedResolution : -1;
        }

        return getLastSelectedVideoQuality();
    }

    /**
     * Injection point.
     * <p>
     * The server chooses the quality of SABR streams from the client ABR state sent with each request.
     * The first request of a video sends the initial video quality as the sticky resolution,
     * but the requests sent while the video plays have none, so the server can switch to another quality,
     * such as a lower one for the size of the player or in power saving mode.
     * The quality is sent as if the user just selected it, in every request.
     *
     * @return The client ABR state with the video quality, or null to keep the original.
     */
    @Nullable
    public static byte[] overrideClientAbrState(@Nullable MessageLite clientAbrState) {
        try {
            if (clientAbrState == null) {
                return null;
            }

            final int resolution = getStickyVideoQuality();
            if (resolution < 0) {
                return null;
            }

            ClientAbrState state = ClientAbrState.parseFrom(clientAbrState.toByteArray());
            ClientAbrState.Builder builder = state.toBuilder()
                    .setStickyResolution(resolution)
                    .setVideoQualitySetting(VIDEO_QUALITY_SETTING_ADVANCED_MENU)
                    .setLastManualSelectedResolution(resolution)
                    .setPowerSaveMode(false);

            if (!state.hasLastManualSelectedResolution()) {
                // No quality was selected in the quality menu, so it was selected just now
                // and it is the quality that is playing.
                builder.setTimeSinceLastManualFormatSelectionMs(0)
                        .setLastManualDirection(0);
            }

            // The server does not choose a quality above the size of the player.
            // The resolution is the short side of the video, so it is compared with the short side of the player.
            if (state.hasClientViewportWidth() && state.hasClientViewportHeight()) {
                final int width = state.getClientViewportWidth();
                final int height = state.getClientViewportHeight();
                final int shortSide = Math.min(width, height);
                if (shortSide > 0 && shortSide < resolution) {
                    builder.setClientViewportWidth((int) Math.ceil((double) width * resolution / shortSide))
                            .setClientViewportHeight((int) Math.ceil((double) height * resolution / shortSide));
                }
            }

            return builder.build().toByteArray();
        } catch (Exception ex) {
            Logger.printException(() -> "overrideClientAbrState failure", ex);
            return null;
        }
    }

    /**
     * The quality last selected is used as the only quality allowed by the player.
     * If the video does not have it, all lower qualities are allowed and
     * the player chooses one of them automatically, such as 720p for the size of the player.
     *
     * @return The highest quality of the current video that is not above the preferred quality,
     *         or the preferred quality if the qualities of the video are not known yet
     *         or all of them are above it (the player then uses the lowest quality).
     */
    private static int getAvailableQualityResolution(int preferredQuality) {
        VideoQualityInterface[] qualities = currentQualities;
        if (qualities != null) {
            // Ordered from largest to smallest, with index 0 being 'Auto'.
            for (VideoQualityInterface quality : qualities) {
                final int resolution = quality.patch_getResolution();
                if (resolution != AUTOMATIC_VIDEO_QUALITY_VALUE && resolution <= preferredQuality) {
                    return resolution;
                }
            }
        }

        return preferredQuality;
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
     * Called before {@link #setVideoQuality(VideoQualityInterface[], VideoQualityMenuInterface, int)}.
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
     * Compatibility bridge for the legacy YouTube quality model.
     *
     * The 20.51 callback passes a {@code VideoQuality[]} array. Arrays are invariant, so the
     * legacy bytecode must not invoke the interface-array overload directly. The legacy model is
     * made to implement {@link VideoQualityInterface} by the patcher before this bridge runs.
     */
    public static int setVideoQuality(VideoQuality[] qualities, VideoQualityMenuInterface menu, int originalQualityIndex) {
        VideoQualityInterface[] interfaceQualities = new VideoQualityInterface[qualities.length];
        for (int i = 0; i < qualities.length; i++) {
            interfaceQualities[i] = (VideoQualityInterface) qualities[i];
        }
        return setVideoQuality(interfaceQualities, menu, originalQualityIndex);
    }

    /**
     * Injection point.
     *
     * @param qualities            Video qualities available, ordered from largest to smallest, with index 0 being the 'automatic' value of -2
     * @param originalQualityIndex quality index to use, as chosen by YouTube
     */
    public static int setVideoQuality(VideoQualityInterface[] qualities, VideoQualityMenuInterface menu, int originalQualityIndex) {
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

            VideoQualityInterface updatedCurrentQuality = qualities[originalQualityIndex];
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
            for (VideoQualityInterface quality : qualities) {
                final int qualityResolution = quality.patch_getResolution();
                if ((qualityResolution != AUTOMATIC_VIDEO_QUALITY_VALUE && qualityResolution <= preferredQuality)
                        // Use the lowest video quality if the default is lower than all available.
                        || i == qualities.length - 1) {
                    final boolean qualityNeedsChange = (i != originalQualityIndex);
                    Logger.printDebug(() -> qualityNeedsChange
                            ? "Changing video quality from: " + updatedCurrentQuality + " to: " + quality
                            : "Video is already the preferred quality: " + quality
                    );

                    if (qualityNeedsChange) {
                        updateQualityString(quality.patch_getQualityName());
                        if (menu != null) {
                            menu.patch_setQuality(qualities[i]);
                        }

                        return i;
                    }

                    return originalQualityIndex;
                }
                i++;
            }
        } catch (Throwable ex) {
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
        try {
            if (currentQualities == null) {
                Logger.printDebug(() -> "Cannot save default quality, qualities is null");
                // The resolution is not known, so a reload uses the quality saved by YouTube.
                setUserSelectedQuality(AUTOMATIC_VIDEO_QUALITY_VALUE);
                return;
            }
            final int resolution = currentQualities[userSelectedQualityIndex].patch_getResolution();
            setUserSelectedQuality(resolution);

            if (shouldRememberVideoQuality()) {
                saveDefaultQuality(resolution);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "userChangedQualityInOldFlyout failure", ex);
        }
    }

    /**
     * Injection point.
     *
     * @param videoResolution Human readable resolution: 480, 720, 1080.
     */
    public static void userChangedQualityInNewFlyout(int videoResolution) {
        setUserSelectedQuality(videoResolution);
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

    /**
     * @return If a default quality is set, so YouTube must not choose the quality automatically.
     */
    private static boolean isDefaultQualitySet() {
        return getDefaultQualityResolution() != AUTOMATIC_VIDEO_QUALITY_VALUE;
    }

    /**
     * Injection point.
     * <p>
     * Overrides the 'Video quality preferences' in YouTube settings with the setting
     * used when the user selects a quality from the quality menu, so neither the device
     * nor the SABR server choose the quality automatically (such as with 'Data saver').
     *
     * @return If the 'Advanced' video quality setting is used.
     */
    public static boolean useAdvancedVideoQualitySetting() {
        return isDefaultQualitySet();
    }
}
