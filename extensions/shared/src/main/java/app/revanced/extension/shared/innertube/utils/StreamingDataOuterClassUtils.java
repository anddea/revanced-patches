package app.revanced.extension.shared.innertube.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings({"deprecation", "unused"})
public class StreamingDataOuterClassUtils {

    /**
     * Get adaptiveFormats from parsed streamingData.
     * <p>
     * @param streamingData StreamingData (GeneratedMessage) parsed by ProtoParser.
     * @return              AdaptiveFormats (ProtoList).
     */
    public static List<?> getAdaptiveFormats(StreamingDataOuterClass.StreamingData streamingData) {
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
    public static void setAdaptiveFormats(StreamingDataOuterClass.StreamingData streamingData, ArrayList<Object> arrayList, boolean isVideo) {
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
     * Get formats from parsed streamingData.
     * <p>
     * @param streamingData StreamingData (GeneratedMessage) parsed by ProtoParser.
     * @return              Formats (ProtoList).
     */
    public static List<?> getFormats(StreamingDataOuterClass.StreamingData streamingData) {
        try {
            if (streamingData != null) {
                Field field = streamingData.getClass().getField(StreamingDataFields.formats);
                field.setAccessible(true);
                if (field.get(streamingData) instanceof List<?> formats) {
                    return formats;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getFormats failed", ex);
        }
        return null;
    }

    /**
     * Some videos have the following video codecs:
     * <p>
     * 1. 1080p AVC
     * 2. 720p AVC
     * 3. 360p VP9
     * <p>
     * If the device supports VP9, 1080p AVC and 720p AVC are ignored,
     * and 360p VP9 is used as the highest video quality.
     * This is the intended behavior of YouTube,
     * which is why the video quality flyout menu is unavailable for some videos.
     * <p>
     * Although VP9 is a more advanced codec than AVC, using 1080p AVC is better than using 360p VP9.
     * <p>
     * This function removes all VP9 / AV1 codecs if the highest resolution video codec is AVC.
     */
    public static List<Object> prioritizeResolution(List<Object> adaptiveFormats) {
        try {
            int maxAVCHeight = -1;
            int maxVP9Height = -1;
            for (Object adaptiveFormat : adaptiveFormats) {
                String mimeType = getMimeType(adaptiveFormat);
                if (StringUtils.startsWith(mimeType, "video")) {
                    int height = getHeight(adaptiveFormat);
                    if (mimeType.contains("avc")) {
                        maxAVCHeight = Math.max(maxAVCHeight, height);
                    } else {
                        maxVP9Height = Math.max(maxVP9Height, height);
                    }
                    if (maxAVCHeight != -1 && maxVP9Height != -1) {
                        break;
                    }
                }
            }
            if (maxAVCHeight > maxVP9Height) {
                ArrayList<Object> arrayList = new ArrayList<>(adaptiveFormats.size());
                for (Object adaptiveFormat : adaptiveFormats) {
                    String mimeType = getMimeType(adaptiveFormat);
                    boolean isVideoType = StringUtils.startsWith(mimeType, "video");

                    if (!isVideoType || mimeType.contains("avc")) {
                        arrayList.add(adaptiveFormat);
                    }
                }
                return arrayList;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "prioritizeResolution failed", ex);
        }
        return adaptiveFormats;
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
     * Parse the original streaming data to get the AudioTracks.
     */
    @Nullable
    public static Map<String, String> getAudioTrackMap(@NonNull StreamingDataOuterClass.StreamingData streamingData) {
        try {
            List<?> adaptiveFormats = getAdaptiveFormats(streamingData);
            // Failed to parse adaptiveFormats.
            if (adaptiveFormats == null) return null;
            int size = adaptiveFormats.size();
            // AdaptiveFormats contains both video and audio codecs.
            // If there are multiple audio tracks, the size of adaptiveFormats is usually large.
            if (size < 5) return null;

            // Check if the video contains audio tracks.
            boolean hasAudioTrack = false;

            // The first half of the index contains video formats, and the remaining half contains audio formats.
            // For faster navigation, the search is performed in reverse order.
            for (int i = size - 1; i > (size / 2); i--) {
                Object adaptiveFormat = adaptiveFormats.get(i);
                Field audioTrackField = adaptiveFormat.getClass().getField(FormatFields.audioTrack);
                audioTrackField.setAccessible(true);
                Object audioTrack = audioTrackField.get(adaptiveFormat);
                // If an audio track is found, stop searching the list.
                if (audioTrack != null) {
                    hasAudioTrack = true;
                    break;
                }
            }

            // No audio track found.
            if (!hasAudioTrack) return null;

            Map<String, String> audioTrackMap = new LinkedHashMap<>(30);

            // For faster navigation, the search is performed in reverse order.
            for (int i = size - 1; i > 0; i--) {
                Object adaptiveFormat = adaptiveFormats.get(i);
                Field audioTrackField = adaptiveFormat.getClass().getField(FormatFields.audioTrack);
                audioTrackField.setAccessible(true);
                Object audioTrack = audioTrackField.get(adaptiveFormat);
                if (audioTrack != null) {
                    Field displayNameField = audioTrack.getClass().getField(AudioTrackFields.displayName);
                    displayNameField.setAccessible(true);
                    if (!(displayNameField.get(audioTrack) instanceof String displayName)) {
                        continue;
                    }
                    Field idField = audioTrack.getClass().getField(AudioTrackFields.id);
                    idField.setAccessible(true);
                    if (!(idField.get(audioTrack) instanceof String id)) {
                        continue;
                    }
                    if (audioTrackMap.get(displayName) == null) {
                        audioTrackMap.put(displayName, id);
                    } else {
                        // One adaptiveFormats contains duplicate AudioTrack Ids.
                        // (Two or more AudioTrack Ids with different audio formats)
                        // If an element already exists in the audioTrackMap, this indicates that a cycle has ended.
                        // Since only duplicate audio tracks will be found, the search can be aborted.
                        break;
                    }
                }
            }
            return audioTrackMap;
        } catch (Exception ex) {
            Logger.printException(() -> "getAudioTrackMap failed", ex);
        }
        return null;
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
                        Field audioIsDefaultField = audioTrack.getClass().getField(AudioTrackFields.audioIsDefault);
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

    public static void setServerAbrStreamingUrl(StreamingDataOuterClass.StreamingData streamingData, String url) {
        try {
            if (streamingData != null) {
                Field[] fields = streamingData.getClass().getFields();
                for (int i = fields.length - 1; i > 0; i--) {
                    Field field = fields[i];
                    if (field.getType().isAssignableFrom(String.class)) {
                        field.setAccessible(true);
                        field.set(streamingData, url);
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setServerAbrStreamingUrl failed", ex);
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

    private static int getHeight(Object adaptiveFormat) {
        if (adaptiveFormat != null) {
            try {
                Field field = adaptiveFormat.getClass().getField(FormatFields.height);
                field.setAccessible(true);
                if (field.get(adaptiveFormat) instanceof Integer height) {
                    return height;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "getHeight failed", ex);
            }
        }
        return -1;
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
                Logger.printException(() -> "getMimeType failed", ex);
            }
        }
        return null;
    }

    public static String getQualityLabel(Object adaptiveFormat) {
        if (adaptiveFormat != null) {
            try {
                Field field = adaptiveFormat.getClass().getField(FormatFields.qualityLabel);
                field.setAccessible(true);
                if (field.get(adaptiveFormat) instanceof String qualityLabel) {
                    return qualityLabel;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "getQualityLabel failed", ex);
            }
        }
        return null;
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
}
