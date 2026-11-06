/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import app.morphe.extension.music.patches.flyout.FlyoutPatch.ProtocolBufferFieldInterface;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

// let native browse resolution and local-file playback own the Library Downloads flow.
@SuppressWarnings("unused")
public final class NativeDownloads {
    public interface Command extends ProtocolBufferFieldInterface {
        Object rvxWithBytes(byte[] bytes);
    }

    private static final int BROWSE_ENDPOINT = 48687626;
    private static final int OFFLINE_PLAYLIST_ENDPOINT = 0x45b26d7;
    private static volatile boolean migrated;
    private static boolean migrationScheduled;

    private NativeDownloads() {}

    static boolean ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= 29 || Utils.getContext().checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return true;
        Activity activity = Utils.getActivity();
        if (activity != null) activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 20842);
        Utils.showToastLong(str("morphe_music_downloads_storage_permission"));
        return false;
    }

    static synchronized void migrateLegacy() {
        if (!migrated) migrated = NativeAudioStore.migrateLegacy();
    }

    private static synchronized void scheduleMigration() {
        if (migrated || migrationScheduled) return;
        migrationScheduled = true;
        Utils.submitOnBackgroundThread(() -> {
            try {
                migrateLegacy();
            } finally {
                synchronized (NativeDownloads.class) {
                    migrationScheduled = false;
                }
            }
            return null;
        });
    }

    public static Object rewriteCommand(Object original, java.util.Map<?, ?> context) {
        if (!Settings.IN_APP_DOWNLOADS.get() || !(original instanceof Command command)) return original;
        try {
            // signed-in non-Premium accounts receive an upsell batch without a playlist id.
            String headerPlaylist = playlistFromHeaderUpsell(command.toByteArray(), context);
            if (headerPlaylist != null) {
                PlaylistDownloads.enqueue(headerPlaylist);
                return null;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not resolve signed-in playlist download", ex);
        }
        try {
            byte[] bytes = command.toByteArray();
            CodedInputStream input = CodedInputStream.newInstance(bytes);
            while (!input.isAtEnd()) {
                int start = input.getTotalBytesRead();
                int tag = input.readTag();
                int number = WireFormat.getTagFieldNumber(tag);
                if ((number == OFFLINE_PLAYLIST_ENDPOINT || number == 133724106)
                        && WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    byte[] endpoint = input.readByteArray();
                    String playlistId = number == OFFLINE_PLAYLIST_ENDPOINT
                            ? playlistFromDownload(endpoint) : playlistFromUpsell(endpoint);
                    if (playlistId != null && !playlistId.isEmpty()) {
                        if (playlistId.startsWith("VL")) playlistId = playlistId.substring(2);
                        PlaylistDownloads.enqueue(playlistId);
                        return null; // The patch consumes only this concrete playlist-download intent.
                    }
                    continue;
                }
                if (WireFormat.getTagFieldNumber(tag) == BROWSE_ENDPOINT
                        && WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    byte[] endpoint = input.readByteArray();
                    byte[] rewritten = rewriteBrowse(endpoint);
                    if (rewritten != null) {
                        scheduleMigration();
                        return command.rvxWithBytes(replaceField(bytes, start,
                                input.getTotalBytesRead(), BROWSE_ENDPOINT, rewritten));
                    }
                } else if (!input.skipField(tag)) break;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not resolve native Music download/navigation command", ex);
        }
        return original;
    }

    private static final int COMMAND_EXECUTOR = 204158123;
    private static final int UPSELL_COMMAND = 443434441;
    // Download-state entity types in the header button's key: playlists use 24, albums use 17 (id is the OLAK audio playlist id).
    private static final int PLAYLIST_DOWNLOAD_ENTITY = 24;
    private static final int ALBUM_DOWNLOAD_ENTITY = 17;
    private static final String ENDPOINT_TAG = "com.google.android.libraries.youtube.innertube.endpoint.tag";

    /**
     * The playlist header's Download button sends a command batch whose only stock action is the Premium upsell.
     * The tapped button model is located in the header element by that exact batch; its download-state entity key
     * names the playlist. Any other upsell, or a header without that structure, keeps stock behavior.
     */
    private static String playlistFromHeaderUpsell(byte[] command, java.util.Map<?, ?> context) throws IOException {
        byte[] executor = fieldBytes(command, COMMAND_EXECUTOR);
        if (executor == null || !containsField(executor, UPSELL_COMMAND, 0) || context == null) return null;
        Object tag = context.get(ENDPOINT_TAG);
        if (tag == null) return null;
        for (java.lang.reflect.Field field : tag.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(tag);
            } catch (IllegalAccessException ex) {
                continue;
            }
            byte[] header = value instanceof com.google.protobuf.MessageLite message ? message.toByteArray()
                    : value instanceof byte[] raw ? raw : null;
            String playlistId = header == null ? null : playlistForButton(header, executor, 0);
            if (playlistId != null) return playlistId;
        }
        return null;
    }

    private static String playlistForButton(byte[] message, byte[] executor, int depth) {
        if (depth > 48) return null;
        java.util.List<byte[]> children = lengthDelimited(message);
        if (children == null) return null;
        boolean tapped = false;
        for (byte[] child : children) {
            try {
                if (java.util.Arrays.equals(fieldBytes(child, COMMAND_EXECUTOR), executor)) tapped = true;
            } catch (IOException ignored) {
                // Strings and bytes are not messages.
            }
        }
        if (tapped) {
            for (byte[] child : children) {
                String playlistId = playlistFromEntityKey(child);
                if (playlistId != null) return playlistId;
            }
            return null;
        }
        for (byte[] child : children) {
            String playlistId = playlistForButton(child, executor, depth + 1);
            if (playlistId != null) return playlistId;
        }
        return null;
    }

    private static String playlistFromEntityKey(byte[] value) {
        try {
            String text = new String(value, StandardCharsets.US_ASCII);
            if (!text.matches("[A-Za-z0-9_%=+/-]{8,}")) return null;
            byte[] key = Base64.decode(Uri.decode(text), Base64.URL_SAFE);
            int entityType = fieldNumber(key, 4);
            if (entityType != PLAYLIST_DOWNLOAD_ENTITY && entityType != ALBUM_DOWNLOAD_ENTITY) return null;
            byte[] id = fieldBytes(key, 2);
            return id == null ? null : new String(id, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    private static boolean containsField(byte[] message, int number, int depth) {
        if (depth > 16) return false;
        try {
            CodedInputStream input = CodedInputStream.newInstance(message);
            while (!input.isAtEnd()) {
                int tag = input.readTag();
                if (WireFormat.getTagFieldNumber(tag) == number) return true;
                if (WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    if (containsField(input.readByteArray(), number, depth + 1)) return true;
                } else if (!input.skipField(tag)) return false;
            }
        } catch (IOException ignored) {
            // Not a message.
        }
        return false;
    }

    /** Returns the length-delimited fields of a well-formed message, or null when the bytes are not a message. */
    private static java.util.List<byte[]> lengthDelimited(byte[] message) {
        java.util.List<byte[]> fields = new java.util.ArrayList<>();
        try {
            CodedInputStream input = CodedInputStream.newInstance(message);
            while (!input.isAtEnd()) {
                int tag = input.readTag();
                if (WireFormat.getTagFieldNumber(tag) == 0) return null;
                if (WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) fields.add(input.readByteArray());
                else if (!input.skipField(tag)) return null;
            }
        } catch (IOException ex) {
            return null;
        }
        return fields;
    }

    private static byte[] rewriteBrowse(byte[] endpoint) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(endpoint);
        while (!input.isAtEnd()) {
            int start = input.getTotalBytesRead();
            int tag = input.readTag();
            // signed-in Account -> Downloads carries browseId in field 2.
            int field = WireFormat.getTagFieldNumber(tag);
            if ((field == 1 || field == 2) && WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                String original = new String(input.readByteArray(), StandardCharsets.UTF_8);
                String browseId = switch (original) {
                    case "FEmusic_offline", "FEmusic_offline_songs" -> "FEmusic_library_sideloaded_tracks";
                    case "FEmusic_offline_playlists" -> "FEmusic_library_sideloaded_playlists";
                    case "FEmusic_offline_albums" -> "FEmusic_library_sideloaded_releases";
                    default -> null;
                };
                if (browseId != null) return replaceField(endpoint, start, input.getTotalBytesRead(),
                        field, browseId.getBytes(StandardCharsets.UTF_8));
            } else if (!input.skipField(tag)) break;
        }
        return null;
    }

    private static byte[] replaceField(byte[] message, int start, int end, int field, byte[] value)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(message.length + value.length);
        bytes.write(message, 0, start);
        CodedOutputStream output = CodedOutputStream.newInstance(bytes);
        output.writeByteArray(field, value);
        output.flush();
        bytes.write(message, end, message.length - end);
        return bytes.toByteArray();
    }

    private static String playlistFromDownload(byte[] endpoint) throws IOException {
        // Action 1 adds a download. Remove/cancel/refresh commands keep their stock semantics.
        if (fieldNumber(endpoint, 2) != 1) return null;
        byte[] id = fieldBytes(endpoint, 1);
        return id == null ? null : new String(id, StandardCharsets.UTF_8);
    }

    private static String playlistFromUpsell(byte[] endpoint) throws IOException {
        byte[] encoded = fieldBytes(endpoint, 1);
        if (encoded == null) return null;
        byte[] params = Base64.decode(Uri.decode(new String(encoded, StandardCharsets.UTF_8)), Base64.URL_SAFE);
        byte[] request = fieldBytes(params, 5);
        if (request == null) return null;
        byte[] download = fieldBytes(request, 10);
        if (download == null || fieldNumber(download, 4) != 1) return null;
        // The playlist target is field 2; a single-video upsell uses a different field.
        // Do not scan opaque strings or mistake the playlist id for an 11-byte song id.
        byte[] id = fieldBytes(download, 2);
        return id == null ? null : new String(id, StandardCharsets.UTF_8);
    }

    private static byte[] fieldBytes(byte[] message, int number) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(message);
        while (!input.isAtEnd()) {
            int tag = input.readTag();
            if (tag == (number << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED)) return input.readByteArray();
            if (!input.skipField(tag)) break;
        }
        return null;
    }

    private static int fieldNumber(byte[] message, int number) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(message);
        while (!input.isAtEnd()) {
            int tag = input.readTag();
            if (tag == number << 3) return input.readInt32();
            if (!input.skipField(tag)) break;
        }
        return 0;
    }
}
