package app.revanced.extension.shared.innertube.utils;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class AuthUtils {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    // Used to identify brand accounts.
    private static final String PAGE_ID_HEADER = "X-Goog-PageId";
    private static final String VISITOR_ID_HEADER = "X-Goog-Visitor-Id";
    private static final Map<String, String> REQUEST_HEADER = new LinkedHashMap<>(3);
    private static String authorization = "";
    private static String dataSyncId = "";
    private static String visitorId = "";
    private static boolean incognitoStatus = false;

    /**
     * Injection point.
     */
    public static void setRequestHeaders(String url, Map<String, String> requestHeaders) {
        if (requestHeaders == null) {
            return;
        }
        String newlyLoadedAuthorization = requestHeaders.get(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(newlyLoadedAuthorization) && !authorization.equals(newlyLoadedAuthorization)) {
            REQUEST_HEADER.put(AUTHORIZATION_HEADER, newlyLoadedAuthorization);
            authorization = newlyLoadedAuthorization;
            Logger.printDebug(() -> "new Authorization loaded: " + newlyLoadedAuthorization);
        }

        String newlyLoadedVisitorId = requestHeaders.get(VISITOR_ID_HEADER);
        if (StringUtils.isNotEmpty(newlyLoadedVisitorId) && !visitorId.equals(newlyLoadedVisitorId)) {
            REQUEST_HEADER.put(VISITOR_ID_HEADER, newlyLoadedVisitorId);
            visitorId = newlyLoadedVisitorId;
            Logger.printDebug(() -> "new VisitorId loaded: " + newlyLoadedVisitorId);
        }
    }

    /**
     * Injection point.
     */
    public static void setDataSyncIdAndIncognitoStatus(@Nullable String newlyLoadedDataSyncId, boolean newlyLoadedIncognitoStatus) {
        if (StringUtils.isEmpty(newlyLoadedDataSyncId)) {
            REQUEST_HEADER.remove(PAGE_ID_HEADER);
            dataSyncId = "";
        } else if (!dataSyncId.equals(newlyLoadedDataSyncId)) {
            REQUEST_HEADER.put(PAGE_ID_HEADER, newlyLoadedDataSyncId);
            dataSyncId = newlyLoadedDataSyncId;
            Logger.printDebug(() -> "new DataSyncId loaded: " + newlyLoadedDataSyncId);
        }
        incognitoStatus = newlyLoadedIncognitoStatus;
    }

    public static Map<String, String> getRequestHeader() {
        return REQUEST_HEADER;
    }

    public static boolean isNotLoggedIn() {
        return authorization.isEmpty() || (dataSyncId.isEmpty() && incognitoStatus);
    }
}
