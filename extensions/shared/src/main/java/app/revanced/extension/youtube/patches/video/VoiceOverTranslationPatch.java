package app.revanced.extension.youtube.patches.video;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.YandexVotUtils;

@SuppressWarnings("unused")
public class VoiceOverTranslationPatch {
    private static final BooleanSetting ENABLE_VOICE_OVER_TRANSLATION =
            Settings.ENABLE_VOICE_OVER_TRANSLATION;
    private static final StringSetting VOICE_OVER_TRANSLATION_LANGUAGE =
            Settings.VOICE_OVER_TRANSLATION_LANGUAGE;

    private static final String VOT_AUDIO_TRACK_ID_PREFIX = "vot_";
    private static final int VOT_AUDIO_ITAG = 140; // Standard audio-only itag

    /**
     * Injection point.
     * Adds translated audio track to StreamingData if voice-over translation is enabled.
     *
     * @param streamingData The StreamingData to modify.
     * @return The modified StreamingData with translated audio track added.
     */
    @Nullable
    public static StreamingDataOuterClass.StreamingData addTranslatedAudioTrack(
            @Nullable StreamingDataOuterClass.StreamingData streamingData) {
        try {
            if (!ENABLE_VOICE_OVER_TRANSLATION.get()) {
                return streamingData;
            }

            if (streamingData == null) {
                return streamingData;
            }

            String videoId = VideoInformation.getVideoId();
            if (videoId == null || videoId.isEmpty()) {
                return streamingData;
            }

            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            long videoLength = VideoInformation.getVideoLength();
            double durationSeconds = videoLength > 0 ? videoLength / 1000.0 : 0;

            if (durationSeconds <= 0 || durationSeconds > 4 * 3600) {
                // Skip videos longer than 4 hours (Yandex API limitation)
                Logger.printDebug(() -> "VOT: Skipping video - invalid duration or too long");
                return streamingData;
            }

            String targetLang = VOICE_OVER_TRANSLATION_LANGUAGE.get();
            if ("app".equals(targetLang)) {
                // Use app default language (would need to get from system)
                targetLang = "en"; // Default to English
            }

            // Get translated audio URL asynchronously
            CompletableFuture<String> audioUrlFuture = CompletableFuture.supplyAsync(() ->
                    YandexVotUtils.getTranslatedAudioUrl(videoUrl, durationSeconds, targetLang)
            );

            // Try to get audio URL (with timeout)
            String audioUrl = null;
            try {
                audioUrl = audioUrlFuture.get(); // Wait for result
            } catch (Exception e) {
                Logger.printException(() -> "VOT: Failed to get audio URL", e);
            }

            if (audioUrl == null || audioUrl.isEmpty()) {
                Logger.printDebug(() -> "VOT: No translated audio URL available");
                return streamingData;
            }

            // Get adaptive formats
            List<?> adaptiveFormats = StreamingDataOuterClassUtils.getAdaptiveFormats(streamingData);
            if (adaptiveFormats == null) {
                return streamingData;
            }

            // Create a new audio format with translated audio
            Object translatedAudioFormat = createTranslatedAudioFormat(streamingData, audioUrl, targetLang, durationSeconds);
            if (translatedAudioFormat == null) {
                return streamingData;
            }

            // Add translated audio format to adaptive formats
            List<Object> newAdaptiveFormats = new ArrayList<>(adaptiveFormats);
            newAdaptiveFormats.add(translatedAudioFormat);

            // Update StreamingData with new adaptive formats
            try {
                Field field = streamingData.getClass().getField("f"); // adaptiveFormats field
                field.setAccessible(true);
                field.set(streamingData, newAdaptiveFormats);
                Logger.printInfo(() -> "VOT: Added translated audio track to StreamingData");
            } catch (Exception e) {
                Logger.printException(() -> "VOT: Failed to update StreamingData", e);
            }

            return streamingData;
        } catch (Exception e) {
            Logger.printException(() -> "VOT: Error in addTranslatedAudioTrack", e);
            return streamingData;
        }
    }

    /**
     * Creates a new audio format object with translated audio URL.
     *
     * @param streamingData  The current StreamingData to find template format.
     * @param audioUrl       The URL of the translated audio.
     * @param language       The target language code.
     * @param durationSeconds The video duration in seconds.
     * @return A new audio format object, or null if creation fails.
     */
    @Nullable
    private static Object createTranslatedAudioFormat(
            @NonNull StreamingDataOuterClass.StreamingData streamingData,
            String audioUrl, String language, double durationSeconds) {
        try {
            Logger.printDebug(() -> "VOT: Creating translated audio format for language: " + language);
            
            // Find an existing audio format to use as template
            Object templateFormat = null;
            List<?> adaptiveFormats = StreamingDataOuterClassUtils.getAdaptiveFormats(streamingData);
            if (adaptiveFormats != null) {
                for (Object format : adaptiveFormats) {
                    try {
                        Field mimeTypeField = format.getClass().getField("g"); // mimeType field
                        mimeTypeField.setAccessible(true);
                        Object mimeTypeObj = mimeTypeField.get(format);
                        if (mimeTypeObj instanceof String mimeType && mimeType.startsWith("audio")) {
                            templateFormat = format;
                            break;
                        }
                    } catch (Exception e) {
                        // Continue searching
                    }
                }
            }
            
            if (templateFormat == null) {
                Logger.printDebug(() -> "VOT: No audio format template found");
                return null;
            }
            
            if (templateFormat == null) {
                Logger.printDebug(() -> "VOT: No audio format template found");
                return null;
            }
            
            // Clone the template format using protobuf serialization
            try {
                // Get the byte array representation
                java.lang.reflect.Method toByteArrayMethod = templateFormat.getClass().getMethod("toByteArray");
                byte[] formatBytes = (byte[]) toByteArrayMethod.invoke(templateFormat);
                
                // Parse it back to create a mutable copy
                java.lang.reflect.Method parseFromMethod = templateFormat.getClass().getMethod("parseFrom", byte[].class);
                Object clonedFormat = parseFromMethod.invoke(null, formatBytes);
                
                // Modify the cloned format
                // Set the URL
                StreamingDataOuterClassUtils.setUrl(clonedFormat, audioUrl);
                
                // Set audio track information
                try {
                    Field audioTrackField = clonedFormat.getClass().getField("x"); // audioTrack field
                    audioTrackField.setAccessible(true);
                    Object audioTrack = audioTrackField.get(clonedFormat);
                    
                    if (audioTrack != null) {
                        // Set display name
                        Field displayNameField = audioTrack.getClass().getField("c"); // displayName field
                        displayNameField.setAccessible(true);
                        String displayName = getLanguageDisplayName(language);
                        displayNameField.set(audioTrack, displayName);
                        
                        // Set track ID
                        Field idField = audioTrack.getClass().getField("d"); // id field
                        idField.setAccessible(true);
                        idField.set(audioTrack, VOT_AUDIO_TRACK_ID_PREFIX + language);
                        
                        // Set as non-default
                        Field isDefaultField = audioTrack.getClass().getField("e"); // audioIsDefault field
                        isDefaultField.setAccessible(true);
                        isDefaultField.set(audioTrack, false);
                    }
                } catch (Exception e) {
                    Logger.printDebug(() -> "VOT: Could not set audio track fields: " + e.getMessage());
                }
                
                Logger.printInfo(() -> "VOT: Created translated audio format successfully");
                return clonedFormat;
                
            } catch (Exception e) {
                Logger.printException(() -> "VOT: Failed to clone format", e);
                return null;
            }
            
        } catch (Exception e) {
            Logger.printException(() -> "VOT: Failed to create translated audio format", e);
            return null;
        }
    }
    
    /**
     * Gets display name for language code.
     */
    private static String getLanguageDisplayName(String langCode) {
        switch (langCode) {
            case "en": return "English (Translated)";
            case "ru": return "Русский (Переведено)";
            case "kk": return "Қазақша (Аударылған)";
            default: return "Translated (" + langCode + ")";
        }
    }
}
