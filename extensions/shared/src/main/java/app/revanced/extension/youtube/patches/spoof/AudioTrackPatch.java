package app.revanced.extension.youtube.patches.spoof;

import static app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest.getLastSpoofedAudioClientIsAndroidVRNoAuth;

import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.revanced.extension.shared.innertube.utils.AuthUtils;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.AppLanguage;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.spoof.requests.AudioTrackRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.VideoUtils;
import kotlin.Pair;
import kotlin.Triple;

@SuppressWarnings("unused")
public class AudioTrackPatch {
    private static final boolean SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON =
            Settings.SPOOF_STREAMING_DATA.get() && Settings.SPOOF_STREAMING_DATA_VR_AUDIO_TRACK_BUTTON.get();

    @NonNull
    private static String audioTrackId = "";
    @NonNull
    private static String videoId = "";

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
            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Network not connected, ignoring video");
                return;
            }
            // Only 'Android VR (No auth)' can change the audio track language when fetching.
            // Check if the last spoofed client is 'Android VR (No auth)'.
            if (!getLastSpoofedAudioClientIsAndroidVRNoAuth()) {
                Logger.printDebug(() -> "Video is not Android VR No Auth");
                return;
            }
            Map<String, String> requestHeader = AuthUtils.getRequestHeader();
            if (MapUtils.isEmpty(requestHeader)) {
                Logger.printDebug(() -> "AuthUtils is not initialized");
                return;
            }

            videoId = newlyLoadedVideoId;
            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);

            // Use the YouTube API to get a list of audio tracks supported by a video.
            AudioTrackRequest.fetchRequestIfNeeded(videoId, requestHeader);
        } catch (Exception ex) {
            Logger.printException(() -> "newVideoStarted failure", ex);
        }
    }

    /**
     * Injection point.
     * In general, the value of audioTrackId is not constant because all audioTrackIds are called.
     * Since the patch has a prerequisite of using 'Android VR (No auth)', the value of the current audioTrackId is always used.
     */
    public static void setAudioTrackId(String newlyLoadedAudioTrackId) {
        if (SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON) {
            if (newlyLoadedAudioTrackId != null
                    && !Objects.equals(audioTrackId, newlyLoadedAudioTrackId)) {
                audioTrackId = newlyLoadedAudioTrackId;
                Logger.printDebug(() -> "new AudioTrackId: " + newlyLoadedAudioTrackId);
            }
        }
    }

    public static Map<String, Pair<String, Boolean>> getAudioTrackMap() {
        try {
            String videoId = VideoInformation.getVideoId();
            AudioTrackRequest request = AudioTrackRequest.getRequestForVideoId(videoId);
            if (request != null) {
                return request.getStream();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getAudioTrackMap failure", ex);
        }

        return null;
    }

    public static boolean audioTrackMapIsNotNull() {
        return getAudioTrackMap() != null;
    }

    public static void showAudioTrackDialog(@NonNull Context context) {
        Map<String, Pair<String, Boolean>> map = getAudioTrackMap();

        // This video does not support audio tracks.
        if (map == null) {
            return;
        }

        Triple<String[], String[], Boolean[]> audioTracks = sortByDisplayNames(map);
        String[] displayNames = audioTracks.getFirst();
        String[] ids = audioTracks.getSecond();
        Boolean[] audioIsDefaults = audioTracks.getThird();

        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(context);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(displayNames.length);
        EnumSetting<AppLanguage> appLanguage = BaseSettings.SPOOF_STREAMING_DATA_VR_LANGUAGE;

        int checkIconId = ResourceUtils.getDrawableIdentifier("quantum_ic_check_white_24");

        for (int i = 0; i < displayNames.length; i++) {
            String id = ids[i];
            String language = id.substring(0, id.indexOf("."));
            String displayName = displayNames[i];
            Boolean audioIsDefault = audioIsDefaults[i];

            Runnable action = () -> {
                audioTrackId = id;

                // Save the language code to be changed in the [overrideLanguage] field of the [StreamingDataRequest] class.
                StreamingDataRequest.overrideLanguage(language);

                // Change the audio track language by reloading the same video.
                // Due to structural limitations of the YouTube app, the url of a video that is already playing will not be opened.
                // As a workaround, the video should be forcefully dismissed.
                VideoUtils.reloadVideo(videoId);

                // If the video has been reloaded, initialize the [overrideLanguage] field of the [StreamingDataRequest] class.
                ExtendedUtils.runOnMainThreadDelayed(() -> StreamingDataRequest.overrideLanguage(""), 3000L);
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
    private static Triple<String[], String[], Boolean[]> sortByDisplayNames(@NonNull Map<String, Pair<String, Boolean>> map) {
        final int firstEntriesToPreserve = 0;
        final int mapSize = map.size();

        List<Triple<String, String, Boolean>> firstTriples = new ArrayList<>(firstEntriesToPreserve);
        List<Triple<String, String, Boolean>> triplesToSort = new ArrayList<>(mapSize);

        int i = 0;
        for (Map.Entry<String, Pair<String, Boolean>> entrySet : map.entrySet()) {
            Pair<String, Boolean> pair = entrySet.getValue();
            String displayName = entrySet.getKey();
            String id = pair.getFirst();
            Boolean audioIsDefault = pair.getSecond();

            Triple<String, String, Boolean> triple = new Triple<>(displayName, id, audioIsDefault);
            if (i < firstEntriesToPreserve) {
                firstTriples.add(triple);
            } else {
                triplesToSort.add(triple);
            }
            i++;
        }

        triplesToSort.sort((triple1, triple2)
                -> triple1.getFirst().compareToIgnoreCase(triple2.getFirst()));

        String[] sortedDisplayNames = new String[mapSize];
        String[] sortedIds = new String[mapSize];
        Boolean[] sortedAudioIsDefaults = new Boolean[mapSize];

        i = 0;
        for (Triple<String, String, Boolean> triple : firstTriples) {
            sortedDisplayNames[i] = triple.getFirst();
            sortedIds[i] = triple.getSecond();
            sortedAudioIsDefaults[i] = triple.getThird();
            i++;
        }

        for (Triple<String, String, Boolean> triple : triplesToSort) {
            sortedDisplayNames[i] = triple.getFirst();
            sortedIds[i] = triple.getSecond();
            sortedAudioIsDefaults[i] = triple.getThird();
            i++;
        }

        return new Triple<>(sortedDisplayNames, sortedIds, sortedAudioIsDefaults);
    }
}
