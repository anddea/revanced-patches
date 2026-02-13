package app.morphe.extension.youtube.patches.components;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.FilterGroup;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.TrieSearch;
import app.morphe.extension.youtube.patches.utils.ReturnYouTubeDislikePatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

/**
 * @noinspection ALL
 * <p>
 * Searches for video id's in the proto buffer of Shorts dislike.
 * <p>
 * Because multiple litho dislike spans are created in the background
 * (and also anytime litho refreshes the components, which is somewhat arbitrary),
 * that makes the value of {@link VideoInformation#getVideoId()} and {@link VideoInformation#getPlayerResponseVideoId()}
 * unreliable to determine which video id a Shorts litho span belongs to.
 * <p>
 * But the correct video id does appear in the protobuffer just before a Shorts litho span is created.
 * <p>
 * Once a way to asynchronously update litho text is found, this strategy will no longer be needed.
 */
public final class ReturnYouTubeDislikeFilterPatch extends Filter {

    /**
     * Last unique video id's loaded.
     * Key is a String represeting the video id.
     * Value is a ByteArrayFilterGroup used for performing KMP pattern searching.
     */
    @GuardedBy("itself")
    private static final Map<String, ByteArrayFilterGroup> lastVideoIds = new LinkedHashMap<>() {
        /**
         * Number of video id's to keep track of for searching thru the buffer.
         * A minimum value of 3 should be sufficient, but check a few more just in case.
         */
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 5;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };
    private final ByteArrayFilterGroupList videoIdFilterGroup = new ByteArrayFilterGroupList();

    public ReturnYouTubeDislikeFilterPatch() {
        // When a new Short is opened, the like buttons always seem to load before the dislike.
        // But if swiping back to a previous video and liking/disliking, then only that single button reloads.
        // So must check for both buttons.
        addPathCallbacks(
                new StringFilterGroup(
                        null,
                        "shorts_like_button.",
                        "reel_like_button.",
                        "reel_like_toggled_button.",
                        "shorts_dislike_button.",
                        "reel_dislike_button.",
                        "reel_dislike_toggled_button."
                )
        );

        // After the button identifiers is binary data and then the video id for that specific short.
        videoIdFilterGroup.addAll(
                new ByteArrayFilterGroup(
                        null,
                        "id.reel_like_button",
                        "id.reel_dislike_button",
                        "ic_right_like",
                        "ic_right_dislike"
                )
        );
    }

    private volatile static String shortsVideoId = "";

    public static String getShortsVideoId() {
        return shortsVideoId;
    }

    /**
     * Injection point.
     */
    public static void newShortsVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                             @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                             final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (!Settings.RYD_SHORTS.get()) {
            return;
        }
        if (shortsVideoId.equals(newlyLoadedVideoId)) {
            return;
        }
        Logger.printDebug(() -> "newShortsVideoStarted: " + newlyLoadedVideoId);
        shortsVideoId = newlyLoadedVideoId;
    }

    /**
     * Injection point.
     */
    public static void newPlayerResponseVideoId(String videoId, boolean isShortAndOpeningOrPlaying) {
        try {
            if (!isShortAndOpeningOrPlaying || !Settings.RYD_ENABLED.get() || !Settings.RYD_SHORTS.get()) {
                return;
            }
            synchronized (lastVideoIds) {
                if (!lastVideoIds.containsKey(videoId)) {
                    Logger.printDebug(() -> "New Shorts video id: " + videoId);
                    // Put a placeholder first
                    lastVideoIds.put(videoId, null);
                    lastVideoIds.put(videoId, new ByteArrayFilterGroup(null, videoId));
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "newPlayerResponseVideoId failure", ex);
        }
    }

    /**
     * This could use {@link TrieSearch}, but since the patterns are constantly changing
     * the overhead of updating the Trie might negate the search performance gain.
     */
    private static boolean byteArrayContainsString(@NonNull byte[] array, @NonNull String text,
                                                   @Nullable ByteArrayFilterGroup videoIdFilter) {
        // If a video filter is available, check it first.
        if (videoIdFilter != null) {
            return videoIdFilter.check(array).isFiltered();
        }
        for (int i = 0, lastArrayStartIndex = array.length - text.length(); i <= lastArrayStartIndex; i++) {
            boolean found = true;
            for (int j = 0, textLength = text.length(); j < textLength; j++) {
                if (array[i + j] != (byte) text.charAt(j)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!Settings.RYD_ENABLED.get() || !Settings.RYD_SHORTS.get()) {
            return false;
        }

        FilterGroup.FilterGroupResult result = videoIdFilterGroup.check(buffer);
        if (result.isFiltered()) {
            String matchedVideoId = findVideoId(buffer);
            // Matched video will be null if in incognito mode.
            // Must pass a null id to correctly clear out the current video data.
            // Otherwise if a Short is opened in non-incognito, then incognito is enabled and another Short is opened,
            // the new incognito Short will show the old prior data.
            ReturnYouTubeDislikePatch.setLastLithoShortsVideoId(matchedVideoId);
        }

        return false;
    }

    @Nullable
    private String findVideoId(byte[] buffer) {
        synchronized (lastVideoIds) {
            for (Map.Entry<String, ByteArrayFilterGroup> entry : lastVideoIds.entrySet()) {
                final String videoId = entry.getKey();
                final ByteArrayFilterGroup videoIdFilter = entry.getValue();
                if (byteArrayContainsString(buffer, videoId, videoIdFilter)) {
                    return videoId;
                }
            }

            return null;
        }
    }
}