package app.revanced.extension.shared.returnyoutubeusername.requests;

import static app.revanced.extension.shared.requests.Route.Method.GET;

import java.io.IOException;
import java.net.HttpURLConnection;

import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.requests.Route;

public class ChannelRoutes {
    public static final String YOUTUBEI_V3_GAPIS_URL = "https://www.googleapis.com/youtube/v3/";

    public static final Route GET_CHANNEL_DETAILS = new Route(GET, "channels?part=snippet&forHandle={handle}&key={api_key}");

    public ChannelRoutes() {
    }

    public static HttpURLConnection getChannelConnectionFromRoute(Route route, String... params) throws IOException {
        return Requester.getConnectionFromRoute(YOUTUBEI_V3_GAPIS_URL, route, params);
    }
}