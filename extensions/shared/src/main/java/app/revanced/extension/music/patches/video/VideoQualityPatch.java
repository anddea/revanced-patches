package app.revanced.extension.music.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoInformation;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class VideoQualityPatch {
    private static final int DEFAULT_YOUTUBE_MUSIC_VIDEO_QUALITY = -2;
    private static final IntegerSetting mobileQualitySetting = Settings.DEFAULT_VIDEO_QUALITY_MOBILE;
    private static final IntegerSetting wifiQualitySetting = Settings.DEFAULT_VIDEO_QUALITY_WIFI;

    /**
     * Injection point.
     */
    public static void newVideoStarted(final String ignoredVideoId) {
        final int preferredQuality =
                Utils.getNetworkType() == Utils.NetworkType.MOBILE
                        ? mobileQualitySetting.get()
                        : wifiQualitySetting.get();

        if (preferredQuality == DEFAULT_YOUTUBE_MUSIC_VIDEO_QUALITY)
            return;

        Utils.runOnMainThreadDelayed(() ->
                        VideoInformation.overrideVideoQuality(
                                VideoInformation.getAvailableVideoQuality(preferredQuality)
                        ),
                500
        );
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

    private static void userSelectedVideoQuality(final int defaultQuality) {
        if (!Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED.get())
            return;
        if (defaultQuality == DEFAULT_YOUTUBE_MUSIC_VIDEO_QUALITY)
            return;

        final Utils.NetworkType networkType = Utils.getNetworkType();

        switch (networkType) {
            case NONE -> {
                Utils.showToastShort(str("revanced_remember_video_quality_none"));
                return;
            }
            case MOBILE -> mobileQualitySetting.save(defaultQuality);
            default -> wifiQualitySetting.save(defaultQuality);
        }

        if (!Settings.REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST.get())
            return;

        Utils.showToastShort(str("revanced_remember_video_quality_" + networkType.getName(), defaultQuality + "p"));
    }
}