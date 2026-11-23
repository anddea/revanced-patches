/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4414
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.components;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

import app.morphe.extension.music.patches.downloads.DownloadedMark;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

/**
 * Finds the litho song rows of the downloaded songs, such as of the albums and of the mixed
 * search results, so the Downloads patch can mark their details text. Nothing is hidden here.
 */
@SuppressWarnings("unused")
public final class DownloadedMarkFilter extends Filter {

    /** In the path of a song row and of every component inside it. */
    private static final String SONG_ROW_PATH = "music_list_item.";
    /** In the path of the text under the title of a song row, with the artist and the length. */
    private static final String SONG_DETAILS_PATH = "badged_text.";
    /**
     * Separators after {@link #SONG_ROW_PATH} in the path of the row itself, such as
     * "music_list_item.eml-fe|e1bdbb31ea4fcde4|CellType|". The components inside the row have more.
     */
    private static final int ROW_PATH_SEPARATORS = 3;

    /**
     * The downloaded song of the row being built on this thread, or null if the row is not of a
     * downloaded song. The components of a row are built one after the other on the same thread,
     * the row itself first, and each text along with its own component.
     */
    private static final ThreadLocal<String> downloadedRow = new ThreadLocal<>();

    public DownloadedMarkFilter() {
        addPathCallbacks(new StringFilterGroup(Settings.DOWNLOADS_MARK, SONG_ROW_PATH));
    }

    /**
     * @param path Path of a litho text.
     * @return If the text is the details text of the song row being built on this thread,
     *         and the song is downloaded.
     */
    public static boolean isDownloadedSongDetails(CharSequence path) {
        return downloadedRow.get() != null
                && path.toString().contains(SONG_DETAILS_PATH)
                && path.toString().contains(SONG_ROW_PATH);
    }

    /** Uses per-component buffers where available, including the local legacy bridge. */
    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return isFiltered(null, identifier, "", path, buffer, matchedGroup, contentType, contentIndex);
    }

    @Override
    public boolean useModernFilterDataInLegacyBridge() {
        return true;
    }

    @Override
    public boolean isFiltered(Object contextInterface, String identifier, String accessibility,
                              String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType,
                              int contentIndex) {
        if (DownloadedMark.isEnabled() && isRow(path, contentIndex)) {
            String videoId = findDownloadedVideoId(buffer);
            downloadedRow.set(videoId);
            if (videoId != null) Logger.printDebug(() -> "Downloaded song row: " + videoId);
        }
        return false;
    }

    private static boolean isRow(CharSequence path, int rowPathIndex) {
        int separators = 0;
        for (int i = rowPathIndex, length = path.length(); i < length; i++) {
            if (path.charAt(i) == '|') separators++;
        }
        return separators == ROW_PATH_SEPARATORS;
    }

    /**
     * The row holds its song in the video ID of its play endpoint and of the endpoints of its
     * menu. The byte before a string field is its length.
     *
     * @return The first video ID of the row that is downloaded, or null if there is none.
     */
    @Nullable
    private static String findDownloadedVideoId(byte[] buffer) {
        final int length = VIDEO_ID_LENGTH;
        for (int i = 1, last = buffer.length - length; i <= last; i++) {
            if (buffer[i - 1] != length || !isVideoId(buffer, i)) continue;

            String videoId = new String(buffer, i, length, StandardCharsets.US_ASCII);
            if (DownloadedMark.isDownloaded(videoId)) return videoId;
        }
        return null;
    }

    private static final int VIDEO_ID_LENGTH = 11;

    static boolean isVideoId(byte[] buffer, int start) {
        for (int i = start, end = start + VIDEO_ID_LENGTH; i < end; i++) {
            final byte b = buffer[i];
            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z')
                    || (b >= '0' && b <= '9') || b == '_' || b == '-')) {
                return false;
            }
        }
        return true;
    }
}
