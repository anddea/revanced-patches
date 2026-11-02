// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.morphe.extension.youtube.patches.queue;

import android.net.Uri;
import android.util.Base64;
import android.util.LruCache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageLite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

/**
 * Port of Reaseam's QueueMenu, QueueReply and QueueContext. Uses YouTube's own queue commands;
 * generated protobuf parsing and obfuscated ByteString access are bound by the bytecode patch.
 */
public final class QueuePatch {
    private static final int BROWSE = 48687626;
    private static final int WATCH = 48687757;
    private static final int WATCH_VIDEO_ID = 1;
    private static final int[] MENU_ITEMS = {98150882, 1, 66439850, 1, 77258115};
    private static final int ITEM_ICON = 2;
    private static final int ITEM_COMMAND = 3;
    private static final int ITEM_BADGE = 12;
    private static final int ICON_TYPE = 1;
    private static final long ICON_QUEUE_PLAY_NEXT = 251;
    private static final int CAST_PLAY_NEXT = 122330696;
    private static final int REQUEST_QUEUE = 3;
    private static final int REQUEST_OPERATION = 1;
    private static final int REQUEST_TARGET = 2;
    private static final long OPERATION_PLAY_NEXT = 2;
    private static final int RESPONSE_COMMAND = 4;
    private static final int PLAYER_STATE_COMMAND = 469942096;
    private static final int QUEUE_INSERT = 465666947;
    private static final int WATCH_MINIMIZED = 339694216;
    private static final int QUEUE_TOKEN = 71;
    private static final long POSITION_NEXT = 1;
    private static final long POSITION_LAST = 2;
    private static final String QUEUE_WATCH_PARAMS = "yAQB";
    private static final int ENDPOINT_PLAYLIST = 2;
    private static final int ENDPOINT_PLAYLIST_ITEM = 13;
    private static final int CONTEXT_TYPE = 1;
    private static final int CONTEXT_PLAYLIST = 5;
    private static final int CONTEXT_VIDEO = 6;
    private static final int CONTEXT_PLAYLIST_ITEM = 7;
    private static final int CONTEXT_UPDATED = 12;
    private static final long TYPE_QUEUE = 1;
    private static final long TYPE_SINGLE_VIDEO = 3;
    private static final String QUEUE_PLAYLIST_PREFIX = "QP";
    private static final int TRACKED_VIDEOS = 32;
    private record QueueItem(String playlistId, String itemId) { }
    private static final LruCache<String, QueueItem> queueItems = new LruCache<>(TRACKED_VIDEOS);

    public static boolean isEnabled() { return Settings.UNLOCK_QUEUE.get(); }

    /** Implemented using the app's parser with its generated extension registry. */
    private static MessageLite parse(MessageLite prototype, byte[] bytes) { return null; }

    public static MessageLite unlockMenu(MessageLite command) {
        if (!Settings.UNLOCK_QUEUE.get()) return command;
        try {
            byte[] bytes = command.toByteArray();
            String videoId = menuVideoId(bytes);
            if (videoId == null) return command;
            byte[] unlocked = rewrite(bytes, 0, videoId);
            return unlocked == bytes ? command : parse(command, unlocked);
        } catch (IllegalArgumentException exception) {
            Logger.printDebug(() -> "Menu is not a queue menu");
            return command;
        }
    }

    // Newer menus name their video in the cast item; older ones in the queue items that already hold the
    // queue command, beside the upsell shown while nothing plays.
    private static String menuVideoId(byte[] command) {
        List<byte[]> commands = new ArrayList<>();
        for (byte[] item : menuEntries(command, 0)) {
            byte[] value = Proto.bytes(item, ITEM_COMMAND);
            if (value != null) commands.add(value);
        }
        // Stop at the first cast command even when it has no video ID.
        for (byte[] value : commands) {
            byte[] cast = Proto.bytes(value, CAST_PLAY_NEXT);
            if (cast != null) return Proto.string(cast, 1);
        }
        for (byte[] value : commands) {
            byte[] state = Proto.bytes(value, PLAYER_STATE_COMMAND);
            String videoId = state == null ? null : playerStateVideoId(state);
            if (videoId != null) return videoId;
        }
        return null;
    }

    private static boolean isUpsell(byte[] item) {
        byte[] icon = Proto.bytes(item, ITEM_ICON);
        byte[] command = Proto.bytes(item, ITEM_COMMAND);
        if (icon == null || command == null) return false;
        boolean queueIcon = Proto.number(icon, ICON_TYPE, 0) == ICON_QUEUE_PLAY_NEXT;
        return queueIcon && Proto.bytes(command, BROWSE) != null;
    }

    private static byte[] queueItem(byte[] item, String videoId) {
        Map<Integer, byte[]> replacements = new LinkedHashMap<>();
        replacements.put(BROWSE, null);
        replacements.put(PLAYER_STATE_COMMAND, playerStateCommand(videoId, POSITION_NEXT));
        byte[] command = Proto.replace(Objects.requireNonNull(Proto.bytes(item, ITEM_COMMAND), "Queue item has no command"), replacements);
        replacements.clear();
        replacements.put(ITEM_COMMAND, command);
        replacements.put(ITEM_BADGE, null);
        return Proto.replace(item, replacements);
    }

    private static List<byte[]> menuEntries(byte[] message, int depth) {
        if (depth == MENU_ITEMS.length) return List.of(message);
        List<byte[]> entries = new ArrayList<>();
        for (Proto.Field field : Proto.fields(message)) {
            if (field.number() == MENU_ITEMS[depth] && field.wireType() == Proto.WIRE_LENGTH) {
                entries.addAll(menuEntries(Objects.requireNonNull(field.bytes(), "Length-delimited field has no bytes"), depth + 1));
            }
        }
        return entries;
    }

    private static byte[] rewrite(byte[] message, int depth, String videoId) {
        if (depth == MENU_ITEMS.length) return isUpsell(message) ? queueItem(message, videoId) : message;
        return Proto.rewriteLength(message, (number, value) -> number == MENU_ITEMS[depth] ? rewrite(value, depth + 1, videoId) : value);
    }

    public static ListenableFuture<Object> wrap(ListenableFuture<Object> response, String params) {
        if (!Settings.UNLOCK_QUEUE.get()) return response;
        byte[] command = queueCommand(params);
        return command == null ? response : new QueueResponse(response, command);
    }

    private static byte[] queueCommand(String params) {
        if (params == null || params.isEmpty()) return null;
        try {
            byte[] queue = Proto.bytes(Base64.decode(Uri.decode(params), Base64.URL_SAFE), REQUEST_QUEUE);
            if (queue == null) return null;
            byte[] target = require(Proto.bytes(queue, REQUEST_TARGET), "Queue request has no target");
            byte[] targetVideo = require(Proto.bytes(target, 1), "Queue target has no video");
            String videoId = require(Proto.string(targetVideo, 1), "Queue target has no video ID");
            long position = Proto.number(queue, REQUEST_OPERATION, 0) == OPERATION_PLAY_NEXT ? POSITION_NEXT : POSITION_LAST;
            return new Proto.Writer().bytes(PLAYER_STATE_COMMAND, playerStateCommand(videoId, position)).toByteArray();
        } catch (IllegalArgumentException exception) {
            Logger.printDebug(() -> "Active-devices parameters are not a queue request");
            return null;
        }
    }

    private static <T> T require(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    private static byte[] playerStateCommand(String videoId, long position) {
        Proto.Writer insertion = new Proto.Writer().string(1, token(videoId, position));
        Proto.Writer watch = new Proto.Writer().string(WATCH_VIDEO_ID, videoId).string(5, QUEUE_WATCH_PARAMS);
        Proto.Writer minimized = new Proto.Writer().message(2, new Proto.Writer().message(WATCH, watch));
        return new Proto.Writer()
                .message(1, new Proto.Writer().message(QUEUE_INSERT, insertion))
                .message(2, new Proto.Writer().message(WATCH_MINIMIZED, minimized)).toByteArray();
    }

    /** The video a player state command shaped like {@link #playerStateCommand}'s plays minimized. */
    private static String playerStateVideoId(byte[] command) {
        byte[] value = Proto.bytes(command, 2);
        if (value != null) value = Proto.bytes(value, WATCH_MINIMIZED);
        if (value != null) value = Proto.bytes(value, 2);
        if (value != null) value = Proto.bytes(value, WATCH);
        return value == null ? null : Proto.string(value, WATCH_VIDEO_ID);
    }

    private static String token(String videoId, long position) {
        byte[] token = new Proto.Writer().message(QUEUE_TOKEN,
                new Proto.Writer().message(1, new Proto.Writer().string(1, videoId).number(3, position))).toByteArray();
        return Uri.encode(Base64.encodeToString(token, Base64.URL_SAFE | Base64.NO_WRAP));
    }

    private static final class QueueResponse implements ListenableFuture<Object> {
        private final ListenableFuture<Object> response;
        private final byte[] command;
        // Null replacements are retried on the next get, as in the app's response wrapper.
        private volatile Object replaced;
        private QueueResponse(ListenableFuture<Object> response, byte[] command) {
            this.response = response;
            this.command = command;
        }
        @Override public Object get() throws InterruptedException, ExecutionException { return replace(response.get()); }
        @Override public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return replace(response.get(timeout, unit));
        }
        @Override public boolean cancel(boolean interrupt) { return response.cancel(interrupt); }
        @Override public boolean isCancelled() { return response.isCancelled(); }
        @Override public boolean isDone() { return response.isDone(); }
        @Override public void addListener(Runnable listener, Executor executor) { response.addListener(listener, executor); }
        private Object replace(Object message) {
            Object cached = replaced;
            if (cached != null) return cached;
            MessageLite proto = (MessageLite) message;
            byte[] bytes = proto.toByteArray();
            byte[] current = Proto.bytes(bytes, RESPONSE_COMMAND);
            Object result = current != null && Proto.bytes(current, BROWSE) != null
                    ? parse(proto, Proto.replace(bytes, Map.of(RESPONSE_COMMAND, command))) : message;
            replaced = result;
            return result;
        }
    }

    public static void watched(MessageLite currentVideo) {
        if (!Settings.UNLOCK_QUEUE.get() || currentVideo == null) return;
        byte[] endpoint = Proto.bytes(currentVideo.toByteArray(), WATCH);
        if (endpoint == null) return;
        String videoId = Proto.string(endpoint, WATCH_VIDEO_ID);
        if (videoId == null) return;
        String playlistId = Proto.string(endpoint, ENDPOINT_PLAYLIST);
        String itemId = Proto.string(endpoint, ENDPOINT_PLAYLIST_ITEM);
        if (itemId != null && playlistId != null && playlistId.startsWith(QUEUE_PLAYLIST_PREFIX)) {
            queueItems.put(videoId, new QueueItem(playlistId, itemId));
        } else {
            queueItems.remove(videoId);
        }
    }

    public static byte[] complete(byte[] current) {
        if (!Settings.UNLOCK_QUEUE.get()) return null;
        String videoId = VideoInformation.getVideoId();
        if (videoId.isEmpty()) return null;
        boolean hasVideo = current != null && Proto.bytes(current, CONTEXT_VIDEO) != null;
        if (hasVideo) return null;
        QueueItem item = queueItems.get(videoId);
        if (item == null && current != null) return null;
        Proto.Writer writer = new Proto.Writer().number(CONTEXT_TYPE, item == null ? TYPE_SINGLE_VIDEO : TYPE_QUEUE);
        if (item != null) writer.string(CONTEXT_PLAYLIST, item.playlistId);
        writer.string(CONTEXT_VIDEO, videoId);
        if (item != null) writer.string(CONTEXT_PLAYLIST_ITEM, item.itemId);
        byte[] updated = current == null ? null : Proto.bytes(current, CONTEXT_UPDATED);
        if (updated != null) writer.bytes(CONTEXT_UPDATED, updated);
        return writer.toByteArray();
    }
}
