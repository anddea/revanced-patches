package app.revanced.extension.shared.patches.auth.requests;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import app.revanced.extension.shared.innertube.client.YouTubeVRClient.ClientType;
import app.revanced.extension.shared.utils.Logger;

public class AuthRoutes {
    private static final String OAUTH2_ACTIVATION_CODE_API_URL =
            "https://www.youtube.com/o/oauth2/device/code";
    private static final String OAUTH2_TOKEN_API_URL =
            "https://www.youtube.com/o/oauth2/token";

    private static final String OAUTH2_YOUTUBE_SCOPE =
            "https://www.googleapis.com/auth/youtube";
    private static final String GRANT_TYPE_DEFAULT =
            "http://oauth.net/grant_type/device/1.0";
    private static final String GRANT_TYPE_REFRESH =
            "refresh_token";

    private static final String YOUTUBE_VR_CLIENT_ID =
            "652469312169-4lvs9bnhr9lpns9v451j5oivd81vjvu1.apps.googleusercontent.com";
    private static final String YOUTUBE_VR_CLIENT_SECRET =
            "3fTWrBJI5Uojm1TK7_iJCW5Z";

    private AuthRoutes() {
    }

    public enum RequestType {
        GET_ACTIVATION_CODE(OAUTH2_ACTIVATION_CODE_API_URL),
        GET_REFRESH_TOKEN(OAUTH2_TOKEN_API_URL),
        GET_ACCESS_TOKEN(OAUTH2_TOKEN_API_URL);

        private final String url;

        public String getUrl() {
            return url + "?prettyPrint=false";
        }

        RequestType(String url) {
            this.url = url;
        }
    }

    static byte[] createActivationCodeBody(ClientType clientType) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", YOUTUBE_VR_CLIENT_ID);
            jsonObject.put("scope", OAUTH2_YOUTUBE_SCOPE);
            jsonObject.put("device_id", UUID.randomUUID().toString());
            jsonObject.put("device_model", clientType.name());
        } catch (JSONException e) {
            Logger.printException(() -> "createActivationCodeBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] createRefreshTokenBody(String code) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", YOUTUBE_VR_CLIENT_ID);
            jsonObject.put("client_secret", YOUTUBE_VR_CLIENT_SECRET);
            jsonObject.put("code", code);
            jsonObject.put("grant_type", GRANT_TYPE_DEFAULT);
        } catch (JSONException e) {
            Logger.printException(() -> "createRefreshTokenBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] createAccessTokenBody(String refreshToken) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", YOUTUBE_VR_CLIENT_ID);
            jsonObject.put("client_secret", YOUTUBE_VR_CLIENT_SECRET);
            jsonObject.put("refresh_token", refreshToken);
            jsonObject.put("grant_type", GRANT_TYPE_REFRESH);
        } catch (JSONException e) {
            Logger.printException(() -> "createAccessTokenBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }
}