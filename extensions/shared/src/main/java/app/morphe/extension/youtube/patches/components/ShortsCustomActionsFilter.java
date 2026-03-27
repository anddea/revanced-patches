package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.TrieSearch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class ShortsCustomActionsFilter extends Filter {
    private static final boolean IS_SPOOFING_TO_YOUTUBE_2023 =
            isSpoofingToLessThan("19.00.00");
    private static final boolean SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED =
            !IS_SPOOFING_TO_YOUTUBE_2023 && Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU.get();
    private static final boolean SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED =
            Settings.ENABLE_SHORTS_CUSTOM_ACTIONS_TOOLBAR.get();
    private static final boolean SHORTS_CUSTOM_ACTIONS_ENABLED =
            SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU_ENABLED || SHORTS_CUSTOM_ACTIONS_TOOLBAR_ENABLED;

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

    private final StringFilterGroup playerFlyoutMenu;

    private final StringFilterGroup likeDislikeButton;

    public static volatile boolean isShortsFlyoutMenuVisible;

    public ShortsCustomActionsFilter() {
        likeDislikeButton = new StringFilterGroup(
                null,
                "shorts_like_button.",
                "reel_like_button.",
                "reel_like_toggled_button.",
                "shorts_dislike_button.",
                "reel_dislike_button.",
                "reel_dislike_toggled_button."
        );
        playerFlyoutMenu = new StringFilterGroup(
                null,
                "overflow_menu_item."
        );

        addIdentifierCallbacks(playerFlyoutMenu);
        addPathCallbacks(likeDislikeButton);

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

    private static void setShortsVideoId(@NonNull String videoId, boolean isLive) {
        if (shortsVideoId.equals(videoId)) {
            return;
        }
        final String prefix = isLive ? "New Short livestream video id: " : "New Short video id: ";
        Logger.printDebug(() -> prefix + videoId);
        shortsVideoId = videoId;
    }

    public static String getShortsVideoId() {
        return shortsVideoId;
    }

    /**
     * Injection point.
     */
    public static void newShortsVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                             @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                             final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (!SHORTS_CUSTOM_ACTIONS_ENABLED) {
            return;
        }
        if (!newlyLoadedLiveStreamValue) {
            return;
        }
        setShortsVideoId(newlyLoadedVideoId, true);
    }

    /**
     * Injection point.
     */
    public static void newPlayerResponseVideoId(String videoId, boolean isShortAndOpeningOrPlaying) {
        try {
            if (!SHORTS_CUSTOM_ACTIONS_ENABLED) {
                return;
            }
            if (!isShortAndOpeningOrPlaying) {
                return;
            }
            synchronized (lastVideoIds) {
                if (!lastVideoIds.containsKey(videoId)) {
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
        if (!SHORTS_CUSTOM_ACTIONS_ENABLED) {
            return false;
        }
        if (matchedGroup == playerFlyoutMenu) {
            isShortsFlyoutMenuVisible = true;
            findVideoId(buffer);
        } else if (matchedGroup == likeDislikeButton && videoIdFilterGroup.check(buffer).isFiltered()) {
            findVideoId(buffer);
        }

        return false;
    }

    private void findVideoId(byte[] buffer) {
        synchronized (lastVideoIds) {
            for (Map.Entry<String, ByteArrayFilterGroup> entry : lastVideoIds.entrySet()) {
                final String videoId = entry.getKey();
                final ByteArrayFilterGroup videoIdFilter = entry.getValue();
                if (byteArrayContainsString(buffer, videoId, videoIdFilter)) {
                    setShortsVideoId(videoId, false);
                    return;
                }
            }
        }
    }
}
