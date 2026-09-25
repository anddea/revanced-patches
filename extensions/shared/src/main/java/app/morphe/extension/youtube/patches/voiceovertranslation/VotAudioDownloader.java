/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

/*
 * Userscript protocol behavior ported from ilyhalight/voice-over-translation.
 * https://github.com/ilyhalight/voice-over-translation
 *
 * MIT License
 * 
 * Copyright (c) 2021 [sodapng](https://github.com/sodapng/voice-over-translation)
 * Copyright (c) 2022-present ilyhalight
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import static app.morphe.extension.youtube.patches.spoof.SpoofVideoStreamsPatch.AVAILABLE_CLIENTS;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import app.morphe.extension.youtube.patches.voiceovertranslation.VotAudioSourceCache.Source;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.utils.Logger;

import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.PlayerResponse;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.Format;
import app.morphe.extension.shared.spoof.ClientType;
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch;
import app.morphe.extension.shared.spoof.requests.StreamOrDetailsDataRequest;
import app.morphe.extension.shared.utils.Utils;

public final class VotAudioDownloader {
    private static final int CHUNK_SIZE_BYTES = 5_295_308;
    private static final int CONNECTION_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String CPN_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
    private static final SecureRandom CPN_RANDOM = new SecureRandom();

    private record AudioFormatInfo(
            String url,
            int itag,
            long fileSize,
            String mimeType,
            int bitrate
    ) {
    }

    /** Upload errors must not trigger client fallback or a failed-download notification. */
    static final class AudioUploadException extends RuntimeException {
        AudioUploadException() { super("VOT audio upload failed after successful download"); }
    }

    private VotAudioDownloader() {
    }

    /**
     * Try cached native audio, then the YouTube fallback clients in order. Complete each
     * download before uploading so a late HTTP 403 cannot mix chunks from different clients.
     */
    static boolean downloadAndSend(String videoId, String videoUrl, String translationId) {
        if (isEmpty(videoId) || isEmpty(videoUrl) || isEmpty(translationId)) return false;

        for (Source source : VotAudioSourceCache.get(videoId)) {
            if (downloadAndSendSource(source, videoUrl, translationId, "native player")) return true;
        }
        Map<String, String> headers = SpoofVideoStreamsPatch.currentVideoRequestHeader;
        // AuthUtils retains headers across incognito changes. Do not revive a signed-in session there.
        if ((headers == null || headers.isEmpty()) && !AuthUtils.isNotLoggedIn()) {
            headers = AuthUtils.getRequestHeader();
        }
        if (headers == null) headers = Collections.emptyMap();
        for (ClientType client : AVAILABLE_CLIENTS) {
            String source = "spoof client " + client.name();
            String userAgent = client.userAgent;
            Logger.printInfo(() -> "VOT audio downloader: trying " + source + " for " + videoId);
            try {
                var stream = StreamOrDetailsDataRequest.fetchDownloadStream(videoId, client, headers);
                if (stream == null) throw new IOException("No playable stream response");
                AudioFormatInfo format = selectBestAudioFormat(PlayerResponse.parseFrom(stream.streamingData())
                        .getStreamingData().getAdaptiveFormatsList());
                if (format == null) throw new IOException("No direct audio format");
                if (downloadAndSendSource(new Source(format.url(), format.itag(), format.fileSize(),
                        format.mimeType(), format.bitrate(), userAgent), videoUrl, translationId, source)) return true;
            } catch (AudioUploadException e) {
                throw e;
            } catch (Exception e) {
                Logger.printInfo(() -> "VOT audio downloader: failed using " + source + " for " + videoId, e);
            }
        }
        Logger.printInfo(() -> "VOT audio downloader: all spoof clients failed for " + videoId);
        return false;
    }

    /** Reuse native and fallback sources without mixing uploads after a failed download. */
    private static boolean downloadAndSendSource(Source format, String videoUrl,
                                                String translationId, String source) {
        String userAgent = format.userAgent();
        File audioFile = null;
        boolean downloaded = false;
        try {
            long fileSize = format.fileSize() > 0 ? format.fileSize()
                    : resolveFileSize(format.url(), userAgent);
            if (fileSize <= 0) throw new IOException("Unknown audio size");
            int parts = toPartsCount(fileSize);
            Logger.printInfo(() -> "VOT audio downloader: selected " + source
                    + ", itag=" + format.itag() + ", mime=" + format.mimeType()
                    + ", bitrate=" + format.bitrate() + ", bytes=" + fileSize);
            audioFile = File.createTempFile("vot-source-", ".audio", Utils.getContext().getCacheDir());
            try (FileOutputStream output = new FileOutputStream(audioFile)) {
                for (int i = 0; i < parts; i++) {
                    long start = (long) i * CHUNK_SIZE_BYTES;
                    long end = Math.min(fileSize - 1, start + CHUNK_SIZE_BYTES - 1);
                    output.write(downloadRange(format.url(), start, end, userAgent));
                }
            }
            downloaded = true;
            Logger.printInfo(() -> "VOT audio downloader: download completed using " + source);
            // Match the streaming-upload protocol with one opaque identity per source.
            String fileId = "random-web_mse_proxy-" + UUID.randomUUID();
            try (FileInputStream input = new FileInputStream(audioFile)) {
                for (int i = 0; i < parts; i++) {
                    int size = (int) Math.min(CHUNK_SIZE_BYTES, fileSize - (long) i * CHUNK_SIZE_BYTES);
                    byte[] data = new byte[size];
                    int offset = 0;
                    while (offset < size) {
                        int read = input.read(data, offset, size - offset);
                        if (read < 0) throw new IOException("Incomplete cached audio");
                        offset += read;
                    }
                    // Like handleCommonAudioDownloadRequest, mark completion only on the last chunk.
                    int amount = i == parts - 1 ? parts : 0;
                    boolean sent = VotApiClient.sendPartialAudio(
                            videoUrl, translationId, fileId, amount, 1, i, data);
                    if (!sent) {
                        Logger.printInfo(() -> "VOT audio upload failed after successful download using "
                                + source + "; not trying other clients");
                        throw new AudioUploadException();
                    }
                }
            }
            Logger.printInfo(() -> "VOT audio downloader: completed using " + source);
            return true;
        } catch (AudioUploadException e) {
            throw e;
        } catch (Exception e) {
            if (downloaded) {
                Logger.printInfo(() -> "VOT audio upload failed; keeping successful download source", e);
                throw new AudioUploadException();
            }
            Logger.printInfo(() -> "VOT audio downloader: failed using " + source + ", source unavailable", e);
        } finally {
            if (audioFile != null && !audioFile.delete()) {
                Logger.printDebug(() -> "VOT audio downloader: could not delete temporary audio");
            }
        }
        return false;
    }

    @Nullable
    private static AudioFormatInfo selectBestAudioFormat(Iterable<Format> formats) {
        AudioFormatInfo bestOpus = null;
        AudioFormatInfo bestOther = null;
        for (Format format : formats) {
            String url = format.getUrl();
            String mimeType = format.getMimeType();
            if (isEmpty(url) || !mimeType.toLowerCase(Locale.US).startsWith("audio/")) continue;
            AudioFormatInfo candidate = new AudioFormatInfo(
                    addCpn(url), format.getItag(), parseClen(url), mimeType, format.getBitrate());
            if (mimeType.toLowerCase(Locale.US).contains("opus")) {
                if (bestOpus == null || compareBitrate(candidate, bestOpus) < 0) bestOpus = candidate;
            } else if (bestOther == null || compareBitrate(candidate, bestOther) < 0) {
                bestOther = candidate;
            }
        }
        return bestOpus != null ? bestOpus : bestOther;
    }

    private static int compareBitrate(AudioFormatInfo left, AudioFormatInfo right) {
        int leftBitrate = left.bitrate() > 0 ? left.bitrate() : Integer.MAX_VALUE;
        int rightBitrate = right.bitrate() > 0 ? right.bitrate() : Integer.MAX_VALUE;
        return Integer.compare(leftBitrate, rightBitrate);
    }

    private static String addCpn(String audioUrl) {
        return Uri.parse(audioUrl)
                .buildUpon()
                .appendQueryParameter("cpn", makeCpn())
                .build()
                .toString();
    }

    private static String makeCpn() {
        StringBuilder cpn = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            cpn.append(CPN_ALPHABET.charAt(CPN_RANDOM.nextInt(CPN_ALPHABET.length())));
        }
        return cpn.toString();
    }

    private static long resolveFileSize(String audioUrl, String userAgent) throws IOException {
        long size = parseClen(audioUrl);
        if (size > 0) return size;

        HttpURLConnection connection = openAudioConnection(audioUrl, 0, 0, userAgent);
        try {
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_PARTIAL) {
                String contentRange = connection.getHeaderField("Content-Range");
                size = parseContentRangeSize(contentRange);
                if (size > 0) return size;
            }

            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Audio size request failed: HTTP " + code);
            }
            long contentLength = connection.getContentLengthLong();
            return contentLength > 0 ? contentLength : -1;
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] downloadRange(String audioUrl, long start, long end, String userAgent) throws IOException {
        long expectedSize = end - start + 1;
        if (expectedSize <= 0 || expectedSize > Integer.MAX_VALUE) {
            throw new IOException("Invalid audio range size: " + expectedSize);
        }

        HttpURLConnection connection = openAudioConnection(audioUrl, start, end, userAgent);
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Audio download failed: HTTP " + code);
            }
            if (code == HttpURLConnection.HTTP_OK && start > 0) {
                throw new IOException("Audio server ignored range request");
            }

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = readBytes(inputStream, expectedSize);
                if (bytes.length != expectedSize) {
                    throw new IOException(
                            "Incomplete audio range: expected " + expectedSize
                                    + " bytes, got " + bytes.length
                    );
                }
                String contentRange = connection.getHeaderField("Content-Range");
                if (contentRange != null && !isExpectedContentRange(contentRange, start, end, bytes.length)) {
                    throw new IOException("Unexpected audio Content-Range: " + contentRange);
                }
                return bytes;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openAudioConnection(String audioUrl, long start, long end, String userAgent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(audioUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (userAgent != null) connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static byte[] readBytes(InputStream inputStream, long expectedSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(expectedSize, CHUNK_SIZE_BYTES));
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > expectedSize) {
                throw new IOException("Audio range exceeded expected size");
            }
            out.write(buffer, 0, read);
        }
        if (total == 0) {
            throw new IOException("Empty audio range");
        }
        return out.toByteArray();
    }

    private static boolean isExpectedContentRange(
            String contentRange, long expectedStart, long expectedEnd, int byteCount
    ) {
        if (contentRange == null) return true;
        Matcher matcher = Pattern.compile("^bytes\\s+(\\d+)-(\\d+)/(?:\\d+|\\*)$", Pattern.CASE_INSENSITIVE)
                .matcher(contentRange.trim());
        if (!matcher.matches()) return false;
        try {
            long start = Long.parseLong(Objects.requireNonNull(matcher.group(1)));
            long end = Long.parseLong(Objects.requireNonNull(matcher.group(2)));
            return start == expectedStart
                    && end == expectedStart + byteCount - 1
                    && expectedEnd >= expectedStart;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static long parseClen(String audioUrl) {
        int queryStart = audioUrl.indexOf('?');
        if (queryStart < 0 || queryStart == audioUrl.length() - 1) return -1;

        String query = audioUrl.substring(queryStart + 1);
        String[] params = query.split("&");
        for (String param : params) {
            if (!param.startsWith("clen=")) continue;
            try {
                return Long.parseLong(param.substring(5));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static long parseContentRangeSize(@Nullable String contentRange) {
        if (contentRange == null) return -1;

        int slash = contentRange.lastIndexOf('/');
        if (slash < 0 || slash == contentRange.length() - 1) return -1;

        try {
            return Long.parseLong(contentRange.substring(slash + 1).trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int toPartsCount(long fileSize) throws IOException {
        long parts = (fileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES;
        if (parts <= 0 || parts > Integer.MAX_VALUE) {
            throw new IOException("Invalid audio parts count: " + parts);
        }
        return (int) parts;
    }

    private static boolean isEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }
}
