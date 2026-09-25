/* Copyright (C) 2026 COOLak. Licensed under GPL-3.0-only. */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.Format;
import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.StreamingData;
import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.VideoDetails;

/** Short-lived, in-memory native audio sources, keyed by the response's video ID. */
final class VotAudioSourceCache {
    record Source(String url, int itag, long fileSize, String mimeType, int bitrate,
                  String userAgent) { }
    private record Entry(List<Source> sources, long expiresAt) { }

    private static final int MAX_VIDEOS = 8;
    private static final int MAX_SOURCES = 3;
    private static final long MAX_AGE_MS = 5 * 60_000;
    private static final Map<String, Entry> cache = new LinkedHashMap<>();

    private VotAudioSourceCache() { }

    static void put(byte[] streamBytes, byte[] detailsBytes) throws IOException {
        put(streamBytes, detailsBytes, System.currentTimeMillis());
    }

    static synchronized void put(byte[] streamBytes, byte[] detailsBytes, long now) throws IOException {
        String videoId = VideoDetails.parseFrom(detailsBytes).getVideoId();
        if (videoId.isEmpty()) return;
        StreamingData stream = StreamingData.parseFrom(streamBytes);
        List<Format> formats = stream.getAdaptiveFormatsList();
        boolean hasDefaultAudio = formats.stream().anyMatch(f -> f.getAudioTrack().getAudioIsDefault());
        List<Source> sources = new ArrayList<>();
        for (Format format : formats) {
            String mime = format.getMimeType().toLowerCase(Locale.US);
            if (!mime.startsWith("audio/") || !isDirectAudioUrl(format.getUrl())) continue;
            if (hasDefaultAudio && !format.getAudioTrack().getAudioIsDefault()) continue;
            // Preserve the native URL, including its signature and any playback token.
            sources.add(new Source(format.getUrl(), format.getItag(), format.getContentLength(),
                    format.getMimeType(), format.getBitrate(), null));
        }
        sources.sort(Comparator.<Source>comparingInt(s -> s.mimeType().contains("opus") ? 0 : 1)
                .thenComparingInt(s -> s.bitrate() > 0 ? s.bitrate() : Integer.MAX_VALUE));
        if (sources.size() > MAX_SOURCES) sources = sources.subList(0, MAX_SOURCES);
        cache.remove(videoId);
        if (sources.isEmpty()) return;
        long seconds = stream.getExpiresInSeconds();
        long age = seconds > 0 && seconds < MAX_AGE_MS / 1000 ? seconds * 1000 : MAX_AGE_MS;
        cache.put(videoId, new Entry(List.copyOf(sources), now + age));
        while (cache.size() > MAX_VIDEOS) cache.remove(cache.keySet().iterator().next());
    }

    static List<Source> get(String videoId) {
        return get(videoId, System.currentTimeMillis());
    }

    static synchronized List<Source> get(String videoId, long now) {
        Entry entry = cache.get(videoId);
        if (entry == null) return List.of();
        if (now >= entry.expiresAt()) {
            cache.remove(videoId);
            return List.of();
        }
        return entry.sources();
    }

    private static boolean isDirectAudioUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.equals("googlevideo.com") || host.endsWith(".googlevideo.com"))
                    && "/videoplayback".equals(uri.getPath()) && uri.getUserInfo() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
