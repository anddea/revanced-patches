/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

// native Music playback needs real MediaStore audio ids, not app-private file paths.
final class NativeAudioStore {
    record Destination(@Nullable Uri uri, @Nullable File file, String mimeType) {
        // scoped-storage writes must use the
        // provider's descriptors; pending raw paths and /proc aliases are not writable.
        private ParcelFileDescriptor openDescriptor() throws IOException {
            if (uri == null) return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            ParcelFileDescriptor descriptor = Utils.getContext().getContentResolver().openFileDescriptor(uri, "rw");
            if (descriptor == null) throw new IOException("Could not open native audio");
            return descriptor;
        }

        FileOutputStream openOutput() throws IOException {
            return new ParcelFileDescriptor.AutoCloseOutputStream(openDescriptor());
        }

        long size() throws IOException {
            if (uri == null) return file.length();
            try (ParcelFileDescriptor descriptor = openDescriptor()) {
                return descriptor.getStatSize();
            }
        }

        void setLength(long length) throws IOException {
            try (ParcelFileDescriptor descriptor = openDescriptor()) {
                Os.ftruncate(descriptor.getFileDescriptor(), length);
            } catch (ErrnoException ex) {
                throw new IOException("Could not size native audio", ex);
            }
        }
    }

    private NativeAudioStore() {}

    private static SharedPreferences preferences() {
        return Utils.getContext().getSharedPreferences("rvx_native_audio", 0);
    }

    @Nullable
    static Uri find(String videoId) {
        String saved = preferences().getString(videoId, null);
        if (saved == null) return null;
        Uri uri = Uri.parse(saved);
        try (ParcelFileDescriptor descriptor = Utils.getContext().getContentResolver().openFileDescriptor(uri, "r")) {
            if (descriptor != null && descriptor.getStatSize() > 0) return uri;
        } catch (Exception ignored) {
            // Deleting a track from Music's native Library also invalidates the download lookup.
        }
        preferences().edit().remove(videoId).apply();
        return null;
    }

    /**
     * Media ids of the tracks this patch downloaded that still exist, newest first.
     * Other audio on the device (e.g. files in Download/) is never included.
     */
    // source for the "Not in a playlist" native playlist.
    static List<Long> downloadedNewestFirst() {
        StringBuilder selection = new StringBuilder();
        for (Object value : preferences().getAll().values()) {
            if (!(value instanceof String saved)) continue;
            try {
                long id = ContentUris.parseId(Uri.parse(saved));
                selection.append(selection.length() == 0 ? "" : ",").append(id);
            } catch (RuntimeException ignored) {
                // Not a media row.
            }
        }
        List<Long> ids = new ArrayList<>();
        if (selection.length() == 0) return ids;
        try (Cursor cursor = Utils.getContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media._ID + " IN (" + selection + ")", null,
                MediaStore.Audio.Media.DATE_ADDED + " DESC, " + MediaStore.Audio.Media._ID + " DESC")) {
            while (cursor != null && cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        return ids;
    }

    private static String displayName(String videoId, String title, String artist) {
        String label = artist.isEmpty() ? title : title + " - " + artist;
        StringBuilder safe = new StringBuilder(Math.min(label.length(), 200));
        int bytes = 0;
        for (int offset = 0; offset < label.length(); ) {
            int original = label.codePointAt(offset);
            int codePoint = switch (original) {
                case '/', '\\', ':', '*', '?', '"', '<', '>', '|' -> '_';
                default -> original < 32 ? '_' : original;
            };
            int width = codePoint <= 0x7f ? 1 : codePoint <= 0x7ff ? 2 : codePoint <= 0xffff ? 3 : 4;
            if (bytes + width > 200) break;
            safe.appendCodePoint(codePoint);
            bytes += width;
            offset += Character.charCount(original);
        }
        String name = safe.toString().trim();
        return name.isEmpty() ? videoId : name.charAt(0) == '.' ? "_" + name : name;
    }

    static Destination create(String videoId, String mimeType, String title, String artist) throws IOException {
        Context context = Utils.getContext();
        // Android extracts no Matroska tags, so Opus arrives
        // as Ogg Opus and AAC as MP4, the two forms whose tags the media scanner reads.
        boolean mp4 = AudioTagger.isMp4(mimeType);
        mimeType = mp4 ? "audio/mp4" : "audio/ogg";
        String extension = mp4 ? ".m4a" : ".ogg";
        String name = displayName(videoId, title, artist);
        if (Build.VERSION.SDK_INT < 29) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "RVX");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create Music/RVX");
            for (int suffix = 0; ; suffix++) {
                File output = new File(directory, name + (suffix == 0 ? "" : " (" + suffix + ")") + extension);
                if (output.createNewFile()) return new Destination(null, output, mimeType);
            }
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, name + extension);
        values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/RVX/");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Could not create native audio entry");
        return new Destination(MediaStore.Files.getContentUri("external", ContentUris.parseId(uri)), null, mimeType);
    }

    static Uri finish(String videoId, Destination destination) throws Exception {
        Context context = Utils.getContext();
        File file = destination.file();
        if (destination.size() == 0) throw new IOException("The downloaded audio is empty");
        if (destination.uri() != null) {
            ContentValues publish = new ContentValues();
            publish.put(MediaStore.Audio.Media.IS_PENDING, 0);
            if (context.getContentResolver().update(destination.uri(), publish, null, null) != 1) {
                throw new IOException("Could not publish downloaded audio");
            }
            // Publishing removes the pending filename. Resolve the final scanner path now.
            try (Cursor cursor = context.getContentResolver().query(destination.uri(),
                    new String[]{MediaStore.Audio.Media.DATA}, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) throw new IOException("Native audio path is unavailable");
                file = new File(cursor.getString(0));
            }
        }
        CountDownLatch scanned = new CountDownLatch(1);
        Uri[] result = new Uri[1];
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                new String[]{destination.mimeType()}, (path, uri) -> {
                    result[0] = uri;
                    scanned.countDown();
                });
        if (!scanned.await(30, TimeUnit.SECONDS) || result[0] == null) {
            throw new IOException("Music could not index the downloaded audio");
        }
        // MediaStore owns extracted tags and
        // rejects post-publication TITLE/ARTIST/DURATION writes. Use readable filenames
        // and validate its actual audio metadata instead of pretending those writes worked.
        String volume = Build.VERSION.SDK_INT >= 29 ? MediaStore.getVolumeName(result[0]) : "external";
        long id = ContentUris.parseId(result[0]);
        Uri audio = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(volume), id);
        try {
            try (Cursor cursor = context.getContentResolver().query(audio,
                    new String[]{MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DURATION},
                    null, null, null)) {
                if (cursor == null || !cursor.moveToFirst() || cursor.getLong(0) <= 0 || cursor.getLong(1) <= 0) {
                    throw new IOException("Music could not index playable audio");
                }
            }
            if (!preferences().edit().putString(videoId, audio.toString()).commit()) {
                throw new IOException("Could not remember downloaded audio");
            }
            return audio;
        } catch (Exception ex) {
            // API 26-28 destinations have no URI until scanning. Roll that row back too.
            remove(MediaStore.Files.getContentUri(volume, id));
            throw ex;
        }
    }

    private static void remove(Uri uri) {
        try {
            Utils.getContext().getContentResolver().delete(uri, null, null);
        } catch (Exception ex) {
            // Cleanup must not replace the download/indexing error, including an already-removed row.
            Logger.printException(() -> "Could not remove incomplete native audio", ex);
        }
    }

    static void abort(Destination destination) {
        if (destination.uri() != null) {
            remove(destination.uri());
        } else if (destination.file().exists() && !destination.file().delete()) {
            Logger.printDebug(() -> "Could not remove incomplete audio");
        }
    }

    static long mediaId(Uri uri) {
        return ContentUris.parseId(uri);
    }

    static boolean migrateLegacy() {
        Utils.verifyOffMainThread();
        File directory = new File(Utils.getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "RVX");
        File[] files = directory.listFiles((parent, name) -> name.endsWith(".m4a") || name.endsWith(".webm"));
        if (files == null) return true;
        boolean complete = true;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().lastIndexOf('.'));
            Destination destination = null;
            try {
                if (find(id) == null) {
                    JSONObject metadata = new JSONObject();
                    File sidecar = new File(directory, id + ".json");
                    if (sidecar.isFile()) {
                        try (FileInputStream input = new FileInputStream(sidecar)) {
                            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int count;
                            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
                            metadata = new JSONObject(bytes.toString("UTF-8"));
                        }
                    }
                    String mimeType = file.getName().endsWith(".m4a") ? "audio/mp4" : "audio/webm; codecs=\"opus\"";
                    AudioTagger.Tags tags = new AudioTagger.Tags(metadata.optString("title", id),
                            metadata.optString("artist"), metadata.optString("album", null),
                            legacyArtwork(new File(directory, id + ".jpg")));
                    destination = create(id, AudioTagger.outputMimeType(mimeType), tags.title, tags.artist);
                    try (FileOutputStream output = destination.openOutput()) {
                        AudioTagger.write(file, mimeType, output, tags);
                    }
                    finish(id, destination);
                    destination = null;
                }
                // Never discard an old download until its native entry is playable and remembered.
                if (file.delete()) {
                    new File(directory, id + ".json").delete();
                    new File(directory, id + ".jpg").delete();
                }
            } catch (Exception ex) {
                complete = false;
                Logger.printException(() -> "Could not migrate a legacy Music download", ex);
            } finally {
                if (destination != null) abort(destination);
            }
        }
        return complete;
    }

    @Nullable
    private static byte[] legacyArtwork(File file) {
        if (!file.isFile() || file.length() > 1024 * 1024) return null;
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] image = new byte[(int) file.length()];
            int done = 0;
            while (done < image.length) {
                int count = input.read(image, done, image.length - done);
                if (count < 0) return null;
                done += count;
            }
            return AudioTagger.imageMimeType(image) == null ? null : image;
        } catch (IOException ex) {
            Logger.printException(() -> "Could not read legacy Music artwork", ex);
            return null;
        }
    }
}
