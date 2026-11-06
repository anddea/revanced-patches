/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

// use Music's own asynchronous playlist transactions, not a second player/database.
@SuppressWarnings("unused")
public final class NativePlaylistStore {
    /** Implemented by Music's stock sideloaded-playlist repository at patch time. */
    public interface Database {
        Future<?> rvxCreatePlaylist(String title);
        Future<?> rvxAddPlaylistTracks(long playlistId, List<Long> mediaIds);
        Future<?> rvxDeletePlaylist(long playlistId);
    }

    private static volatile Database database;

    // downloads that belong to no native playlist are kept in one derived playlist.
    private static final String PREFERENCES = "rvx_native_playlists";
    private static final String UNSORTED = "rvx:not_in_playlist";
    private static final String DATABASE_NAME = "music_sideloaded_playlist_database";
    private static final AtomicBoolean refreshQueued = new AtomicBoolean();

    private NativePlaylistStore() {}

    /** Called at the end of Music's own repository constructor. */
    public static void onDatabase(Object instance) {
        database = (Database) instance;
        refreshUnsortedLater();
    }

    private static Database database() throws IOException {
        Database current = database;
        if (current == null) throw new IOException("Music's playlist database is not ready");
        return current;
    }

    static synchronized void save(String sourceId, String title, List<Long> mediaIds) throws Exception {
        Utils.verifyOffMainThread();
        Database store = database();
        SharedPreferences preferences = Utils.getContext().getSharedPreferences(PREFERENCES, 0);
        long previous = preferences.getLong(sourceId, -1);
        long created = ((Number) store.rvxCreatePlaylist(title).get()).longValue();
        boolean committed = false;
        try {
            int inserted = ((Number) store.rvxAddPlaylistTracks(created, mediaIds).get()).intValue();
            if (inserted != mediaIds.size()) throw new IOException("Music did not save every playlist position");
            // Publish only after every track is locally playable. A failed refresh keeps the old playlist.
            if (!preferences.edit().putLong(sourceId, created).commit()) {
                throw new IOException("Could not remember the native playlist");
            }
            committed = true;
        } finally {
            if (!committed) store.rvxDeletePlaylist(created).get();
        }
        if (previous >= 0) {
            try {
                store.rvxDeletePlaylist(previous).get();
            } catch (Exception ex) {
                Logger.printException(() -> "Could not remove the previous downloaded playlist", ex);
            }
        }
    }

    /** Refreshes "Not in a playlist" in the background; requests made while one is queued are merged. */
    static void refreshUnsortedLater() {
        if (!Settings.IN_APP_DOWNLOADS.get() || !refreshQueued.compareAndSet(false, true)) return;
        Utils.submitOnBackgroundThread(() -> {
            refreshQueued.set(false);
            try {
                refreshUnsorted();
            } catch (Exception ex) {
                Logger.printException(() -> "Could not update the Not in a playlist playlist", ex);
            }
            return null;
        });
    }

    /**
     * Rebuilds the derived playlist: every downloaded track that is in no other native playlist, including
     * playlists the user made in Device files, newest first. It is removed when empty and left alone when
     * already current. Manual edits to it are overwritten.
     */
    private static synchronized void refreshUnsorted() throws Exception {
        Utils.verifyOffMainThread();
        SharedPreferences preferences = Utils.getContext().getSharedPreferences(PREFERENCES, 0);
        long current = preferences.getLong(UNSORTED, -1);
        Set<Long> inPlaylists = new HashSet<>();
        List<Long> existing = new ArrayList<>();
        boolean exists = false;
        File file = Utils.getContext().getDatabasePath(DATABASE_NAME);
        if (file.isFile()) {
            // Read-only, so Android leaves the journal mode alone; every write still goes through Music's repository.
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                 Cursor cursor = db.rawQuery("SELECT playlist_id, audio_media_id FROM music_sideloaded_playlist_track"
                         + " ORDER BY playlist_id, position", null)) {
                while (cursor.moveToNext()) {
                    if (cursor.getLong(0) == current) existing.add(cursor.getLong(1));
                    else inPlaylists.add(cursor.getLong(1));
                }
                exists = current >= 0 && DatabaseUtils.longForQuery(db,
                        "SELECT COUNT(*) FROM music_sideloaded_playlist WHERE id = ?",
                        new String[]{Long.toString(current)}) > 0;
            }
        }
        List<Long> wanted = new ArrayList<>();
        for (long id : NativeAudioStore.downloadedNewestFirst()) if (!inPlaylists.contains(id)) wanted.add(id);
        if (wanted.isEmpty()) {
            if (exists) database().rvxDeletePlaylist(current).get();
            if (current >= 0 && !preferences.edit().remove(UNSORTED).commit()) {
                throw new IOException("Could not forget the Not in a playlist playlist");
            }
            return;
        }
        if (exists && wanted.equals(existing)) return;
        save(UNSORTED, str("morphe_music_not_in_playlist"), wanted);
    }
}
