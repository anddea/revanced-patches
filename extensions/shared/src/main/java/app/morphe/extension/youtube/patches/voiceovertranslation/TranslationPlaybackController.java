package app.morphe.extension.youtube.patches.voiceovertranslation;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.shared.VideoState;

/** Shares a single playback pause between the two translation providers. */
public final class TranslationPlaybackController {
    private static final TranslationPlaybackState state = new TranslationPlaybackState();
    private static boolean internalChange;
    private static String automaticVideoId = "";

    private static int configuredProvider() {
        // Automatic translation takes precedence over a provider waiting for a manual tap.
        if (Settings.VOT_ENABLED.get() && Settings.VOT_AUTO_TRANSLATE.get()) return TranslationPlaybackState.YANDEX;
        if (Settings.GOOGLE_VOT_ENABLED.get() && Settings.GOOGLE_VOT_AUTO_TRANSLATE.get()) return TranslationPlaybackState.GOOGLE;
        if (pauseEnabled(TranslationPlaybackState.YANDEX)) return TranslationPlaybackState.YANDEX;
        if (pauseEnabled(TranslationPlaybackState.GOOGLE)) return TranslationPlaybackState.GOOGLE;
        if (Settings.VOT_ENABLED.get()) return TranslationPlaybackState.YANDEX;
        if (Settings.GOOGLE_VOT_ENABLED.get()) return TranslationPlaybackState.GOOGLE;
        return TranslationPlaybackState.NONE;
    }

    private static boolean pauseEnabled(int provider) {
        return provider == TranslationPlaybackState.YANDEX
                ? Settings.VOT_ENABLED.get() && Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.get()
                : provider == TranslationPlaybackState.GOOGLE
                && Settings.GOOGLE_VOT_ENABLED.get() && Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.get();
    }

    /** Injection point, before YouTube has assigned the new video metadata. */
    public static void initialize() {
        int provider = configuredProvider();
        state.reset(provider, pauseEnabled(provider));
        automaticVideoId = "";
    }

    /** Injection point in ExoPlayer.setPlayWhenReady, before any frames are played. */
    public static boolean overridePlayWhenReady(boolean playing) {
        if (!pauseEnabled(state.provider())) return playing;
        return state.filterPlay(playing, internalChange);
    }

    static void newVideoLoaded(String videoId) {
        int provider = configuredProvider();
        if (state.newVideo(videoId, provider, pauseEnabled(provider))) automaticVideoId = "";
        Utils.runOnMainThread(() -> {
            if (!videoId.equals(state.videoId()) || !videoId.equals(VideoInformation.getVideoId())) return;
            enforcePause();
            if (videoId.equals(automaticVideoId)) return;
            if (state.provider() == TranslationPlaybackState.YANDEX && Settings.VOT_AUTO_TRANSLATE.get()) {
                if (VoiceOverTranslationPatch.startAutomaticTranslation(videoId)) automaticVideoId = videoId;
            } else if (state.provider() == TranslationPlaybackState.GOOGLE && Settings.GOOGLE_VOT_AUTO_TRANSLATE.get()) {
                automaticVideoId = videoId;
                // The visible-video hook is absent during background playlist transitions.
                GoogleVoiceOverTranslationPatch.newVideoLoaded(videoId);
                GoogleVoiceOverTranslationPatch.startAutomaticTranslation();
            }
        });
    }

    static void select(int provider, String videoId) {
        Utils.verifyOnMainThread();
        state.select(videoId, provider, pauseEnabled(provider), VideoState.getCurrent() == VideoState.PLAYING);
        if (provider == TranslationPlaybackState.YANDEX) GoogleVoiceOverTranslationPatch.suspendTranslation();
        else VoiceOverTranslationPatch.suspendTranslation();
        enforcePause();
    }

    static void enforcePause() {
        if (!state.isWaiting() || !pauseEnabled(state.provider())) return;
        internalChange = true;
        try { VideoInformation.setPlayerPlaying(false); }
        finally { internalChange = false; }
    }

    static boolean isWaiting(int provider, String videoId) {
        return pauseEnabled(provider) && state.isWaiting(provider, videoId);
    }

    static boolean ready(int provider, String videoId) {
        Utils.verifyOnMainThread();
        if (!videoId.equals(VideoInformation.getVideoId())) return false;
        boolean resume = state.ready(provider, videoId);
        Logger.printDebug(() -> "Translation ready: provider=" + provider + " video=" + videoId + " resume=" + resume);
        return resume && VideoInformation.setPlayerPlaying(true);
    }

    /** A failed/cancelled translation leaves the video paused, with manual playback available. */
    static void failed(int provider, String videoId) {
        state.cancel(provider, videoId);
    }

    static boolean usesGoogle() { return state.provider() == TranslationPlaybackState.GOOGLE; }
}
