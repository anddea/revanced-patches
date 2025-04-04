package app.revanced.extension.youtube.utils;

import java.util.Map;

import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class AuthUtils {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String[] REQUEST_HEADER_KEYS = {
            AUTHORIZATION_HEADER,
            "X-GOOG-API-FORMAT-VERSION",
            "X-Goog-Visitor-Id"
    };
    public static volatile String authorization = "";
    public static volatile String dataSyncId = "";
    public static volatile boolean isIncognito = false;
    public static volatile Map<String, String> requestHeader;
    public static volatile String playlistId = "";
    public static volatile String videoId = "";

    public static void setRequestHeaders(String url, Map<String, String> requestHeaders) {
        try {
            // Save requestHeaders whenever an account is switched.
            String auth = requestHeaders.get(AUTHORIZATION_HEADER);
            if (auth == null || authorization.equals(auth)) {
                return;
            }
            for (String key : REQUEST_HEADER_KEYS) {
                if (requestHeaders.get(key) == null) {
                    return;
                }
            }
            authorization = auth;
            requestHeader = requestHeaders;
        } catch (Exception ex) {
            Logger.initializationException(AuthUtils.class, "setRequestHeaders failure", ex);
        }
    }
}
