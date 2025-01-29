package app.revanced.extension.music.patches.misc.requests;

import static app.revanced.extension.shared.requests.Route.Method.GET;

import java.io.IOException;
import java.net.HttpURLConnection;

import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.requests.Route;

class PipedRoutes {
    private static final String PIPED_URL = "https://pipedapi.kavin.rocks/";
    private static final Route GET_PLAYLIST = new Route(GET, "playlists/{playlist_id}");

    private PipedRoutes() {
    }

    static HttpURLConnection getPlaylistConnectionFromRoute(String... params) throws IOException {
        return Requester.getConnectionFromRoute(PIPED_URL, GET_PLAYLIST, params);
    }

}