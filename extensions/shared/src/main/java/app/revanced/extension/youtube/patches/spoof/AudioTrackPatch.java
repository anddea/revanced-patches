package app.revanced.extension.youtube.patches.spoof;

import static app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest.getLastSpoofedClientHasSingleAudioTrack;

import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;

import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.AppLanguage;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.VideoUtils;
import kotlin.Pair;

@SuppressWarnings("unused")
public class AudioTrackPatch extends SpoofStreamingDataPatch {
    private static final boolean SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON =
            SPOOF_STREAMING_DATA && Settings.SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON.get();

    @NonNull
    private static String audioTrackId = "";
    @NonNull
    private static String videoId = "";

    @GuardedBy("itself")
    private static final Map<String, StreamingData> streamingDataMaps = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 5;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    @GuardedBy("itself")
    private static final Map<String, Map<String, String>> audioTrackMaps = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 5;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    /**
     * Injection point.
     * Called after {@link StreamingDataRequest}.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedVideoId, StreamingData streamingData) {
        if (SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON &&
                isValidVideoId(newlyLoadedVideoId) &&
                streamingData != null) {
            synchronized (streamingDataMaps) {
                streamingDataMaps.putIfAbsent(newlyLoadedVideoId, streamingData);
            }
        }
    }

    /**
     * Injection point.
     * Called after {@link StreamingDataRequest}.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        try {
            if (!SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON) {
                return;
            }
            if (Objects.equals(videoId, newlyLoadedVideoId)) {
                return;
            }
            // Only 'No auth' can change the audio track language when fetching.
            // Check if the last spoofed client is 'No auth'.
            if (!getLastSpoofedClientHasSingleAudioTrack()) {
                Logger.printDebug(() -> "Video is not No Auth");
                return;
            }

            videoId = newlyLoadedVideoId;
            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);

            // Instead of using the YouTube API to fetch a list of available audio tracks,
            // You can parse the streamingData class.
            //
            // 1. The processing time is reduced:
            //   - Using the YouTube API: 500ms - 1000ms.
            //   - Parsing the original streamingData class: 1ms - 10ms.
            // 2. No additional network resources are consumed.
            // 3. Works even when the user is not logged in.
            synchronized (streamingDataMaps) {
                StreamingData streamingData = streamingDataMaps.get(newlyLoadedVideoId);
                if (streamingData != null) {
                    var audioTracks = StreamingDataOuterClassUtils.getAudioTrackMap(streamingData);
                    if (MapUtils.isNotEmpty(audioTracks) && audioTracks.size() > 1) {
                        synchronized (audioTrackMaps) {
                            audioTrackMaps.putIfAbsent(newlyLoadedVideoId, audioTracks);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "newVideoStarted failure", ex);
        }
    }

    /**
     * Injection point.
     * In general, the value of audioTrackId is not constant because all audioTrackIds are called.
     * Since the patch has a prerequisite of using 'No auth', the value of the current audioTrackId is always used.
     */
    public static void setAudioTrackId(String newlyLoadedAudioTrackId) {
        if (SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON &&
                getLastSpoofedClientHasSingleAudioTrack() &&
                newlyLoadedAudioTrackId != null &&
                newlyLoadedAudioTrackId.contains(".") &&
                !audioTrackId.equals(newlyLoadedAudioTrackId)
        ) {
            audioTrackId = newlyLoadedAudioTrackId;
            Logger.printDebug(() -> "new AudioTrackId: " + newlyLoadedAudioTrackId);
        }
    }

    public static Map<String, String> getAudioTrackMap() {
        String videoId = VideoInformation.getVideoId();
        synchronized (audioTrackMaps) {
            return audioTrackMaps.get(videoId);
        }
    }

    public static boolean audioTrackMapIsNotNull() {
        return getAudioTrackMap() != null;
    }

    public static void showAudioTrackDialog(@NonNull Context context) {
        Map<String, String> audioTrackMap = getAudioTrackMap();

        // This video does not support audio tracks.
        if (audioTrackMap == null) {
            return;
        }

        Pair<String[], String[]> audioTracks = sortByDisplayNames(audioTrackMap);
        String[] displayNames = audioTracks.getFirst();
        String[] ids = audioTracks.getSecond();

        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(context);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(displayNames.length);
        EnumSetting<AppLanguage> appLanguage = BaseSettings.SPOOF_STREAMING_DATA_NO_AUTH_LANGUAGE;

        int checkIconId = ResourceUtils.getDrawableIdentifier("quantum_ic_check_white_24");

        for (int i = 0; i < displayNames.length; i++) {
            String id = ids[i];
            String language = id.substring(0, id.indexOf("."));
            String displayName = displayNames[i];

            Runnable action = () -> {
                audioTrackId = id;

                // Save the language code to be changed in the [overrideLanguage] field of the [StreamingDataRequest] class.
                StreamingDataRequest.overrideLanguage(language);

                // Change the audio track language by reloading the same video.
                // Due to structural limitations of the YouTube app, the url of a video that is already playing will not be opened.
                // As a workaround, the video should be forcefully dismissed.
                VideoUtils.reloadVideo(videoId);
            };

            LinearLayout itemLayout =
                    ExtendedUtils.createItemLayout(context, displayName, audioTrackId.equals(id) ? checkIconId : 0);
            actionsMap.putIfAbsent(itemLayout, action);
            mainLayout.addView(itemLayout);
        }

        ExtendedUtils.showBottomSheetDialog(context, mainLayout, actionsMap);
    }

    /**
     * Sorts audio tracks by displayName in lexicographical order.
     */
    private static Pair<String[], String[]> sortByDisplayNames(@NonNull Map<String, String> map) {
        final int firstEntriesToPreserve = 0;
        final int mapSize = map.size();

        List<Pair<String, String>> firstPairs = new ArrayList<>(firstEntriesToPreserve);
        List<Pair<String, String>> pairsToSort = new ArrayList<>(mapSize);

        int i = 0;
        for (Map.Entry<String, String> entrySet : map.entrySet()) {
            String displayName = entrySet.getKey();
            String id = entrySet.getValue();

            Pair<String, String> pair = new Pair<>(displayName, id);
            if (i < firstEntriesToPreserve) {
                firstPairs.add(pair);
            } else {
                pairsToSort.add(pair);
            }
            i++;
        }

        pairsToSort.sort((pair1, pair2)
                -> pair1.getFirst().compareToIgnoreCase(pair2.getFirst()));

        String[] sortedDisplayNames = new String[mapSize];
        String[] sortedIds = new String[mapSize];

        i = 0;
        for (Pair<String, String> pair : firstPairs) {
            sortedDisplayNames[i] = pair.getFirst();
            sortedIds[i] = pair.getSecond();
            i++;
        }

        for (Pair<String, String> pair : pairsToSort) {
            sortedDisplayNames[i] = pair.getFirst();
            sortedIds[i] = pair.getSecond();
            i++;
        }

        return new Pair<>(sortedDisplayNames, sortedIds);
    }
}
