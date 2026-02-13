package app.morphe.extension.shared.patches.auth.requests;

import static app.morphe.extension.shared.patches.auth.requests.AuthRoutes.RequestType;
import static app.morphe.extension.shared.patches.auth.requests.AuthRoutes.createAccessTokenBody;
import static app.morphe.extension.shared.patches.auth.requests.AuthRoutes.createActivationCodeBody;
import static app.morphe.extension.shared.patches.auth.requests.AuthRoutes.createRefreshTokenBody;

import android.util.Pair;

import androidx.annotation.Nullable;

import com.aurora.store.task.AuthTask;

import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.shared.innertube.client.YouTubeVRClient.ClientType;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("deprecation")
public class AuthRequest {
    /**
     * TCP connection and HTTP read timeout
     */
    private static final int CONNECTION_TIMEOUT_SECONDS = 5; // 5 Seconds.

    private static final ClientType YOUTUBE_VR_CLIENT =
            ClientType.QUEST1;

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex) {
        Logger.printDebug(() -> toastMessage, ex);
    }

    @Nullable
    public static JSONObject fetch(RequestType requestType, @Nullable String token) {
        try {
            return Utils.submitOnBackgroundThread(() -> fetchUrl(requestType, token)).get();
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printDebug(() -> "fetch failed: " + requestType, ex);
        }

        return null;
    }

    @Nullable
    public static Pair<String, String> fetch(String email, String token, boolean isAASToken) {
        try {
            return Utils.submitOnBackgroundThread(() -> {
                AuthTask authTask = new AuthTask();

                if (isAASToken) {
                    return new Pair<>(token, authTask.refreshAuthToken(email, token));
                } else {
                    return authTask.getAuthToken(email, token);
                }
            }).get();
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printDebug(() -> "fetch failed", ex);
        }

        return null;
    }

    @Nullable
    private static JSONObject fetchUrl(RequestType requestType, @Nullable String token) {
        Utils.verifyOffMainThread();

        final long startTime = System.currentTimeMillis();
        String url = requestType.getUrl();
        Logger.printDebug(() -> "fetching url: " + url);

        try {
            byte[] body = new byte[0];
            switch (requestType) {
                case GET_ACTIVATION_CODE -> body = createActivationCodeBody(YOUTUBE_VR_CLIENT);
                case GET_REFRESH_TOKEN -> body = createRefreshTokenBody(token);
                case GET_ACCESS_TOKEN -> body = createAccessTokenBody(token);
                default ->
                        Logger.printException(() -> "Unknown request type: " + requestType); // Should never happen.
            }
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    body
            );
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(requestBody);
            String userAgent = YOUTUBE_VR_CLIENT.getUserAgent();
            if (userAgent != null) {
                builder.header("User-Agent", userAgent);
            }
            Request request = builder.build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        return new JSONObject(responseBody.string());
                    }
                } else {
                    handleConnectionError("API not available with response code: "
                            + response.code() + " message: " + response.message(), null);
                }
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Connection timeout", ex);
        } catch (IOException ex) {
            handleConnectionError("Network error", ex);
        } catch (Exception ex) {
            Logger.printException(() -> "fetchUrl failed", ex);
        } finally {
            Logger.printDebug(() -> "fetchUrl: " + url + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }
}
