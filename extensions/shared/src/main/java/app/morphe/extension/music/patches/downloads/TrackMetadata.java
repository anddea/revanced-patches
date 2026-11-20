/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.downloads;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import app.morphe.extension.music.patches.misc.requests.MusicBrowseRequest;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.ThumbnailEntry;
import app.morphe.extension.shared.innertube.PlayerResponseOuterClass.VideoDetails;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

// tags for a download come from Music's own lock-screen
// metadata (title, artists, album, square cover), the same data its player shows for the track.
final class TrackMetadata {
    private static final int MAX_ARTWORK_BYTES = 1024 * 1024;

    private TrackMetadata() {}

    /**
     * @param playingTitle  Media session title, if the track is the one playing.
     * @param playingArtist Media session artist, if the track is the one playing.
     */
    static AudioTagger.Tags resolve(String videoId, @Nullable String playingTitle,
                                    @Nullable String playingArtist, VideoDetails details) {
        String title = null;
        String artist = null;
        String album = null;
        String artworkUrl = null;
        try {
            JSONObject response = MusicBrowseRequest.next(Utils.getContext(), videoId);
            JSONObject endpoint = response.optJSONObject("currentVideoEndpoint");
            JSONObject watch = endpoint == null ? null : endpoint.optJSONObject("watchEndpoint");
            JSONObject screen = response.optJSONObject("lockScreen");
            JSONObject lock = screen == null ? null : screen.optJSONObject("lockScreenRenderer");
            // A redirected video would describe a different track.
            if (watch != null && videoId.equals(watch.optString("videoId")) && lock != null) {
                title = text(lock.optJSONObject("title"));
                artist = text(lock.optJSONObject("shortBylineText"));
                album = text(lock.optJSONObject("albumText"));
                JSONObject thumbnail = lock.optJSONObject("thumbnail");
                artworkUrl = largest(thumbnail == null ? null : thumbnail.optJSONArray("thumbnails"));
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not resolve Music metadata, using stream details: " + videoId, ex);
        }
        title = firstNotBlank(title, playingTitle, details.getTitle(), videoId);
        artist = firstNotBlank(artist, playingArtist, stripTopic(details.getOwnerChannelName()), "");
        if (artworkUrl == null) {
            int width = 0;
            for (ThumbnailEntry entry : details.getThumbnail().getThumbnailsList()) {
                if (!entry.getUrl().isEmpty() && entry.getWidth() >= width) {
                    width = entry.getWidth();
                    artworkUrl = entry.getUrl();
                }
            }
        }
        byte[] artwork = artworkUrl == null ? null : artwork(videoId, artworkUrl);
        return new AudioTagger.Tags(title, artist, album == null || album.isBlank() ? null : album, artwork);
    }

    @Nullable
    private static String text(@Nullable JSONObject text) {
        if (text == null) return null;
        if (text.has("simpleText")) return text.optString("simpleText");
        JSONArray runs = text.optJSONArray("runs");
        if (runs == null) return null;
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject run = runs.optJSONObject(i);
            if (run != null) value.append(run.optString("text"));
        }
        return value.toString();
    }

    @Nullable
    private static String largest(@Nullable JSONArray thumbnails) {
        if (thumbnails == null) return null;
        String url = null;
        int width = 0;
        for (int i = 0; i < thumbnails.length(); i++) {
            JSONObject thumbnail = thumbnails.optJSONObject(i);
            if (thumbnail == null || thumbnail.optString("url").isEmpty()) continue;
            if (thumbnail.optInt("width") >= width) {
                width = thumbnail.optInt("width");
                url = thumbnail.optString("url");
            }
        }
        return url;
    }

    /** @return The cover, or null: a missing cover must not fail the download. */
    @Nullable
    private static byte[] artwork(String videoId, String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Artwork HTTP " + connection.getResponseCode());
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > MAX_ARTWORK_BYTES) throw new IOException("Artwork too large");
                }
            }
            byte[] image = bytes.toByteArray();
            if (AudioTagger.imageMimeType(image) == null) throw new IOException("Artwork is not JPEG or PNG");
            return image;
        } catch (Exception ex) {
            Logger.printException(() -> "Could not download artwork: " + videoId, ex);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static String firstNotBlank(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    /** Music uploads are owned by an "Artist - Topic" channel, which is not the artist name. */
    private static String stripTopic(String channelName) {
        return channelName.endsWith(" - Topic")
                ? channelName.substring(0, channelName.length() - " - Topic".length())
                : channelName;
    }
}
