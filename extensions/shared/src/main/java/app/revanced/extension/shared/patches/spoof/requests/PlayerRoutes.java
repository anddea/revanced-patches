package app.revanced.extension.shared.patches.spoof.requests;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import app.revanced.extension.shared.patches.client.AppClient.ClientType;
import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.requests.Route;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"ExtractMethodRecommender", "deprecation"})
public final class PlayerRoutes {
    public static final Route.CompiledRoute GET_PLAYLIST_PAGE = new Route(
            Route.Method.POST,
            "next" +
                    "?fields=contents.singleColumnWatchNextResults.playlist.playlist"
    ).compile();
    static final Route.CompiledRoute GET_STREAMING_DATA = new Route(
            Route.Method.POST,
            "player" +
                    "?fields=streamingData" +
                    "&alt=proto"
    ).compile();
    private static final String YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/";
    /**
     * TCP connection and HTTP read timeout
     */
    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000; // 10 Seconds.

    private static final String LOCALE_LANGUAGE = Utils.getContext().getResources()
            .getConfiguration().locale.getLanguage();

    private PlayerRoutes() {
    }

    public static String createInnertubeBody(ClientType clientType) {
        return createInnertubeBody(clientType, false);
    }

    public static String createInnertubeBody(ClientType clientType, boolean playlistId) {
        JSONObject innerTubeBody = new JSONObject();

        try {
            JSONObject client = new JSONObject();
            client.put("clientName", clientType.clientName);
            client.put("clientVersion", clientType.clientVersion);
            client.put("deviceModel", clientType.deviceModel);
            client.put("osVersion", clientType.osVersion);
            if (clientType.androidSdkVersion != null) {
                client.put("androidSdkVersion", clientType.androidSdkVersion);
            }
            client.put("hl", LOCALE_LANGUAGE);

            JSONObject context = new JSONObject();
            context.put("client", client);

            innerTubeBody.put("context", context);
            innerTubeBody.put("contentCheckOk", true);
            innerTubeBody.put("racyCheckOk", true);
            innerTubeBody.put("videoId", "%s");
            if (playlistId) {
                innerTubeBody.put("playlistId", "%s");
            }
        } catch (JSONException e) {
            Logger.printException(() -> "Failed to create innerTubeBody", e);
        }

        return innerTubeBody.toString();
    }

    /**
     * @noinspection SameParameterValue
     */
    public static HttpURLConnection getPlayerResponseConnectionFromRoute(Route.CompiledRoute route, ClientType clientType) throws IOException {
        var connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route);

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", clientType.userAgent);

        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        return connection;
    }
}