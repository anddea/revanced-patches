package app.revanced.extension.shared.patches.spoof;

import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings({"deprecation", "unused"})
public class StreamingDataOuterClassPatch {
    public interface StreamingDataMessage {
        // Methods are added to YT classes during patching.
        StreamingData parseFrom(ByteBuffer responseProto);
    }

    // com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData
    // It is based on YouTube 19.47.53, but 'field c' to 'field k' are the same regardless of the YouTube version.
    private interface StreamingDataFields {
        // int
        String unknownField_c = "c";

        // long
        String expiresInSeconds = "d";

        // List<?>
        String formats = "e";

        // List<?>
        String adaptiveFormats = "f";

        // List<?>
        String metadataFormats = "g";

        // String
        String dashManifestUrl = "h";

        // String
        String hlsManifestUrl = "i";

        // String
        String unknownField_j = "j";

        // String
        String drmParams = "k";

        // String
        String serverAbrStreamingUrl = "l";

        // List<?>
        // licenseInfos? or initialAuthorizedDrmTrackTypes?
        String unknownField_m = "m";
    }

    // It is based on YouTube 19.47.53, but all fields are the same regardless of the YouTube version.
    private interface FormatFields {
        // Double
        String unknownField_A = "A";

        // Double
        String unknownField_B = "B";

        // int
        String unknownField_C = "C";

        // Message
        String unknownField_D = "D";

        // int
        String unknownField_E = "E";

        // long
        String approxDurationMs = "F";

        // long
        String audioSampleRate = "G";

        // int
        String audioChannels = "H";

        // Float
        String loudnessDb = "I";

        // Float
        String unknownField_J = "J";

        // String
        String isDrc = "K";

        // Byte
        String unknownField_M = "M";

        // int
        String unknownField_c = "c";

        // int
        String unknownField_d = "d";

        // int
        String itag = "e";

        // String
        String url = "f";

        // String
        String mimeType = "g";

        // int
        String bitrate = "h";

        // int
        String averageBitrate = "i";

        // int
        String width = "j";

        // int
        String height = "k";

        // int
        String fps = "m";

        // Message
        String unknownField_n = "n";

        // Message
        String unknownField_o = "o";

        // long
        String lastModified = "p";

        // long
        String contentLength = "q";

        // String
        String xtags = "r";

        // Message
        String drmFamilies = "s";

        // String
        String qualityLabel = "t";

        // int
        String unknownField_u = "u";

        // int
        String unknownField_v = "v";

        // int
        String unknownField_w = "w";

        // Message
        String audioTrack = "x";

        // Message
        String unknownField_y = "y";

        // Message
        String unknownField_z = "z";
    }

    // It is based on YouTube 19.47.53, but all fields are the same regardless of the YouTube version.
    private interface AudioTrackFields {
        // int
        String unknownField_b = "b";

        // String
        String displayName = "c";

        // String
        String id = "d";

        // boolean
        String audioIsDefault = "e";

    }

    private static final boolean SPOOF_STREAMING_DATA = BaseSettings.SPOOF_STREAMING_DATA.get();

    /**
     * Do not use {@link WeakReference}.
     * This class can be null, as hooking and invoking are performed in different methods.
     */
    @Nullable
    private static StreamingDataMessage streamingDataMessage;

    /**
     * Injection point.
     */
    public static void initialize(StreamingDataMessage message) {
        if (SPOOF_STREAMING_DATA && message != null) {
            streamingDataMessage = message;
        }
    }

    /**
     * Parse the Proto Buffer and convert it to StreamingData (GeneratedMessage).
     * @param responseProto Proto Buffer.
     * @return              StreamingData (GeneratedMessage) parsed by ProtoParser.
     */
    @Nullable
    public static StreamingData parseFrom(ByteBuffer responseProto) {
        try {
            if (streamingDataMessage == null) {
                Logger.printDebug(() -> "Cannot parseFrom because streaming data is null");
            } else {
                return streamingDataMessage.parseFrom(responseProto);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "parseFrom failure", ex);
        }
        return null;
    }

    /**
     * Get adaptiveFormats from parsed streamingData.
     * <p>
     * @param streamingData StreamingData (GeneratedMessage) parsed by ProtoParser.
     * @return              AdaptiveFormats (ProtoList).
     */
    public static List<?> getAdaptiveFormats(StreamingData streamingData) {
        try {
            if (streamingData != null) {
                Field field = streamingData.getClass().getField(StreamingDataFields.adaptiveFormats);
                field.setAccessible(true);
                if (field.get(streamingData) instanceof List<?> adaptiveFormats) {
                    return adaptiveFormats;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getAdaptiveFormats failed", ex);
        }
        return null;
    }

    /**
     * Add the desired formats to the ArrayList in AdaptiveFormats.
     * @param streamingData StreamingData (GeneratedMessage) parsed by ProtoParser.
     * @param arrayList     An ArrayList where formats are added, this is what is actually used for playback.
     *                      Since formats that are not in this ArrayList will not be used for playback, you can filter by not adding unwanted formats.
     *                      See {@link #removeAV1Codecs(ArrayList)},
     *                      and {@link #removeNonOriginalAudioTracks(ArrayList)} for examples.
     * @param isVideo       This method only distinguishes between video and audio formats.
     */
    public static void setAdaptiveFormats(StreamingData streamingData, ArrayList<Object> arrayList, boolean isVideo) {
        try {
            List<?> adaptiveFormats = getAdaptiveFormats(streamingData);
            if (adaptiveFormats != null) {
                for (Object adaptiveFormat : adaptiveFormats) {
                    // 'audio/webm; codecs="opus"', 'audio/mp4; codecs="mp4a.40.2"', ...
                    // 'video/webm; codecs="vp9"', 'video/mp4; codecs="av01.0.00M.08.0.110.05.01.06.0"', ...
                    String mimeType = getMimeType(adaptiveFormat);

                    // mimeType starts with 'video', which means it is a video format.
                    boolean isVideoType = StringUtils.startsWith(mimeType, "video");

                    // streamingData is AudioFormat, and mimeType also starts with 'audio'.
                    boolean isAudioFormat = !isVideo && !isVideoType;
                    // streamingData is VideoFormat, and mimeType also starts with 'video'.
                    boolean isVideoFormat = isVideo && isVideoType;

                    if (isAudioFormat || isVideoFormat) {
                        // Add formats.
                        arrayList.add(adaptiveFormat);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setAdaptiveFormats failed", ex);
        }
    }

    /**
     * Remove 'AV1' video format from arrayList.
     * @param arrayList An ArrayList where formats are added.
     */
    public static void removeAV1Codecs(ArrayList<Object> arrayList) {
        try {
            for (Object adaptiveFormat : arrayList) {
                // 'audio/webm; codecs="opus"', 'audio/mp4; codecs="mp4a.40.2"', ...
                // 'video/webm; codecs="vp9"', 'video/mp4; codecs="av01.0.00M.08.0.110.05.01.06.0"', ...
                String mimeType = getMimeType(adaptiveFormat);

                // mimeType starts with 'video', which means it is a video format.
                boolean isVideoType = StringUtils.startsWith(mimeType, "video");

                if (isVideoType) {
                    if (mimeType.contains("av01")) {
                        // Remove formats.
                        arrayList.remove(adaptiveFormat);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "removeAV1Codecs failed", ex);
        }
    }

    /**
     * Remove non-original audioTracks from the arrayList.
     * @param arrayList An ArrayList where formats are added.
     */
    public static void removeNonOriginalAudioTracks(ArrayList<Object> arrayList) {
        try {
            for (Object adaptiveFormat : arrayList) {
                // 'audio/webm; codecs="opus"', 'audio/mp4; codecs="mp4a.40.2"', ...
                // 'video/webm; codecs="vp9"', 'video/mp4; codecs="av01.0.00M.08.0.110.05.01.06.0"', ...
                String mimeType = getMimeType(adaptiveFormat);

                // mimeType starts with 'audio', which means it is a audio format.
                boolean isAudioType = StringUtils.startsWith(mimeType, "audio");

                if (isAudioType) {
                    Field audioTrackField = adaptiveFormat.getClass().getField(FormatFields.audioTrack);
                    audioTrackField.setAccessible(true);
                    Object audioTrack = audioTrackField.get(adaptiveFormat);
                    if (audioTrack != null) { // AudioTrack field exists.
                        Field audioIsDefaultField = adaptiveFormat.getClass().getField(AudioTrackFields.audioIsDefault);
                        audioIsDefaultField.setAccessible(true);
                        if (audioIsDefaultField.get(audioTrack) instanceof Boolean audioIsDefault) {
                            if (!audioIsDefault) { // This is not the original audio track.
                                // Remove formats.
                                arrayList.remove(adaptiveFormat);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "removeNonOriginalAudioTracks failed", ex);
        }
    }

    /**
     * Set the deobfuscated streaming url in the 'url' field of adaptiveFormat.
     * <p>
     * @param adaptiveFormat AdaptiveFormat (GeneratedMessage).
     * @param url            Deobfuscated streaming url.
     */
    public static void setUrl(Object adaptiveFormat, String url) {
        if (adaptiveFormat != null) {
            try {
                Field field = adaptiveFormat.getClass().getField(FormatFields.url);
                field.setAccessible(true);
                field.set(adaptiveFormat, url);
            } catch (Exception ex) {
                Logger.printException(() -> "setUrl failed", ex);
            }
        }
    }

    private static String getMimeType(Object adaptiveFormat) {
        if (adaptiveFormat != null) {
            try {
                Field field = adaptiveFormat.getClass().getField(FormatFields.mimeType);
                field.setAccessible(true);
                if (field.get(adaptiveFormat) instanceof String mimeType) {
                    return mimeType;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "setUrl failed", ex);
            }
        }
        return null;
    }
}