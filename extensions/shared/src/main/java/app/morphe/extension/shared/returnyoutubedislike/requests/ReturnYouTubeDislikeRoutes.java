package app.morphe.extension.shared.returnyoutubedislike.requests;

import static app.morphe.extension.shared.requests.Route.Method.GET;
import static app.morphe.extension.shared.requests.Route.Method.POST;

import java.io.IOException;
import java.net.HttpURLConnection;

import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.shared.requests.Route;

public class ReturnYouTubeDislikeRoutes {
    public static final String RYD_API_URL = "https://returnyoutubedislikeapi.com/";

    public static final Route SEND_VOTE = new Route(POST, "interact/vote");
    public static final Route CONFIRM_VOTE = new Route(POST, "interact/confirmVote");
    public static final Route GET_DISLIKES = new Route(GET, "votes?videoId={video_id}");
    public static final Route GET_REGISTRATION = new Route(GET, "puzzle/registration?userId={user_id}");
    public static final Route CONFIRM_REGISTRATION = new Route(POST, "puzzle/registration?userId={user_id}");

    public ReturnYouTubeDislikeRoutes() {
    }

    public static HttpURLConnection getRYDConnectionFromRoute(Route route, String... params) throws IOException {
        return Requester.getConnectionFromRoute(RYD_API_URL, route, params);
    }

}