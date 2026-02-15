package app.morphe.extension.youtube.patches.voiceovertranslation;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.shared.VideoState;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.showToastShort;

@SuppressWarnings("unused")
public class VoiceOverTranslationPatch {

    private static final String TAG = "VOT";

    private static final int STATUS_FAILED = 0;
    private static final int STATUS_FINISHED = 1;
    private static final int STATUS_WAITING = 2;
    private static final int STATUS_LONG_WAITING = 3;
    private static final int STATUS_PART_CONTENT = 5;
    private static final int STATUS_AUDIO_REQUESTED = 6;
    private static final long PAUSE_DETECTION_TIMEOUT_MS = 1500;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final AtomicReference<MediaPlayer> mediaPlayer = new AtomicReference<>(null);
    private static final AtomicBoolean isTranslating = new AtomicBoolean(false);
    private static final AtomicReference<String> currentTranslatedVideoId = new AtomicReference<>("");
    private static volatile boolean isPaused = false;
    private static float lastAppliedPlaybackSpeed = 1.0f;
    private static Object audioFocusRequest;
    private static volatile long lastVideoTimeMs = -1;
    private static final long SEEK_DRIFT_THRESHOLD_MS = 20000;
    private static final long USER_SEEK_JUMP_MS = 3000;

    private static final Runnable pauseCheckRunnable = () -> {
        if (!isPaused) {

            pauseAudio();
        }
    };

    private static volatile String pendingVideoId = "";
    private static volatile String pendingVideoTitle = "";
    private static volatile long pendingVideoLength = 0L;
    private static volatile boolean pendingIsLive = false;

    public static void initialize() {
        VideoState.addOnPlayingListener(() -> mainHandler.post(() -> {
            if (VideoState.getCurrent() != VideoState.PLAYING) return;
            resumeAudio(-1);
        }));
        VideoInformation.addOnPlaybackSpeedChangeListener(() -> mainHandler.post(() -> {
            if (VideoState.getCurrent() != VideoState.PLAYING) return;
            MediaPlayer p = mediaPlayer.get();
            if (p != null) applyPlaybackSpeedToPlayer(p);
        }));
    }

    public static void newVideoStarted(
            String channelId, String channelName,
            String videoId, String videoTitle,
            long videoLength, boolean isLive
    ) {
        if (!Settings.VOT_ENABLED.get()) return;
        String newId = videoId != null ? videoId : "";
        if (!newId.equals(currentTranslatedVideoId.get())) {
            stopAudioPlayback();
        }
        pendingVideoId = newId;
        pendingVideoTitle = videoTitle != null ? videoTitle : "";
        pendingVideoLength = videoLength;
        pendingIsLive = isLive;

    }

    public static void toggleTranslation() {
        if (!Settings.VOT_ENABLED.get()) return;

        if (isTranslationActive()) {
            stopAudioPlayback();
            showToastShort(str("revanced_vot_stopped"));
            return;
        }

        if (pendingIsLive) {
            showToastShort(str("revanced_vot_unavailable_live"));
            return;
        }
        if (pendingVideoLength > 4 * 60 * 60 * 1000L) {
            showToastShort(str("revanced_vot_unavailable_too_long"));
            return;
        }
        String sourceLang = Settings.VOT_SOURCE_LANGUAGE.get();
        String targetLang = Settings.VOT_TARGET_LANGUAGE.get();
        if (!sourceLang.isEmpty() && !"auto".equalsIgnoreCase(sourceLang) && sourceLang.equals(targetLang)) {
            showToastShort(str("revanced_vot_unavailable_same_language"));
            return;
        }
        if (pendingVideoId == null || pendingVideoId.isEmpty()) return;

        double durationSeconds = pendingVideoLength / 1000.0;
        showToastShort(str("revanced_vot_started"));
        Utils.runOnBackgroundThread(() -> requestTranslation(
                pendingVideoId, pendingVideoTitle,
                sourceLang, targetLang,
                durationSeconds
        ));
    }

    public static boolean isTranslationActive() {
        MediaPlayer mp = mediaPlayer.get();
        if (mp == null) return false;
        return currentTranslatedVideoId.get() != null && !currentTranslatedVideoId.get().isEmpty();
    }

    public static void setVideoTime(long videoTimeMillis) {
        if (!Settings.VOT_ENABLED.get()) return;
        if (isPaused) {
            final long time = videoTimeMillis;
            mainHandler.postDelayed(() -> resumeAudio(time), 80);
        }
        mainHandler.removeCallbacks(pauseCheckRunnable);
        mainHandler.postDelayed(pauseCheckRunnable, PAUSE_DETECTION_TIMEOUT_MS);
        MediaPlayer mp = mediaPlayer.get();
        if (mp == null || !mp.isPlaying()) return;
        final long time = videoTimeMillis;
        mainHandler.post(() -> {
            MediaPlayer p = mediaPlayer.get();
            if (p == null || !p.isPlaying()) return;
            applyPlaybackSpeedToPlayer(p);
            try {
                int audioPos = p.getCurrentPosition();
                long drift = Math.abs(audioPos - time);
                long prev = lastVideoTimeMs;
                lastVideoTimeMs = time;
                boolean userSeeked = prev >= 0 && (time < prev - 500 || time > prev + USER_SEEK_JUMP_MS);
                if (userSeeked || drift > SEEK_DRIFT_THRESHOLD_MS) {
                    p.seekTo((int) time);
                    applyPlaybackSpeedToPlayer(p);
                }
            } catch (IllegalStateException ignored) { }
        });
    }

    private static void requestTranslation(
            String videoId, String videoTitle,
            String sourceLang, String targetLang,
            double durationSeconds
    ) {
        if (isTranslating.getAndSet(true)) return;
        try {
            String youtubeUrl = "https://youtu.be/" + videoId;
            VotApiClient.TranslationResult result = VotApiClient.requestTranslation(
                    youtubeUrl, durationSeconds, sourceLang, targetLang, videoTitle);
            if (result == null) return;
            switch (result.status()) {
                case STATUS_FINISHED:
                case STATUS_PART_CONTENT:
                    if (result.audioUrl() != null && !result.audioUrl().isEmpty()) {
                        final String url = result.audioUrl();
                        Utils.runOnMainThread(() -> startAudioPlayback(videoId, url));
                    }
                    break;
                case STATUS_WAITING:
                case STATUS_LONG_WAITING:
                    Utils.runOnMainThread(() -> showToastShort(str("revanced_vot_stream_waiting")));
                    int waitTime = result.remainingTime() > 0 ? result.remainingTime() : 5;
                    pollTranslation(videoId, videoTitle, youtubeUrl, durationSeconds, sourceLang, targetLang, waitTime);
                    break;
                case STATUS_AUDIO_REQUESTED:
                    handleAudioRequested(videoId, youtubeUrl, result.translationId(), durationSeconds, sourceLang, targetLang, videoTitle);
                    break;
                case STATUS_FAILED:
                default:
                    break;
            }
        } catch (Exception e) {
            Logger.printException(() -> "requestTranslation failed", e);
        } finally {
            isTranslating.set(false);
        }
    }

    private static void pollTranslation(
            String videoId, String videoTitle,
            String url, double duration,
            String sourceLang, String targetLang,
            int waitSeconds
    ) {
        int maxRetries = 30;
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                Thread.sleep(waitSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            String currentVideoId = VideoInformation.getVideoId();
            if (!videoId.equals(currentVideoId)) return;
            try {
                VotApiClient.TranslationResult result = VotApiClient.requestTranslation(
                        url, duration, sourceLang, targetLang, videoTitle);
                if (result == null) continue;
                if (result.status() == STATUS_FINISHED || result.status() == STATUS_PART_CONTENT) {
                    if (result.audioUrl() != null && !result.audioUrl().isEmpty()) {
                        Utils.runOnMainThread(() -> startAudioPlayback(videoId, result.audioUrl()));
                        return;
                    }
                } else if (result.status() == STATUS_FAILED) {
                    return;
                }
                waitSeconds = result.remainingTime() > 0 ? result.remainingTime() : 5;
            } catch (Exception e) {
                Logger.printException(() -> "pollTranslation failure", e);
            }
        }
    }

    private static void handleAudioRequested(
            String videoId, String url, String translationId,
            double duration, String sourceLang, String targetLang,
            String videoTitle
    ) {
        try {
            VotApiClient.sendFailedAudio(url);
            VotApiClient.sendEmptyAudio(url, translationId);
            Thread.sleep(3000);
            VotApiClient.TranslationResult result = VotApiClient.requestTranslation(
                    url, duration, sourceLang, targetLang, videoTitle);
            if (result != null && (result.status() == STATUS_WAITING || result.status() == STATUS_LONG_WAITING)) {
                Utils.runOnMainThread(() -> showToastShort(str("revanced_vot_stream_waiting")));
                int waitTime = result.remainingTime() > 0 ? result.remainingTime() : 10;
                pollTranslation(videoId, videoTitle, url, duration, sourceLang, targetLang, waitTime);
            } else if (result != null && (result.status() == STATUS_FINISHED || result.status() == STATUS_PART_CONTENT) && result.audioUrl() != null) {
                Utils.runOnMainThread(() -> startAudioPlayback(videoId, result.audioUrl()));
            }
        } catch (Exception e) {
            Logger.printException(() -> "handleAudioRequested failed", e);
        }
    }

    private static void startAudioPlayback(String videoId, String audioUrl) {
        stopAudioPlayback();
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build());
            mp.setDataSource(audioUrl);
            mp.setOnPreparedListener(player -> {
                Utils.runOnMainThread(VoiceOverTranslationPatch::requestAudioFocusForTranslation);
                float vol = Settings.VOT_TRANSLATION_VOLUME.get() / 100.0f;
                player.setVolume(vol, vol);
                long videoTime = VideoInformation.getVideoTime();
                if (videoTime > 0) player.seekTo((int) videoTime);

                // Only start playing if the video is actually playing.
                // Otherwise, mark it as paused so it starts automatically when the user hits play.
                if (VideoState.getCurrent() == VideoState.PLAYING) {
                    // applyPlaybackSpeedToPlayer uses setPlaybackParams, which may
                    // auto-start the player on some Android versions.
                    applyPlaybackSpeedToPlayer(player);
                    player.start();
                } else {
                    isPaused = true;
                }
            });
            mp.setOnErrorListener((p, what, extra) -> true);
            mediaPlayer.set(mp);
            currentTranslatedVideoId.set(videoId != null ? videoId : "");
            mp.prepareAsync();
        } catch (IOException e) {
            Logger.printException(() -> "startAudioPlayback failed for videoId: " + videoId, e);
        }
    }

    public static void stopAudioPlayback() {
        mainHandler.removeCallbacks(pauseCheckRunnable);
        MediaPlayer mp = mediaPlayer.getAndSet(null);
        if (mp != null) {
            try {
                if (mp.isPlaying()) mp.stop();
                mp.release();
            } catch (Exception ignored) { }
        }
        currentTranslatedVideoId.set("");
        isPaused = false;
        lastAppliedPlaybackSpeed = 1.0f;
        lastVideoTimeMs = -1;
        abandonAudioFocus();
    }

    private static void requestAudioFocusForTranslation() {
        try {
            Context ctx = RootView.getContext();
            if (ctx == null) return;
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest req = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                    .build();
                int result = am.requestAudioFocus(req);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioFocusRequest = req;
                } else {
                    Logger.printDebug(() -> "requestAudioFocus failed: " + result);
                }
            } else {
                int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Logger.printDebug(() -> "requestAudioFocus failed: " + result);
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> "requestAudioFocusForTranslation failed", e);
        }
    }

    private static void abandonAudioFocus() {
        try {
            Context ctx = RootView.getContext();
            if (ctx == null) return;
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                am.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequest);
                audioFocusRequest = null;
            } else {
                am.abandonAudioFocus(null);
            }
        } catch (Exception ignored) { }
    }

    public static void pauseAudio() {
        MediaPlayer mp = mediaPlayer.get();
        if (mp != null) {
            try {
                if (mp.isPlaying()) {
                    mp.pause();
                    isPaused = true;
                }
            } catch (Exception ignored) { }
        }
    }

    public static void resumeAudio(long videoTimeMillis) {
        if (VideoState.getCurrent() != VideoState.PLAYING) return;
        MediaPlayer mp = mediaPlayer.get();
        if (mp == null || !isPaused) return;
        try {
            long position = videoTimeMillis >= 0 ? videoTimeMillis : VideoInformation.getVideoTime();
            mp.seekTo((int) position);
            applyPlaybackSpeedToPlayer(mp);
            mp.start();
            isPaused = false;
        } catch (Exception ignored) { }
    }

    private static void applyPlaybackSpeedToPlayer(MediaPlayer mp) {
        if (mp == null) return;
        float speed = VideoInformation.getPlaybackSpeedFromPlayer();
        if (speed > 0f) {
            VideoInformation.setPlaybackSpeed(speed);
        } else {
            speed = VideoInformation.getPlaybackSpeed();
        }
        if (speed <= 0f) speed = 1.0f;
        if (speed < 0.25f) speed = 0.25f;
        if (speed > 2.5f) speed = 2.5f;
        try {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);
            mp.setPlaybackParams(params);
            lastAppliedPlaybackSpeed = speed;
        } catch (Exception ignored) { }
    }
}
