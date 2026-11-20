/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4211
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.downloads;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Reads completed native downloads using Morphe's OfflineVideoExporter stream lookup.
 * MediaStore only contains exported files, not songs in YouTube Music's offline cache.
 */
final class NativeDownloadStatus {
    private static final String DATABASE_PREFIX = "offline.";
    private static final String DATABASE_SUFFIX = ".db";
    /**
     * Streams saved with another encryption, such as streams of paid content, have a different key type.
     */
    private static final int ENCRYPTION_KEY_TYPE_CACHE = 0;

    private record SavedStream(int itag, long length) {
    }

    private NativeDownloadStatus() {
    }

    static boolean isDownloaded(String videoId) {
        for (File database : getDatabases()) {
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    database.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (getSavedStreams(db, videoId) != null) return true;
            } catch (Exception ex) {
                Logger.printException(() -> "Could not read native download status", ex);
            }
        }
        return false;
    }

    /**
     * Each signed in account has its own database.
     */
    private static List<File> getDatabases() {
        List<File> databases = new ArrayList<>();
        File folder = Utils.getContext().getDatabasePath(DATABASE_PREFIX).getParentFile();
        File[] files = folder == null ? null : folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith(DATABASE_PREFIX) && name.endsWith(DATABASE_SUFFIX)) {
                    databases.add(file);
                }
            }
        }
        return databases;
    }

    @Nullable
    private static List<SavedStream> getSavedStreams(SQLiteDatabase db, String videoId) {
        List<SavedStream> streams = new ArrayList<>();

        try (Cursor cursor = db.query(
                "streams",
                new String[]{"itag", "bytes_total", "bytes_transferred", "encryption_key_type"},
                "video_id = ?",
                new String[]{videoId},
                null, null, null)) {
            while (cursor.moveToNext()) {
                final int itag = cursor.getInt(0);
                final long length = cursor.getLong(1);
                if (length <= 0 || cursor.getLong(2) != length) {
                    Logger.printDebug(() -> "Stream is not fully saved: " + videoId + " itag: " + itag);
                    return null;
                }
                final int keyType = cursor.getInt(3);
                if (keyType != ENCRYPTION_KEY_TYPE_CACHE) {
                    Logger.printDebug(() -> "Unsupported encryption: " + keyType + " video: " + videoId);
                    return null;
                }
                streams.add(new SavedStream(itag, length));
            }
        }

        return streams.isEmpty() ? null : streams;
    }

}
