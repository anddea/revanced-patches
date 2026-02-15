/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/Jav1x.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/Jav1x.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
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
                    if (hadWaiting) {
                        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_not_ready")));
                    }
                    return stream;
                }
                if (result.status() == STATUS_WAITING || result.status() == STATUS_LONG_WAITING) {
                    hadWaiting = true;
                    if (retry == 0) {
                        Utils.runOnMainThread(() -> Utils.showToastShort(str("revanced_vot_stream_waiting")));
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
            for (Object format : formatList) {
                if (StreamingDataOuterClassUtils.isAudioOnlyFormat(format)) {
                    StreamingDataOuterClassUtils.setUrl(format, result.audioUrl());
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
