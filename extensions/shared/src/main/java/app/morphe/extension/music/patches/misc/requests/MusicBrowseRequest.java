/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.extension.music.patches.misc.requests;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.requests.Requester;

// share the app-authenticated browse transport with playlist downloads,
// and the same transport for "next", whose lock-screen metadata names each downloaded track.
public final class MusicBrowseRequest {
    private MusicBrowseRequest() {}

    public static JSONObject browse(Context context, String browseId,
                                    @Nullable String continuation, @Nullable String fields)
            throws IOException, JSONException {
        JSONObject body = new JSONObject().put("context", context(context));
        body.put(continuation == null ? "browseId" : "continuation",
                continuation == null ? browseId : continuation);
        String query = fields == null ? "" : "&fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8.name());
        try {
            return post(context, "browse", body, query);
        } catch (BadRequestException ex) {
            // The AA field mask is an optimisation, never a requirement for browsing.
            if (fields != null) return browse(context, browseId, continuation, null);
            throw ex;
        }
    }

    /** Music's watch response for one video; its lock-screen renderer is Music's own track metadata. */
    public static JSONObject next(Context context, String videoId) throws IOException, JSONException {
        return post(context, "next", new JSONObject().put("context", context(context)).put("videoId", videoId), "");
    }

    private static final class BadRequestException extends IOException {
        BadRequestException() {
            super("Music request HTTP 400");
        }
    }

    private static String version(Context context) throws IOException {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception ex) {
            throw new IOException("Cannot determine Music client version", ex);
        }
    }

    private static JSONObject context(Context context) throws IOException, JSONException {
        Locale locale = Locale.getDefault();
        JSONObject client = new JSONObject()
                .put("clientName", "ANDROID_MUSIC").put("clientVersion", version(context))
                .put("osName", "Android").put("osVersion", Build.VERSION.RELEASE)
                .put("androidSdkVersion", Build.VERSION.SDK_INT).put("hl", locale.toLanguageTag());
        if (!locale.getCountry().isEmpty()) client.put("gl", locale.getCountry());
        return new JSONObject().put("client", client);
    }

    private static JSONObject post(Context context, String endpoint, JSONObject body, String query)
            throws IOException, JSONException {
        String version = version(context);
        String url = "https://youtubei.googleapis.com/youtubei/v1/" + endpoint + "?prettyPrint=false" + query;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "com.google.android.apps.youtube.music/" + version
                    + " (Linux; U; Android " + Build.VERSION.RELEASE + ") gzip");
            connection.setRequestProperty("X-YouTube-Client-Name", "21");
            connection.setRequestProperty("X-YouTube-Client-Version", version);
            for (Map.Entry<String, String> header : AuthUtils.getRequestHeader().entrySet()) {
                if (header.getValue() != null && !header.getValue().isEmpty()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_BAD_REQUEST) throw new BadRequestException();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("Music " + endpoint + " HTTP " + status);
            JSONObject response = Requester.parseJSONObject(connection);
            if (response.has("error")) throw new IOException("Music " + endpoint + " returned an error");
            return response;
        } finally {
            connection.disconnect();
        }
    }
}
