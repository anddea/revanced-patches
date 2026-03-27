/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.morphe.extension.shared.innertube.utils.StreamingDataOuterClassUtils;

import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

import static app.morphe.extension.shared.utils.StringRef.str;
import app.morphe.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public final class VotStreamReplacer {

    private static volatile String lastReplacedVideoId = null;
    private static volatile String skipReplacementForVideoId = null;
    private static volatile String replaceOnlyForVideoId = null;
    private static final int TRANSLATION_TIMEOUT_SEC = 60;
    private static final int STATUS_FAILED = 0;
    private static final int STATUS_FINISHED = 1;
    private static final int STATUS_WAITING = 2;
    private static final int STATUS_LONG_WAITING = 3;
    private static final int STATUS_PART_CONTENT = 5;
    private static final int MAX_POLL_RETRIES = 30;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vot-stream-replacer");
        t.setDaemon(true);
        return t;
    });

    @Nullable
    public static StreamingData process(@NonNull StreamingData stream, @NonNull String videoId) {
        if (!Settings.VOT_ENABLED.get()) {
            return stream;
        }
        if (videoId.equals(skipReplacementForVideoId)) {
            skipReplacementForVideoId = null;
            return stream;
        }
        if (!videoId.equals(replaceOnlyForVideoId)) {
            return stream;
        }
        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_requesting")));
        String sourceLang = Settings.VOT_SOURCE_LANGUAGE.get();
        String targetLang = Settings.VOT_TARGET_LANGUAGE.get();
        if (!sourceLang.isEmpty() && !"auto".equalsIgnoreCase(sourceLang) && sourceLang.equals(targetLang)) {
            return stream;
        }
        long durationMs = StreamingDataOuterClassUtils.getApproxDurationMsFromFirstFormat(stream);
        double durationSec = durationMs / 1000.0;
        if (durationSec <= 0) durationSec = 60.0;
        if (durationSec > 4 * 3600) {
            return stream;
        }
        List<?> formats = StreamingDataOuterClassUtils.getAdaptiveFormats(stream);
        int audioCount = 0;
        if (formats != null) {
            for (Object f : formats) {
                if (StreamingDataOuterClassUtils.isAudioOnlyFormat(f)) audioCount++;
            }
        }
        String title = VideoInformation.getVideoTitle();
        final double durationSecFinal = durationSec;
        final String titleFinal = title;
        final String youtubeUrlFinal = "https://youtu.be/" + videoId;

        Callable<StreamingData> task = () -> {
            long deadline = System.currentTimeMillis() + TRANSLATION_TIMEOUT_SEC * 1000L;
            int waitSeconds = 5;
            VotApiClient.TranslationResult result = null;
            boolean hadWaiting = false;

            for (int retry = 0; retry < MAX_POLL_RETRIES && System.currentTimeMillis() < deadline; retry++) {
                result = VotApiClient.requestTranslation(
                        youtubeUrlFinal, durationSecFinal, sourceLang, targetLang, titleFinal);
                if (result == null) {
                    try { Thread.sleep(1000L * Math.min(waitSeconds, 10)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return stream; }
                    continue;
                }
                if (result.status() == STATUS_FINISHED || result.status() == STATUS_PART_CONTENT) {
                    break;
                }
                if (result.status() == STATUS_FAILED) {
                    if (Settings.VOT_USE_LIVE_VOICES.get()) {
                        Settings.VOT_USE_LIVE_VOICES.save(false);
                        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_live_voices_unavailable")));
                        continue; // retry with standard TTS
                    }
                    if (hadWaiting) {
                        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_not_ready")));
                    }
                    return stream;
                }
                if (result.status() == STATUS_WAITING || result.status() == STATUS_LONG_WAITING) {
                    hadWaiting = true;
                    if (retry == 0) {
                        int waitSecs = result.remainingTime() > 0 ? result.remainingTime() : 5;
                        String timeStr = VoiceOverTranslationPatch.formatRemainingTime(waitSecs);
                        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_waiting", timeStr)));
                    }
                    waitSeconds = result.remainingTime() > 0 ? result.remainingTime() : 5;
                    waitSeconds = Math.min(waitSeconds, (int) ((deadline - System.currentTimeMillis()) / 1000));
                    if (waitSeconds <= 0) break;
                    try {
                        Thread.sleep(1000L * waitSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return stream;
                    }
                }
            }

            if (result == null || result.audioUrl() == null || result.audioUrl().isEmpty()) {
                if (hadWaiting) {
                    Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_not_ready")));
                }
                return stream;
            }
            if (result.status() != STATUS_FINISHED && result.status() != STATUS_PART_CONTENT) {
                if (hadWaiting) {
                    Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_not_ready")));
                }
                return stream;
            }
            List<?> formatList = StreamingDataOuterClassUtils.getAdaptiveFormats(stream);
            if (formatList == null || formatList.isEmpty()) {
                return stream;
            }
            int replaced = 0;
            String audioUrl = result.audioUrl();
            if (Settings.VOT_AUDIO_PROXY_ENABLED.get()) {
                audioUrl = VotApiClient.toProxyAudioUrl(audioUrl);
            }
            for (Object format : formatList) {
                if (StreamingDataOuterClassUtils.isAudioOnlyFormat(format)) {
                    StreamingDataOuterClassUtils.setUrl(format, audioUrl);
                    replaced++;
                }
            }
            if (replaced > 0) {
                lastReplacedVideoId = videoId;
                replaceOnlyForVideoId = null;
                Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_ready")));
            }
            return stream;
        };

        try {
            Future<StreamingData> future = executor.submit(task);
            return future.get(TRANSLATION_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_not_ready")));
            return stream;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return stream;
        } catch (ExecutionException e) {
            return stream;
        }
    }
}
