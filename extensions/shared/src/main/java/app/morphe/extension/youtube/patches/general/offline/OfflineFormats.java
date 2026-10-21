/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import static app.morphe.extension.youtube.patches.spoof.SpoofVideoStreamsPatch.AVAILABLE_CLIENTS;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.Format;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.ThumbnailEntry;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.VideoDetails;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;
import app.morphe.extension.shared.utils.Logger;

/**
 * Resolves the downloadable MP4 streams of a video, the same way the voice-over-translation
 * audio downloader does: one player request per spoof client, until a client answers with
 * plain stream urls (SABR-only clients have none).
 */
final class OfflineFormats {
    static final String CODEC_H264 = "H.264";
    static final String CODEC_AV1 = "AV1";
    // VP9 (WebM) is offered where H.264 and playable AV1 are missing.
    static final String CODEC_VP9 = "VP9";

    private static final String CPN_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
    private static final SecureRandom CPN_RANDOM = new SecureRandom();
    /** The mime type is lower case: {@code codecs="av01.0.12m.10"}. */
    private static final Pattern AV1_TEN_BIT = Pattern.compile("av01\\.\\d+\\.\\d+[mh]\\.1[02]");
    private static final Pattern VP9_PROFILE2 = Pattern.compile("vp09\\.0[23]|profile_id=[23]");
    private static final Pattern QUALITY_LABEL = Pattern.compile("^(\\d+)p(\\d+)?");

    /** Index 0: 8 bit, 1: 10 bit. Filled on first use. */
    @SuppressWarnings("unchecked")
    private static final List<MediaCodecInfo.VideoCapabilities>[] av1Decoders = new List[2];
    private static List<MediaCodecInfo.VideoCapabilities> vp9Decoders;

    private OfflineFormats() {
    }

    /** One downloadable stream. */
    record Stream(String url, int itag, String mimeType, long contentLength, int height, int fps,
                  String codec, int bitrate, boolean playable) {
        boolean isWebm() {
            return mimeType.toLowerCase(Locale.US).contains("webm");
        }

        String fileExtension() {
            return isWebm() ? "webm" : "mp4";
        }

        /** Identifies the same quality on another client, whose urls and itags may differ. */
        String key() {
            return height + "p" + fps + "/" + codec;
        }
    }

    /** The streams of one video as answered by one client. */
    record Resolved(String videoId, int clientIndex, @Nullable String userAgent,
                    String title, String author, String channelId, long lengthMs,
                    @Nullable String thumbnailUrl, List<Stream> videos, Stream audio,
                    @Nullable Stream webmAudio) {
        /** The audio stream that goes with {@code video}: Opus for WebM, AAC for MP4. */
        Stream audioFor(Stream video) {
            return video.isWebm() && webmAudio != null ? webmAudio : audio;
        }
    }

    static int clientCount() {
        return AVAILABLE_CLIENTS.size();
    }

    /**
     * Requests the player response from each client starting at {@code firstClient}.
     *
     * @return The first usable answer, or null if no client has MP4 video and AAC audio.
     */
    @Nullable
    static Resolved resolve(String videoId, int firstClient) {
        Map<String, String> headers = SpoofVideoStreamsPatch.currentVideoRequestHeader;
        // AuthUtils retains headers across incognito changes. Do not revive a signed-in session there.
        if ((headers == null || headers.isEmpty()) && !AuthUtils.isNotLoggedIn()) {
            headers = AuthUtils.getRequestHeader();
        }
        if (headers == null) headers = Collections.emptyMap();

        for (int index = Math.max(0, firstClient); index < AVAILABLE_CLIENTS.size(); index++) {
            ClientType client = AVAILABLE_CLIENTS.get(index);
            final int clientIndex = index;
            try {
                Logger.printInfo(() -> "Resolving " + videoId + " with client " + client.name());
                StreamOrDetailsDataRequest.StreamData stream =
                        StreamOrDetailsDataRequest.fetchDownloadStream(videoId, client, headers);
                if (stream == null) {
                    Logger.printInfo(() -> "No playable stream response from " + client.name());
                    continue;
                }
                PlayerResponse response = PlayerResponse.parseFrom(stream.streamingData());
                Resolved resolved = parse(videoId, clientIndex, client.userAgent, response);
                if (resolved != null) {
                    Logger.printInfo(() -> "Resolved " + videoId + " with client " + client.name()
                            + ": " + resolved.videos().size() + " video streams");
                    return resolved;
                }
                Logger.printInfo(() -> "Client " + client.name() + " has no MP4 video with AAC audio");
            } catch (Exception ex) {
                Logger.printInfo(() -> "Could not resolve " + videoId + " with client " + client.name(), ex);
            }
        }
        return null;
    }

    @Nullable
    private static Resolved parse(String videoId, int clientIndex, @Nullable String userAgent,
                                  PlayerResponse response) {
        // One stream per quality and codec. The largest file of a quality is the best encode.
        Map<String, Stream> videos = new LinkedHashMap<>();
        Map<String, Stream> vp9Videos = new LinkedHashMap<>();
        Stream bestAudio = null;
        boolean bestAudioIsOriginal = false;
        Stream bestOpus = null;
        boolean bestOpusIsOriginal = false;

        for (Format format : response.getStreamingData().getAdaptiveFormatsList()) {
            String url = format.getUrl();
            if (url.isEmpty()) continue;
            String mimeType = format.getMimeType().toLowerCase(Locale.US);
            long length = format.getContentLength() > 0 ? format.getContentLength() : parseClen(url);

            if (mimeType.startsWith("video/mp4")) {
                final boolean isAv1 = mimeType.contains("av01");
                if (!isAv1 && !mimeType.contains("avc1")) continue;

                // label and key follow YouTube's quality label, not the pixel height.
                int[] quality = qualityOf(format);
                if (quality == null) continue;
                final int height = quality[0];
                final int fps = quality[1];

                // AV1 this device cannot decode is listed too, marked as such in the picker.
                final boolean playable = !isAv1 || canDecodeAv1(format.getWidth(), pixelHeight(format, height), decoderFps(format, fps), mimeType);
                final String codec = isAv1 ? CODEC_AV1 : CODEC_H264;

                Stream candidate = new Stream(addCpn(url), format.getItag(), format.getMimeType(),
                        length, height, fps, codec, format.getBitrate(), playable);
                Stream existing = videos.get(candidate.key());
                if (existing == null || candidate.bitrate() > existing.bitrate()) {
                    videos.put(candidate.key(), candidate);
                }
            } else if (mimeType.startsWith("video/webm") && (mimeType.contains("vp9") || mimeType.contains("vp09"))) {
                if (VP9_PROFILE2.matcher(mimeType).find()) continue; // HDR (profile 2) is skipped.
                int[] quality = qualityOf(format);
                if (quality == null) continue;
                final int height = quality[0];
                final int fps = quality[1];

                Stream candidate = new Stream(addCpn(url), format.getItag(), format.getMimeType(),
                        length, height, fps, CODEC_VP9, format.getBitrate(),
                        canDecodeVp9(format.getWidth(), pixelHeight(format, height), decoderFps(format, fps)));
                Stream existing = vp9Videos.get(candidate.key());
                if (existing == null || candidate.bitrate() > existing.bitrate()) {
                    vp9Videos.put(candidate.key(), candidate);
                }
            } else if (mimeType.startsWith("audio/webm") && mimeType.contains("opus")) {
                boolean original = url.contains("acont%3Doriginal") || url.contains("acont=original");
                boolean dubbed = url.contains("acont%3Ddubbed") || url.contains("acont=dubbed");
                boolean drc = url.contains("drc%3D1") || url.contains("drc=1");
                if (dubbed || drc) continue;

                Stream candidate = new Stream(addCpn(url), format.getItag(), format.getMimeType(),
                        length, 0, 0, "Opus", format.getBitrate(), true);
                if (bestOpus == null
                        || (original && !bestOpusIsOriginal)
                        || (original == bestOpusIsOriginal && candidate.bitrate() > bestOpus.bitrate())) {
                    bestOpus = candidate;
                    bestOpusIsOriginal = original;
                }
            } else if (mimeType.startsWith("audio/mp4") && mimeType.contains("mp4a")) {
                // Multi-language videos list dubbed tracks as well. The original is marked in the url.
                boolean original = url.contains("acont%3Doriginal") || url.contains("acont=original");
                boolean dubbed = url.contains("acont%3Ddubbed") || url.contains("acont=dubbed");
                boolean drc = url.contains("drc%3D1") || url.contains("drc=1");
                if (dubbed || drc) continue;

                Stream candidate = new Stream(addCpn(url), format.getItag(), format.getMimeType(),
                        length, 0, 0, "AAC", format.getBitrate(), true);
                if (bestAudio == null
                        || (original && !bestAudioIsOriginal)
                        || (original == bestAudioIsOriginal && candidate.bitrate() > bestAudio.bitrate())) {
                    bestAudio = candidate;
                    bestAudioIsOriginal = original;
                }
            }
        }

        // VP9 only fills qualities that have neither H.264 nor AV1 this device plays, and only where
        // a hardware decoder takes the stream and the WebM can be written (Opus audio present).
        if (bestOpus != null) {
            for (Stream vp9 : vp9Videos.values()) {
                if (!vp9.playable()) continue;
                boolean covered = false;
                for (Stream other : videos.values()) {
                    if (other.playable() && other.height() == vp9.height() && other.fps() == vp9.fps()) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) videos.put(vp9.key(), vp9);
            }
        }
        if (bestAudio == null) {
            // Without AAC only WebM can be saved.
            videos.values().removeIf(stream -> !stream.isWebm());
        }
        if (videos.isEmpty()) return null;

        List<Stream> sorted = new ArrayList<>(videos.values());
        // Within one quality: H.264, playable AV1, VP9, then AV1 this device cannot play.
        sorted.sort(Comparator.comparingInt(Stream::height).reversed()
                .thenComparing(Comparator.comparingInt(Stream::fps).reversed())
                .thenComparingInt(OfflineFormats::codecRank));

        VideoDetails details = response.getVideoDetails();
        String title = details.getTitle().isBlank() ? videoId : details.getTitle();
        long lengthMs = details.getLengthSeconds() * 1000L;

        return new Resolved(videoId, clientIndex, userAgent, title, details.getOwnerChannelName(),
                details.getChannelId(), lengthMs, largestThumbnail(details), sorted, bestAudio, bestOpus);
    }

    /**
     * The quality as YouTube labels it, e.g. 1080p60 for a 1920x960 stream or 1440p for 2560x1280:
     * {@code {label number, frame rate}}. The label is absent or unparsable only rarely, then the
     * shorter side of the picture stands in (vertical videos count by their width).
     *
     * @return Null if neither tells the size.
     */
    @Nullable
    private static int[] qualityOf(Format format) {
        int height = 0;
        int fps = 0;
        Matcher matcher = QUALITY_LABEL.matcher(format.getQualityLabel());
        if (matcher.find()) {
            height = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) fps = Integer.parseInt(matcher.group(2));
        }
        if (height <= 0) {
            height = Math.min(format.getWidth() > 0 ? format.getWidth() : Integer.MAX_VALUE, format.getHeight());
            if (height <= 0 || height == Integer.MAX_VALUE) return null;
        }
        if (fps <= 0) fps = 30;
        return new int[]{height, fps};
    }

    /** The real picture height, which the decoder capability checks need (not the label number). */
    private static int pixelHeight(Format format, int labelHeight) {
        return format.getHeight() > 0 ? format.getHeight() : labelHeight;
    }

    /** The real frame rate for decoder checks; the label has none below 50 fps. */
    private static int decoderFps(Format format, int labelFps) {
        return format.getFps() > 0 ? format.getFps() : labelFps;
    }

    private static int codecRank(Stream stream) {
        if (CODEC_H264.equals(stream.codec())) return 0;
        if (CODEC_AV1.equals(stream.codec())) return stream.playable() ? 1 : 3;
        return 2;
    }

    @Nullable
    private static String largestThumbnail(VideoDetails details) {
        String url = null;
        int widest = 0;
        for (ThumbnailEntry entry : details.getThumbnail().getThumbnailsList()) {
            if (entry.getWidth() >= widest && !entry.getUrl().isBlank()) {
                widest = entry.getWidth();
                url = entry.getUrl();
            }
        }
        return url;
    }

    /**
     * H.264 is always offered. AV1 is offered per stream, only if the saved file can be written (the
     * platform muxer needs Android 14 for AV1 in MP4) and played: a hardware decoder has to handle
     * the stream's size, frame rate and bit depth.
     * <p>
     * Software AV1 decoders are not accepted. The Android software decoder is limited to 2048 pixels
     * wide and, by its own report, to well under 60 fps at 1080p, so on devices without an AV1 chip
     * the offline player showed audio over a grey picture. YouTube offers H.264 up to 1080p, so the
     * software decoder would only be useful where it cannot do the job.
     */
    static boolean canDecodeAv1(int width, int height, int fps, String mimeType) {
        if (Build.VERSION.SDK_INT < 34) return false;
        if (width <= 0) width = (int) Math.round(height * 16 / 9.0 / 2) * 2;

        final boolean tenBit = AV1_TEN_BIT.matcher(mimeType).find();
        for (MediaCodecInfo.VideoCapabilities capabilities : av1HardwareDecoders(tenBit)) {
            try {
                if (capabilities.areSizeAndRateSupported(width, height, fps)) return true;
            } catch (Exception ex) {
                Logger.printDebug(() -> "AV1 capability query failed", ex);
            }
        }
        return false;
    }

    /** Hardware AV1 decoders (not the secure variants) that handle the wanted bit depth. */
    private static List<MediaCodecInfo.VideoCapabilities> av1HardwareDecoders(boolean tenBit) {
        List<MediaCodecInfo.VideoCapabilities> decoders = av1Decoders[tenBit ? 1 : 0];
        if (decoders != null) return decoders;

        decoders = new ArrayList<>();
        try {
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (info.isEncoder() || !info.isHardwareAccelerated() || info.isSoftwareOnly()) continue;
                for (String type : info.getSupportedTypes()) {
                    if (!"video/av01".equalsIgnoreCase(type)) continue;
                    MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(type);
                    MediaCodecInfo.VideoCapabilities video = capabilities.getVideoCapabilities();
                    if (video == null
                            || capabilities.isFeatureRequired(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)
                            || !supportsAv1Profile(capabilities, tenBit)) {
                        continue;
                    }
                    final String name = info.getName();
                    Logger.printInfo(() -> "AV1 hardware decoder " + name + (tenBit ? " (10 bit)" : ""));
                    decoders.add(video);
                }
            }
        } catch (Exception ex) {
            Logger.printInfo(() -> "Could not query AV1 decoders", ex);
        }
        av1Decoders[tenBit ? 1 : 0] = decoders;
        return decoders;
    }

    private static boolean supportsAv1Profile(MediaCodecInfo.CodecCapabilities capabilities, boolean tenBit) {
        for (MediaCodecInfo.CodecProfileLevel level : capabilities.profileLevels) {
            if (tenBit) {
                if (level.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10
                        || level.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10
                        || level.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus) {
                    return true;
                }
            } else if (level.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hardware VP9 decoder (profile 0, not secure-only) that takes this size and frame rate.
     * Software VP9 (libvpx) is not accepted, for the same reason as software AV1.
     */
    static boolean canDecodeVp9(int width, int height, int fps) {
        if (width <= 0) width = (int) Math.round(height * 16 / 9.0 / 2) * 2;
        for (MediaCodecInfo.VideoCapabilities capabilities : vp9HardwareDecoders()) {
            try {
                if (capabilities.areSizeAndRateSupported(width, height, fps)) return true;
            } catch (Exception ex) {
                Logger.printDebug(() -> "VP9 capability query failed", ex);
            }
        }
        return false;
    }

    private static synchronized List<MediaCodecInfo.VideoCapabilities> vp9HardwareDecoders() {
        if (vp9Decoders != null) return vp9Decoders;
        List<MediaCodecInfo.VideoCapabilities> decoders = new ArrayList<>();
        try {
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (info.isEncoder() || !info.isHardwareAccelerated() || info.isSoftwareOnly()) continue;
                for (String type : info.getSupportedTypes()) {
                    if (!"video/x-vnd.on2.vp9".equalsIgnoreCase(type)) continue;
                    MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(type);
                    MediaCodecInfo.VideoCapabilities video = capabilities.getVideoCapabilities();
                    if (video == null
                            || capabilities.isFeatureRequired(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)) {
                        continue;
                    }
                    boolean profile0 = false;
                    for (MediaCodecInfo.CodecProfileLevel level : capabilities.profileLevels) {
                        if (level.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile0) profile0 = true;
                    }
                    if (!profile0) continue;
                    final String name = info.getName();
                    Logger.printInfo(() -> "VP9 hardware decoder " + name);
                    decoders.add(video);
                }
            }
        } catch (Exception ex) {
            Logger.printInfo(() -> "Could not query VP9 decoders", ex);
        }
        vp9Decoders = decoders;
        return decoders;
    }

    private static String addCpn(String url) {
        StringBuilder cpn = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            cpn.append(CPN_ALPHABET.charAt(CPN_RANDOM.nextInt(CPN_ALPHABET.length())));
        }
        return Uri.parse(url).buildUpon().appendQueryParameter("cpn", cpn.toString()).build().toString();
    }

    private static long parseClen(String url) {
        try {
            String clen = Uri.parse(url).getQueryParameter("clen");
            return clen == null ? -1 : Long.parseLong(clen);
        } catch (Exception ex) {
            return -1;
        }
    }
}
