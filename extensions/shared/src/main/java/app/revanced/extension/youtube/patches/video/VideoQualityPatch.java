package app.revanced.extension.youtube.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.shared.RootView.isShortsActive;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class VideoQualityPatch {
    private static final int DEFAULT_YOUTUBE_VIDEO_QUALITY = -2;
    private static final IntegerSetting shortsQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE_SHORTS;
    private static final IntegerSetting shortsQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI_SHORTS;
    private static final IntegerSetting videoQualityMobile = Settings.DEFAULT_VIDEO_QUALITY_MOBILE;
    private static final IntegerSetting videoQualityWifi = Settings.DEFAULT_VIDEO_QUALITY_WIFI;

    @NonNull
    public static String videoId = "";

    /**
     * Injection point.
     */
    public static void newVideoStarted() {
        VideoInformation.qualityNeedsUpdating = true;
        setVideoQuality(250);
    }

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (PlayerType.getCurrent() != PlayerType.INLINE_MINIMAL &&
                !videoId.equals(newlyLoadedVideoId)) {
            videoId = newlyLoadedVideoId;
            setVideoQuality(750);
        }
    }

    /**
     * Injection point.
     */
    public static void userSelectedVideoQuality() {
        Utils.runOnMainThreadDelayed(() ->
                        userSelectedVideoQuality(VideoInformation.getVideoQuality()),
                300
        );
    }

    private static void setVideoQuality(long delayMillis) {
        boolean isShorts = isShortsActive();
        IntegerSetting defaultQualitySetting = Utils.getNetworkType() == Utils.NetworkType.MOBILE
                ? isShorts ? shortsQualityMobile : videoQualityMobile
                : isShorts ? shortsQualityWifi : videoQualityWifi;

        int defaultQuality = defaultQualitySetting.get();

        if (defaultQuality != DEFAULT_YOUTUBE_VIDEO_QUALITY) {
            Utils.runOnMainThreadDelayed(() -> {
                        final int qualityToUseFinal = VideoInformation.getAvailableVideoQuality(defaultQuality);
                        Logger.printDebug(() -> "Changing video quality to: " + qualityToUseFinal);
                        VideoInformation.overrideVideoQuality(qualityToUseFinal);
                    }, delayMillis
            );
        }
    }

    public static void userSelectedVideoQuality(final int defaultQuality) {
        if (defaultQuality != DEFAULT_YOUTUBE_VIDEO_QUALITY) {
            final Utils.NetworkType networkType = Utils.getNetworkType();
            String networkTypeMessage = networkType == Utils.NetworkType.MOBILE
                    ? str("revanced_remember_video_quality_mobile")
                    : str("revanced_remember_video_quality_wifi");

            if (isShortsActive()) {
                if (Settings.REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED.get()) {
                    IntegerSetting defaultQualitySetting = networkType == Utils.NetworkType.MOBILE
                            ? shortsQualityMobile
                            : shortsQualityWifi;

                    defaultQualitySetting.save(defaultQuality);

                    if (Settings.REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED_TOAST.get()) {
                        Utils.showToastShort(str(
                                "revanced_remember_video_quality_toast_shorts",
                                networkTypeMessage, (defaultQuality + "p")
                        ));
                    }
                }
            } else {
                if (Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED.get()) {
                    IntegerSetting defaultQualitySetting = networkType == Utils.NetworkType.MOBILE
                            ? videoQualityMobile
                            : videoQualityWifi;

                    defaultQualitySetting.save(defaultQuality);

                    if (Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST.get()) {
                        Utils.showToastShort(str(
                                "revanced_remember_video_quality_toast",
                                networkTypeMessage, (defaultQuality + "p")
                        ));
                    }
                }
            }
        }
    }
}