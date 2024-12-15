package app.revanced.extension.youtube.patches.misc.requests;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Objects;

import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.requests.Route;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.misc.client.AppClient.ClientType;

@SuppressWarnings("deprecation")
public final class PlayerRoutes {
    /**
     * The base URL of requests of non-web clients to the InnerTube internal API.
     */
    private static final String YOUTUBEI_V1_GAPIS_URL = "https://youtubei.googleapis.com/youtubei/v1/";

    static final Route.CompiledRoute GET_STREAMING_DATA = new Route(
            Route.Method.POST,
            "player" +
                    "?fields=streamingData" +
                    "&alt=proto"
    ).compile();

    static final Route.CompiledRoute GET_PLAYLIST_PAGE = new Route(
            Route.Method.POST,
            "next" +
                    "?fields=contents.singleColumnWatchNextResults.playlist.playlist"
    ).compile();

    /**
     * TCP connection and HTTP read timeout
     */
    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000; // 10 Seconds.

    private PlayerRoutes() {
    }

    static String createInnertubeBody(ClientType clientType, String videoId) {
        return createInnertubeBody(clientType, videoId, null);
    }

    static String createInnertubeBody(ClientType clientType, String videoId, String playlistId) {
        JSONObject innerTubeBody = new JSONObject();

        try {
            JSONObject context = new JSONObject();

            JSONObject client = new JSONObject();
            client.put("clientName", clientType.name());
            client.put("clientVersion", clientType.clientVersion);
            client.put("deviceModel", clientType.deviceModel);
            client.put("osVersion", clientType.osVersion);
            if (clientType.deviceMake != null) {
                client.put("deviceMake", clientType.deviceMake);
            }
            if (clientType.osName != null) {
                client.put("osName", clientType.osName);
            }
            if (clientType.androidSdkVersion != null) {
                client.put("androidSdkVersion", clientType.androidSdkVersion.toString());
            }
            String languageCode = Objects.requireNonNull(Utils.getContext()).getResources().getConfiguration().locale.getLanguage();
            client.put("hl", languageCode);

            context.put("client", client);

            innerTubeBody.put("context", context);
            innerTubeBody.put("contentCheckOk", true);
            innerTubeBody.put("racyCheckOk", true);
            innerTubeBody.put("videoId", videoId);
            if (playlistId != null) {
                innerTubeBody.put("playlistId", playlistId);
            }
        } catch (JSONException e) {
            Logger.printException(() -> "Failed to create innerTubeBody", e);
        }

        return innerTubeBody.toString();
    }

    /**
     * @noinspection SameParameterValue
     */
    static HttpURLConnection getPlayerResponseConnectionFromRoute(Route.CompiledRoute route, ClientType clientType) throws IOException {
        var connection = Requester.getConnectionFromCompiledRoute(YOUTUBEI_V1_GAPIS_URL, route);

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", clientType.userAgent);
        connection.setRequestProperty("X-YouTube-Client-Name", clientType.id);
        connection.setRequestProperty("X-YouTube-Client-Version", clientType.clientVersion);

        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        return connection;
    }
}
