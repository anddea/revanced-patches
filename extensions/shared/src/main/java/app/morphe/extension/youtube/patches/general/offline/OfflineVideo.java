/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One downloaded video as stored in the offline index.
 */
public final class OfflineVideo {
    public final String videoId;
    public final String title;
    public final String author;
    public final String channelId;
    public final long lengthMs;
    public final String contentUri;
    public final String displayName;
    public final long sizeBytes;
    public final String qualityLabel;
    public final String codec;
    public final long downloadedAt;
    public final String thumbnailPath;
    public volatile long lastPositionMs;

    public OfflineVideo(@NonNull String videoId, @NonNull String title, @NonNull String author,
                        @NonNull String channelId, long lengthMs, @NonNull String contentUri,
                        @NonNull String displayName, long sizeBytes, @NonNull String qualityLabel,
                        @NonNull String codec, long downloadedAt, @NonNull String thumbnailPath,
                        long lastPositionMs) {
        this.videoId = videoId;
        this.title = title;
        this.author = author;
        this.channelId = channelId;
        this.lengthMs = lengthMs;
        this.contentUri = contentUri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.qualityLabel = qualityLabel;
        this.codec = codec;
        this.downloadedAt = downloadedAt;
        this.thumbnailPath = thumbnailPath;
        this.lastPositionMs = lastPositionMs;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("videoId", videoId);
        json.put("title", title);
        json.put("author", author);
        json.put("channelId", channelId);
        json.put("lengthMs", lengthMs);
        json.put("contentUri", contentUri);
        json.put("displayName", displayName);
        json.put("sizeBytes", sizeBytes);
        json.put("qualityLabel", qualityLabel);
        json.put("codec", codec);
        json.put("downloadedAt", downloadedAt);
        json.put("thumbnailPath", thumbnailPath);
        json.put("lastPositionMs", lastPositionMs);
        return json;
    }

    @Nullable
    static OfflineVideo fromJson(JSONObject json) {
        String videoId = json.optString("videoId", "");
        String contentUri = json.optString("contentUri", "");
        if (videoId.isEmpty() || contentUri.isEmpty()) return null;
        return new OfflineVideo(
                videoId,
                json.optString("title", videoId),
                json.optString("author", ""),
                json.optString("channelId", ""),
                json.optLong("lengthMs", 0),
                contentUri,
                json.optString("displayName", ""),
                json.optLong("sizeBytes", 0),
                json.optString("qualityLabel", ""),
                json.optString("codec", ""),
                json.optLong("downloadedAt", 0),
                json.optString("thumbnailPath", ""),
                json.optLong("lastPositionMs", 0)
        );
    }
}
