/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manual protobuf encoder/decoder that avoids conflicts with YouTube's bundled protobuf version.
 * Implements only the subset of protobuf needed for VOT API communication.
 */
public class VotProtobuf {

    // Wire types
    private static final int WIRETYPE_VARINT = 0;
    private static final int WIRETYPE_64BIT = 1;
    private static final int WIRETYPE_LENGTH_DELIMITED = 2;

    // ==================== ENCODER ====================

    /**
     * Encode a YandexSessionRequest: { uuid = 1 (string), module = 2 (string) }
     */
    public static byte[] encodeSessionRequest(String uuid, String module) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeString(out, 1, uuid);
            writeString(out, 2, module);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode session request", e);
        }
    }

    /**
     * Encode a VideoTranslationRequest.
     * Field numbers from yandex.proto:
     *   url = 3 (string)
     *   firstRequest = 5 (bool)
     *   duration = 6 (double)
     *   unknown0 = 7 (int32)
     *   language = 8 (string)
     *   forceSourceLang = 9 (bool)
     *   unknown1 = 10 (int32)
     *   responseLanguage = 14 (string)
     *   unknown2 = 15 (int32)
     *   unknown3 = 16 (int32)
     *   useLivelyVoice = 18 (bool) — live voices from Yandex (more natural TTS)
     *   videoTitle = 19 (string)
     */
    public static byte[] encodeTranslationRequest(
            String url, boolean firstRequest, double duration,
            String language, String responseLanguage, String videoTitle,
            boolean useLiveVoices
    ) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeString(out, 3, url);
            writeBool(out, 5, firstRequest);
            writeDouble(out, duration);
            writeInt32(out, 7, 1);          // unknown0
            writeString(out, 8, language);
            writeBool(out, 9, false);       // forceSourceLang
            writeInt32(out, 10, 0);         // unknown1
            writeString(out, 14, responseLanguage);
            writeInt32(out, 15, 1);         // unknown2
            writeInt32(out, 16, 2);         // unknown3
            writeBool(out, 18, useLiveVoices);  // useLivelyVoice — live voices
            if (videoTitle != null && !videoTitle.isEmpty()) {
                writeString(out, 19, videoTitle);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode translation request", e);
        }
    }

    /**
     * Encode upstream SubtitlesRequest: url = 1, language = 2 (both strings).
     * Translation uses different field numbers, but shares these wire-format helpers.
     */
    public static byte[] encodeSubtitlesRequest(String url, String language) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (url != null && !url.isEmpty()) writeString(out, 1, url);
            if (language != null && !language.isEmpty()) writeString(out, 2, language);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode subtitles request", e);
        }
    }

    public static byte[] encodeAudioRequest(String translationId, String url, String fileId, byte[] audioData) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeString(out, 1, translationId);
            writeString(out, 2, url);

            ByteArrayOutputStream audioInfoOut = new ByteArrayOutputStream();
            writeString(audioInfoOut, 1, fileId);
            if (audioData != null && audioData.length > 0) {
                writeBytes(audioInfoOut, audioData);
            }

            byte[] audioInfoBytes = audioInfoOut.toByteArray();
            writeMessage(out, 6, audioInfoBytes);

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode audio request", e);
        }
    }

    public static byte[] encodeEmptyAudioRequest(String translationId, String url) {
        return encodeAudioRequest(
                translationId,
                url,
                "web_api_get_all_generating_urls_data_from_iframe",
                new byte[0]
        );
    }

    public static byte[] encodePartialAudioRequest(
            String translationId, String url, String fileId,
            int audioPartsLength, int version, int chunkId, byte[] audioData
    ) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeString(out, 1, translationId);
            writeString(out, 2, url);

            ByteArrayOutputStream partialAudioBufferOut = new ByteArrayOutputStream();
            writeInt32(partialAudioBufferOut, 1, chunkId);
            writeBytes(partialAudioBufferOut, audioData);

            ByteArrayOutputStream partialAudioInfoOut = new ByteArrayOutputStream();
            writeMessage(partialAudioInfoOut, 1, partialAudioBufferOut.toByteArray());
            writeInt32(partialAudioInfoOut, 2, audioPartsLength);
            writeString(partialAudioInfoOut, 3, fileId);
            writeInt32(partialAudioInfoOut, 4, version);

            writeMessage(out, 4, partialAudioInfoOut.toByteArray());

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode partial audio request", e);
        }
    }

    // ==================== DECODER ====================

    /**
     * Decoded translation response fields.
     */
    public static class TranslationResponse {
        public String url;
        public double duration;
        public int status;
        public int remainingTime = -1;
        public String translationId;
        public String language;
        public String message;
    }

    /**
     * Decoded session response fields.
     */
    public static class SessionResponse {
        public String secretKey;
        public int expires;
    }

    /**
     * Decode a YandexSessionResponse: { secretKey = 1 (string), expires = 2 (int32) }
     */
    public static SessionResponse decodeSessionResponse(byte[] data) {
        SessionResponse response = new SessionResponse();
        int pos = 0;

        while (pos < data.length) {
            int[] tagResult = readVarint(data, pos);
            int tag = tagResult[0];
            pos = tagResult[1];

            int fieldNumber = tag >>> 3;
            int wireType = tag & 0x7;

            switch (fieldNumber) {
                case 1: // secretKey (string)
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        int[] lenResult = readVarint(data, pos);
                        int len = lenResult[0];
                        pos = lenResult[1];
                        response.secretKey = new String(data, pos, len, StandardCharsets.UTF_8);
                        pos += len;
                    }
                    break;
                case 2: // expires (int32)
                    if (wireType == WIRETYPE_VARINT) {
                        int[] valResult = readVarint(data, pos);
                        response.expires = valResult[0];
                        pos = valResult[1];
                    }
                    break;
                default:
                    pos = skipField(data, pos, wireType);
                    break;
            }
        }

        return response;
    }

    /**
     * Decode a VideoTranslationResponse:
     *   url = 1 (string), duration = 2 (double), status = 4 (int32),
     *   remainingTime = 5 (int32), translationId = 7 (string),
     *   language = 8 (string), message = 9 (string)
     */
    public static TranslationResponse decodeTranslationResponse(byte[] data) {
        TranslationResponse response = new TranslationResponse();
        int pos = 0;

        while (pos < data.length) {
            int[] tagResult = readVarint(data, pos);
            int tag = tagResult[0];
            pos = tagResult[1];

            int fieldNumber = tag >>> 3;
            int wireType = tag & 0x7;

            switch (fieldNumber) {
                case 1: // url (string)
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        int[] lenResult = readVarint(data, pos);
                        int len = lenResult[0];
                        pos = lenResult[1];
                        response.url = new String(data, pos, len, StandardCharsets.UTF_8);
                        pos += len;
                    }
                    break;
                case 2: // duration (double)
                    if (wireType == WIRETYPE_64BIT) {
                        response.duration = readDouble(data, pos);
                        pos += 8;
                    }
                    break;
                case 4: // status (int32)
                    if (wireType == WIRETYPE_VARINT) {
                        int[] valResult = readVarint(data, pos);
                        response.status = valResult[0];
                        pos = valResult[1];
                    }
                    break;
                case 5: // remainingTime (int32)
                    if (wireType == WIRETYPE_VARINT) {
                        int[] valResult = readVarint(data, pos);
                        response.remainingTime = valResult[0];
                        pos = valResult[1];
                    }
                    break;
                case 7: // translationId (string)
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        int[] lenResult = readVarint(data, pos);
                        int len = lenResult[0];
                        pos = lenResult[1];
                        response.translationId = new String(data, pos, len, StandardCharsets.UTF_8);
                        pos += len;
                    }
                    break;
                case 8: // language (string)
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        int[] lenResult = readVarint(data, pos);
                        int len = lenResult[0];
                        pos = lenResult[1];
                        response.language = new String(data, pos, len, StandardCharsets.UTF_8);
                        pos += len;
                    }
                    break;
                case 9: // message (string)
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        int[] lenResult = readVarint(data, pos);
                        int len = lenResult[0];
                        pos = lenResult[1];
                        response.message = new String(data, pos, len, StandardCharsets.UTF_8);
                        pos += len;
                    }
                    break;
                default:
                    pos = skipField(data, pos, wireType);
                    break;
            }
        }

        return response;
    }

    /** Upstream SubtitlesObject fields consumed by subtitle selection; other fields are skipped. */
    public static class SubtitleTrack {
        public String language = "";
        public String url = "";
        public String translatedLanguage = "";
        public String translatedUrl = "";
    }

    /** Upstream SubtitlesResponse: waiting = 1, repeated SubtitlesObject subtitles = 2. */
    public static class SubtitlesResponse {
        public boolean waiting;
        public final List<SubtitleTrack> subtitles = new ArrayList<>();
    }

    /** Decodes tracks with the same binary helpers as translation, checking nested message bounds. */
    public static SubtitlesResponse decodeSubtitlesResponse(byte[] data) {
        SubtitlesResponse response = new SubtitlesResponse();
        int pos = 0;
        while (pos < data.length) {
            int[] tag = readVarint(data, pos);
            pos = tag[1];
            if (tag[0] == 8) {
                int[] value = readVarint(data, pos);
                response.waiting = value[0] != 0;
                pos = value[1];
            } else if (tag[0] == 18) {
                int[] length = readVarint(data, pos);
                pos = length[1];
                int end = subtitleFieldEnd(data, pos, length[0]);
                response.subtitles.add(decodeSubtitleTrack(Arrays.copyOfRange(data, pos, end)));
                pos = end;
            } else {
                pos = skipSubtitleField(data, pos, tag[0]);
            }
        }
        return response;
    }

    private static SubtitleTrack decodeSubtitleTrack(byte[] data) {
        SubtitleTrack track = new SubtitleTrack();
        int pos = 0;
        while (pos < data.length) {
            int[] tag = readVarint(data, pos);
            pos = tag[1];
            if (tag[0] == 10 || tag[0] == 18 || tag[0] == 34 || tag[0] == 42) {
                int[] length = readVarint(data, pos);
                pos = length[1];
                int end = subtitleFieldEnd(data, pos, length[0]);
                String value = new String(data, pos, length[0], StandardCharsets.UTF_8);
                switch (tag[0]) {
                    case 10 -> track.language = value;
                    case 18 -> track.url = value;
                    case 34 -> track.translatedLanguage = value;
                    case 42 -> track.translatedUrl = value;
                }
                pos = end;
            } else {
                pos = skipSubtitleField(data, pos, tag[0]);
            }
        }
        return track;
    }

    private static int subtitleFieldEnd(byte[] data, int pos, int length) {
        if (length < 0 || length > data.length - pos) {
            throw new IllegalArgumentException("Truncated subtitle protobuf field");
        }
        return pos + length;
    }

    private static int skipSubtitleField(byte[] data, int pos, int tag) {
        int wireType = tag & 7;
        if (tag >>> 3 == 0 || (wireType != 0 && wireType != 1 && wireType != 2 && wireType != 5)) {
            throw new IllegalArgumentException("Invalid subtitle protobuf tag: " + tag);
        }
        int end = skipField(data, pos, wireType);
        subtitleFieldEnd(data, pos, end - pos);
        return end;
    }

    // ==================== LOW-LEVEL ENCODING ====================

    private static void writeTag(ByteArrayOutputStream out, int fieldNumber, int wireType) {
        writeRawVarint(out, (fieldNumber << 3) | wireType);
    }

    private static void writeString(ByteArrayOutputStream out, int fieldNumber, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeTag(out, fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint(out, bytes.length);
        out.write(bytes);
    }

    private static void writeMessage(ByteArrayOutputStream out, int fieldNumber, byte[] value) throws IOException {
        writeTag(out, fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint(out, value.length);
        out.write(value);
    }

    private static void writeBytes(ByteArrayOutputStream out, byte[] value) throws IOException {
        writeMessage(out, 2, value);
    }

    private static void writeInt32(ByteArrayOutputStream out, int fieldNumber, int value) throws IOException {
        writeTag(out, fieldNumber, WIRETYPE_VARINT);
        writeRawVarint(out, value);
    }

    private static void writeBool(ByteArrayOutputStream out, int fieldNumber, boolean value) throws IOException {
        writeTag(out, fieldNumber, WIRETYPE_VARINT);
        out.write(value ? 1 : 0);
    }

    private static void writeDouble(ByteArrayOutputStream out, double value) throws IOException {
        writeTag(out, 6, WIRETYPE_64BIT);
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(value);
        out.write(buf.array());
    }

    private static void writeRawVarint(ByteArrayOutputStream out, int value) {
        // Handle unsigned encoding for potentially large values
        long unsigned = value & 0xFFFFFFFFL;
        while (unsigned > 0x7F) {
            out.write((int) ((unsigned & 0x7F) | 0x80));
            unsigned >>>= 7;
        }
        out.write((int) unsigned);
    }

    // ==================== LOW-LEVEL DECODING ====================

    /**
     * Read a varint from data at the given position.
     * Returns [value, newPosition].
     */
    private static int[] readVarint(byte[] data, int pos) {
        int result = 0;
        int shift = 0;
        while (pos < data.length && shift < 64) {
            int b = data[pos++] & 0xFF;
            // Only the low 32 bits are needed by the API's int32/uint32 fields.
            if (shift < 32) result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return new int[]{result, pos};
            shift += 7;
        }
        throw new IllegalArgumentException("Truncated or oversized protobuf varint");
    }

    /**
     * Read a double (8 bytes, little-endian) from data at the given position.
     */
    private static double readDouble(byte[] data, int pos) {
        ByteBuffer buf = ByteBuffer.wrap(data, pos, 8).order(ByteOrder.LITTLE_ENDIAN);
        return buf.getDouble();
    }

    /**
     * Skip a field of the given wire type. Returns new position.
     */
    private static int skipField(byte[] data, int pos, int wireType) {
        return switch (wireType) {
            case WIRETYPE_VARINT -> {
                while (pos < data.length && (data[pos] & 0x80) != 0) {
                    pos++;
                }
                yield pos + 1;
            }
            case WIRETYPE_64BIT -> pos + 8;
            case WIRETYPE_LENGTH_DELIMITED -> {
                int[] lenResult = readVarint(data, pos);
                yield lenResult[1] + lenResult[0];
            }
            case 5 -> // 32-bit
                    pos + 4;
            default -> data.length; // unknown wire type, skip to end
        };
    }
}
