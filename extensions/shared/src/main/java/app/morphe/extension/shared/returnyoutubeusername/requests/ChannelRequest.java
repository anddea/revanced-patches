package app.morphe.extension.shared.returnyoutubeusername.requests;

import static app.morphe.extension.shared.returnyoutubeusername.requests.ChannelRoutes.GET_CHANNEL_DETAILS;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class ChannelRequest {
    /**
     * TCP connection and HTTP read timeout.
     */
    private static final int HTTP_TIMEOUT_MILLISECONDS = 3 * 1000;

    /**
     * Any arbitrarily large value, but must be at least twice {@link #HTTP_TIMEOUT_MILLISECONDS}
     */
    private static final int MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 6 * 1000;

    @GuardedBy("itself")
    private static final Map<String, ChannelRequest> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(200) {
                private static final int CACHE_LIMIT = 100;

                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    return size() > CACHE_LIMIT; // Evict the oldest entry if over the cache limit.
                }
            });

    public static void fetchRequestIfNeeded(@NonNull String handle, @NonNull String apiKey, Boolean userNameFirst) {
        synchronized (cache) {
            if (cache.get(handle) == null) {
                cache.put(handle, new ChannelRequest(handle, apiKey, userNameFirst));
            }
        }
    }

    @Nullable
    public static ChannelRequest getRequestForHandle(@NonNull String handle) {
        synchronized (cache) {
            return cache.get(handle);
        }
    }

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex) {
        Logger.printInfo(() -> toastMessage, ex);
    }

    @Nullable
    private static JSONObject send(String handle, String apiKey) {
        Objects.requireNonNull(handle);
        Objects.requireNonNull(apiKey);

        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "Fetching channel handle for: " + handle);

        try {
            HttpURLConnection connection = ChannelRoutes.getChannelConnectionFromRoute(GET_CHANNEL_DETAILS, handle, apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "keep-alive"); // keep-alive is on by default with http 1.1, but specify anyways
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLISECONDS);

            final int responseCode = connection.getResponseCode();
            if (responseCode == 200) return Requester.parseJSONObject(connection);

            handleConnectionError("API not available with response code: "
                            + responseCode + " message: " + connection.getResponseMessage(),
                    null);
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "send failed", ex);
        } finally {
            Logger.printDebug(() -> "handle: " + handle + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    private static String fetch(@NonNull String handle, @NonNull String apiKey, Boolean userNameFirst) {
        final JSONObject channelJsonObject = send(handle, apiKey);
        if (channelJsonObject != null) {
            try {
                final String userName = channelJsonObject
                        .getJSONArray("items")
                        .getJSONObject(0)
                        .getJSONObject("brandingSettings")
                        .getJSONObject("channel")
                        .getString("title");
                return authorBadgeBuilder(handle, userName, userNameFirst);
            } catch (JSONException e) {
                Logger.printDebug(() -> "Fetch failed while processing response data for response: " + channelJsonObject);
            }
        }
        return null;
    }

    private static final String AUTHOR_BADGE_FORMAT = "\u202D%s\u2009%s";
    private static final String PARENTHESES_FORMAT = "(%s)";

    private static String authorBadgeBuilder(@NonNull String handle, @NonNull String userName, Boolean userNameFirst) {
        if (userNameFirst == null) {
            return userName;
        } else if (userNameFirst) {
            handle = String.format(Locale.ENGLISH, PARENTHESES_FORMAT, handle);
            if (!Utils.isRightToLeftLocale()) {
                return String.format(Locale.ENGLISH, AUTHOR_BADGE_FORMAT, userName, handle);
            }
        } else {
            userName = String.format(Locale.ENGLISH, PARENTHESES_FORMAT, userName);
        }
        return String.format(Locale.ENGLISH, AUTHOR_BADGE_FORMAT, handle, userName);
    }

    private final String handle;
    private final Future<String> future;

    private ChannelRequest(String handle, String apiKey, Boolean append) {
        this.handle = handle;
        this.future = Utils.submitOnBackgroundThread(() -> fetch(handle, apiKey, append));
    }

    @Nullable
    public String getStream() {
        try {
            return future.get(MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            Logger.printInfo(() -> "getStream timed out", ex);
        } catch (InterruptedException ex) {
            Logger.printException(() -> "getStream interrupted", ex);
            Thread.currentThread().interrupt(); // Restore interrupt status flag.
        } catch (ExecutionException ex) {
            Logger.printException(() -> "getStream failure", ex);
        }

        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChannelRequest{" + "handle='" + handle + '\'' + '}';
    }
}
