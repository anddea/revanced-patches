/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.morphe.extension.music.patches.misc.requests.MusicBrowseRequest;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

// download the complete playlist, then publish a native offline queue in its original order.
public final class PlaylistDownloads {
    private static final Set<String> active = ConcurrentHashMap.newKeySet();

    private PlaylistDownloads() {}

    public static void enqueue(String playlistId) {
        if (!NativeDownloads.ensureStoragePermission()) return;
        if (!active.add(playlistId)) {
            Utils.showToastShort(str("morphe_music_downloads_already_running"));
            return;
        }
        Utils.showToastShort(str("morphe_music_playlist_download_started"));
        Utils.submitOnBackgroundThread(() -> {
            String title = playlistId;
            try {
                NativeDownloads.migrateLegacy();
                List<String> tracks = new ArrayList<>();
                Set<String> continuations = new HashSet<>();
                String continuation = null;
                do {
                    JSONObject response = MusicBrowseRequest.browse(Utils.getContext(), "VL" + playlistId,
                            continuation, null);
                    if (continuation == null) {
                        String resolvedTitle = playlistTitle(response);
                        if (resolvedTitle.isEmpty()) resolvedTitle = albumTitle(playlistId, response);
                        if (resolvedTitle.isEmpty()) throw new IOException("Playlist title is unavailable");
                        title = resolvedTitle;
                    }
                    Page page = readPage(response);
                    tracks.addAll(page.videoIds());
                    continuation = page.continuation();
                    if (continuation != null && !continuations.add(continuation)) {
                        throw new IOException("Music repeated a playlist continuation");
                    }
                } while (continuation != null);
                if (tracks.isEmpty()) throw new IOException("Playlist contains no downloadable tracks");
                List<Long> mediaIds = new ArrayList<>(tracks.size());
                for (int index = 0; index < tracks.size(); index++) {
                    // already-saved tracks finish in milliseconds; unpaced
                    // updates exceed Android's per-app rate limit, which then drops the final notification.
                    if (LocalDownloadManager.canPostProgress()) {
                        LocalDownloadManager.showDownloadNotification("playlist:" + playlistId, title,
                                index * 100 / tracks.size(), true,
                                str("morphe_music_playlist_download_progress", index + 1, tracks.size()));
                    }
                    Uri track = LocalDownloadManager.download(tracks.get(index));
                    mediaIds.add(NativeAudioStore.mediaId(track));
                }
                NativePlaylistStore.save(playlistId, title, mediaIds);
                LocalDownloadManager.awaitNotificationSlot();
                LocalDownloadManager.showDownloadNotification("playlist:" + playlistId, title, 100,
                        false, str("morphe_music_playlist_download_complete", tracks.size()));
                Utils.showToastShort(str("morphe_music_playlist_download_complete", tracks.size()));
            } catch (Exception ex) {
                Logger.printException(() -> "Playlist download failed", ex);
                // Completed songs remain playable, but never label a truncated playlist complete.
                LocalDownloadManager.awaitNotificationSlot();
                LocalDownloadManager.showDownloadNotification("playlist:" + playlistId, title, 0,
                        false, str("morphe_music_playlist_download_failed"));
                Utils.showToastLong(str("morphe_music_playlist_download_failed"));
            } finally {
                active.remove(playlistId);
                // tracks now in a playlist leave "Not in a playlist";
                // songs kept from a failed download join it.
                NativePlaylistStore.refreshUnsortedLater();
            }
            return null;
        });
    }

    record Page(List<String> videoIds, String continuation) {}

    static Page readPage(JSONObject response) throws IOException {
        List<String> ids = new ArrayList<>();
        String[] next = new String[1];
        boolean[] found = new boolean[1];
        collectShelves(response, ids, next, found, false);
        if (!found[0]) collectShelves(response, ids, next, found, true);
        if (!found[0]) throw new IOException("Music did not return a playlist shelf");
        return new Page(ids, next[0]);
    }

    private static void collectShelves(Object node, List<String> ids, String[] next, boolean[] found,
                                      boolean legacyShelf) throws IOException {
        if (node instanceof JSONObject object) {
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = object.opt(key);
                if (key.equals("musicPlaylistShelfRenderer") || key.equals("musicPlaylistShelfContinuation")
                        || (legacyShelf && !found[0]
                        && (key.equals("musicShelfRenderer") || key.equals("musicShelfContinuation")))) {
                    found[0] = true;
                    collectItems(value, ids, next);
                } else if (key.equals("appendContinuationItemsAction") || key.equals("reloadContinuationItemsCommand")) {
                    found[0] = true;
                    collectItems(value, ids, next);
                } else if (!key.equals("header") && !key.equals("menu") && !key.equals("menus")
                        && !key.equals("frameworkUpdates") && !key.equals("musicCarouselShelfRenderer")) {
                    collectShelves(value, ids, next, found, legacyShelf);
                }
            }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) collectShelves(array.opt(i), ids, next, found, legacyShelf);
        }
    }

    private static void collectItems(Object node, List<String> ids, String[] next) throws IOException {
        if (node instanceof JSONObject object) {
            for (String key : new String[]{"musicResponsiveListItemRenderer", "musicTwoColumnItemRenderer", "musicMultiRowListItemRenderer"}) {
                JSONObject row = object.optJSONObject(key);
                if (row == null) continue;
                JSONObject itemData = row.optJSONObject("playlistItemData");
                String id = itemData == null ? "" : itemData.optString("videoId");
                if (id.isEmpty()) {
                    JSONObject endpoint = row.optJSONObject("navigationEndpoint");
                    // an owned playlist lists an "Add a song" search row, not a track.
                    if (itemData == null && endpoint != null && endpoint.has("searchEndpoint")) return;
                    JSONObject watch = endpoint == null ? null : endpoint.optJSONObject("watchEndpoint");
                    if (watch != null) id = watch.optString("videoId");
                }
                if (id.isEmpty()) throw new IOException("A playlist track is unavailable");
                ids.add(id); // Deliberate duplicate positions must remain in the playlist.
                return; // Never mistake a row's menus/recommendations for additional playlist tracks.
            }
            JSONObject continuation = object.optJSONObject("nextContinuationData");
            if (continuation == null) continuation = object.optJSONObject("continuationCommand");
            if (continuation != null) {
                String token = continuation.optString("continuation", continuation.optString("token"));
                if (!token.isEmpty()) next[0] = token;
                return;
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) collectItems(object.opt(keys.next()), ids, next);
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) collectItems(array.opt(i), ids, next);
        }
    }

    /**
     * Album playlists (OLAK…) have no header in their playlist response. Every row links to
     * the album page instead; use that page's header only if all rows agree on one album and
     * the album page itself points back at this playlist.
     */
    private static String albumTitle(String playlistId, JSONObject response) throws Exception {
        Set<String> albums = new HashSet<>();
        collectAlbums(response, albums);
        if (albums.size() != 1) return "";
        JSONObject album = MusicBrowseRequest.browse(Utils.getContext(), albums.iterator().next(), null, null);
        return album.toString().contains("\"" + playlistId + "\"") ? playlistTitle(album) : "";
    }

    private static void collectAlbums(Object node, Set<String> albums) {
        if (node instanceof JSONObject object) {
            JSONObject endpoint = object.optJSONObject("browseEndpoint");
            String id = endpoint == null ? "" : endpoint.optString("browseId");
            if (id.startsWith("MPREb_")) albums.add(id);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) collectAlbums(object.opt(keys.next()), albums);
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) collectAlbums(array.opt(i), albums);
        }
    }

    private static String playlistTitle(Object node) {
        if (node instanceof JSONObject object) {
            JSONObject metadata = object.optJSONObject("musicPlaylistMetadataRenderer");
            if (metadata != null && !metadata.optString("title").isEmpty()) return metadata.optString("title");
            for (String key : new String[]{"musicDetailHeaderRenderer", "musicResponsiveHeaderRenderer",
                    "musicHeaderRenderer", "musicElementHeaderRenderer"}) {
                JSONObject header = object.optJSONObject(key);
                if (header != null) {
                    JSONObject title = header.optJSONObject("title");
                    if (title == null) continue;
                    if (title.has("simpleText")) return title.optString("simpleText");
                    JSONArray runs = title.optJSONArray("runs");
                    if (runs != null) {
                        StringBuilder text = new StringBuilder();
                        for (int i = 0; i < runs.length(); i++) text.append(runs.optJSONObject(i).optString("text"));
                        if (text.length() > 0) return text.toString();
                    }
                }
            }
            // Titles come from playlist metadata/headers, not from a song row or menu label.
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.endsWith("ItemRenderer") || key.equals("musicPlaylistShelfRenderer")) continue;
                String title = playlistTitle(object.opt(key));
                if (!title.isEmpty()) return title;
            }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                String title = playlistTitle(array.opt(i));
                if (!title.isEmpty()) return title;
            }
        }
        return "";
    }
}
