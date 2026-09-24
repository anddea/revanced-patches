/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - COOLak (https://github.com/COOLak)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */


package app.morphe.extension.youtube.patches.voiceovertranslation;

import com.google.protobuf.MessageLite;
import java.lang.ref.WeakReference;
import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.VideoDetails;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

/** Shares a single playback pause between the two translation providers. */
@SuppressWarnings("unused")
public final class TranslationPlaybackController {
    private static final TranslationPlaybackState state = new TranslationPlaybackState();
    private static boolean internalChange;
    private static String automaticVideoId = "";
    private static volatile WeakReference<Object> nativePlayer = new WeakReference<>(null);

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

    private static boolean pauseOnStartup(int provider) {
        return pauseEnabled(provider) && (provider == TranslationPlaybackState.YANDEX
                ? Settings.VOT_AUTO_TRANSLATE.get() : Settings.GOOGLE_VOT_AUTO_TRANSLATE.get());
    }

    /** Injection point, before YouTube has assigned the new video metadata. */
    public static void initialize(Object player) {
        nativePlayer = new WeakReference<>(player);
        int provider = configuredProvider();
        state.initialize(provider, pauseOnStartup(provider));
        Logger.printDebug(() -> "Translation player initialized; retaining video: " + state.videoId());
    }

    /** Injection point in the current LocalDirector's loadVideo, not response preloading. */
    public static void nativeVideoLoaded(Object player, Object rawDetails) {
        if (nativePlayer.get() != player || !(rawDetails instanceof MessageLite details)) return;
        try {
            VideoDetails video = VideoDetails.parseFrom(details.toByteArray());
            String id = video.getVideoId();
            if (id.isEmpty()) return;
            Utils.runOnMainThread(() -> {
                // A queued callback from a retired director must not replace the pause owner.
                if (nativePlayer.get() != player) return;
                Logger.printDebug(() -> "Translation native video loaded: " + id);
                VideoInformation.setVideoInformation(video.getChannelId(), video.getAuthor(),
                        id, video.getTitle(), video.getLengthSeconds() * 1000, video.getIsLiveContent());
                VoiceOverTranslationPatch.newVideoStarted(video.getChannelId(), video.getAuthor(),
                        id, video.getTitle(), video.getLengthSeconds() * 1000, video.getIsLiveContent());
                newVideoLoaded(id);
                // Stop the previous video's speech even if the Google UI hook is absent.
                GoogleVoiceOverTranslationPatch.newVideoLoaded(id);
            });
        } catch (Exception ex) {
            Logger.printException(() -> "Could not read translation playback metadata", ex);
        }
    }

    /** YouTube normally waits for playback before showing the watch page, or for a timeout. */
    public static int overrideWatchNextProcessingDelay(int delay) {
        // This is read before the player is created, so use the configured provider.
        // Loading details/comments must not depend on releasing the translation pause.
        if (!pauseEnabled(configuredProvider())) return delay;
        Logger.printDebug(() -> "Loading watch page without waiting for playback");
        return 0;
    }

    /** Injection point in ExoPlayer.setPlayWhenReady, before any frames are played. */
    public static boolean overridePlayWhenReady(Object player, boolean playing) {
        if (!VideoInformation.isCurrentPlayer(player)) return playing;
        boolean result = pauseEnabled(state.provider()) ? state.filterPlay(playing, internalChange) : playing;
        if (!result) Utils.runOnMainThread(() -> {
            if (!VideoInformation.isCurrentPlayer(player) || VideoInformation.isPlayerPlaying()) return;
            VoiceOverTranslationPatch.pauseAudio();
            GoogleVoiceOverTranslationPatch.onPlaybackPaused();
        });
        return result;
    }

    /** Injection point using the actual video-ID register, never the cached metadata bridge. */
    public static void newVideoLoaded(String videoId) {
        int provider = configuredProvider();
        if (state.newVideo(videoId, provider, pauseOnStartup(provider))) automaticVideoId = "";
        metadataLoaded(videoId);
    }

    static void metadataLoaded(String videoId) {
        if (!state.matchesVideo(videoId)) return;
        Utils.runOnMainThread(() -> {
            if (!state.matchesVideo(videoId) || !videoId.equals(VideoInformation.getVideoId())) return;
            if (state.takeDeferredResume()) VideoInformation.setPlayerPlaying(true);
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

    /** Apply translation choices to the current video as well as subsequent videos. */
    public static void onSettingChanged(String key) {
        Utils.verifyOnMainThread();
        if (key.equals(Settings.GOOGLE_VOT_USE_NATIVE_TTS.key)
                || key.equals(Settings.GOOGLE_VOT_TTS_VOICE_TYPE.key)) {
            GoogleVoiceOverTranslationPatch.onVoiceChanged();
            return;
        }
        if (key.equals(Settings.GOOGLE_VOT_TRANSLATION_SERVICE.key)
                || key.equals(Settings.GOOGLE_VOT_CAPTION_LANGUAGE.key)
                || key.equals(Settings.GOOGLE_VOT_OPENROUTER_MODEL.key)) {
            GoogleVoiceOverTranslationPatch.reloadTranscript();
            return;
        }
        String id = VideoInformation.getVideoId();
        if (!state.matchesVideo(id)) return;
        if (key.equals(Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.key)
                || key.equals(Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.key)) {
            if (!pauseEnabled(state.provider())) state.cancel(state.provider(), id);
            return;
        }
        if (!key.equals(Settings.VOT_ENABLED.key) && !key.equals(Settings.GOOGLE_VOT_ENABLED.key)
                && !key.equals(Settings.VOT_AUTO_TRANSLATE.key) && !key.equals(Settings.GOOGLE_VOT_AUTO_TRANSLATE.key)) return;
        int provider = configuredProvider();
        if (provider != state.provider()) {
            state.configure(id, provider, pauseOnStartup(provider), VideoInformation.isPlayerPlaying());
            suspendOtherProvider(provider);
            enforcePause();
        } else if (!state.hasRequest()) {
            state.configure(id, provider, pauseOnStartup(provider), VideoInformation.isPlayerPlaying());
            enforcePause();
        }
        if (provider == TranslationPlaybackState.NONE) GoogleVoiceOverTranslationPatch.suspendTranslation();
        automaticVideoId = "";
        metadataLoaded(id);
    }

    static void select(int provider, String videoId) {
        Utils.verifyOnMainThread();
        state.select(videoId, provider, pauseEnabled(provider), VideoInformation.isPlayerPlaying());
        suspendOtherProvider(provider);
        enforcePause();
    }

    private static void suspendOtherProvider(int provider) {
        if (provider == TranslationPlaybackState.YANDEX) GoogleVoiceOverTranslationPatch.suspendTranslation();
        else VoiceOverTranslationPatch.suspendTranslation();
    }

    static void enforcePause() {
        if (!state.isWaiting() || !pauseEnabled(state.provider())) return;
        internalChange = true;
        try { VideoInformation.setPlayerPlaying(false); }
        finally { internalChange = false; }
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "SameParameterValue"})
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

    /** A failed/canceled translation leaves the video paused, with manual playback available. */
    static void failed(int provider, String videoId) {
        state.cancel(provider, videoId);
    }

    static boolean usesGoogle() { return state.provider() == TranslationPlaybackState.GOOGLE; }
}
