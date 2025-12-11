package com.dragons.aurora.playstoreapiv2;

import static com.dragons.aurora.playstoreapiv2.PackageNameUtil.getGmsCorePackageName;

import java.io.IOException;
import java.util.*;

/**
 * @author akdeniz, yeriomin, whyorean
 */
public class GooglePlayAPI {

    private static final String URL = "https://android.clients.google.com/auth";

    private HttpClientAdapter client;
    private Locale locale;
    private DeviceInfoProvider deviceInfoProvider;

    /**
     * Some methods instead of a protobuf return key-value pairs on each string
     */
    public static Map<String, String> parseResponse(String response) {
        Map<String, String> keyValueMap = new HashMap<>();
        StringTokenizer st = new StringTokenizer(response, "\n\r");
        while (st.hasMoreTokens()) {
            String[] keyValue = st.nextToken().split("=", 2);
            if (keyValue.length >= 2) {
                keyValueMap.put(keyValue[0], keyValue[1]);
            }
        }
        return keyValueMap;
    }

    public HttpClientAdapter getClient() {
        return client;
    }

    public void setClient(HttpClientAdapter httpClient) {
        this.client = httpClient;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setDeviceInfoProvider(DeviceInfoProvider deviceInfoProvider) {
        this.deviceInfoProvider = deviceInfoProvider;
    }

    public String generatePlayStoreToken(String email, String aasToken) throws IOException {
        return generateToken(
                email,
                aasToken,
                "com.android.vending",
                "38918a453d07199354f8b19af05ec6562ced5788",
                "oauth2:https://www.googleapis.com/auth/googleplay"
        );
    }

    public String generateYouTubeToken(String email, String aasToken) throws IOException {
        return generateToken(
                email,
                aasToken,
                "com.google.android.youtube",
                "24bb24c05e47e0aefa68a58a766179d9b613a600",
                "oauth2:https://www.googleapis.com/auth/youtube"
        );
    }

    public String generateYouTubeMusicToken(String email, String aasToken) throws IOException {
        return generateToken(
                email,
                aasToken,
                "com.google.android.apps.youtube.music",
                "afb0fed5eeaebdd86f56a97742f4b6b33ef59875",
                "oauth2:https://www.googleapis.com/auth/youtube"
        );
    }

    public String generateYouTubeUnpluggedToken(String email, String aasToken) throws IOException {
        return generateToken(
                email,
                aasToken,
                "com.google.android.apps.youtube.unplugged",
                "3a82b5ee26bc46bf68113d920e610cd090198d4a",
                "oauth2:https://www.googleapis.com/auth/youtube"
        );
    }

    /**
     * Authenticates on server with given email and password and sets
     * authentication token. This token can be used to login instead of using
     * email and password every time.
     */
    public String generateToken(String email, String aasToken,
                                String packageName, String signature,
                                String scopes) throws IOException {
        Map<String, String> params = getDefaultLoginParams(email);
        params.put("service", scopes);
        params.put("app", packageName);
        params.put("oauth2_foreground", "1");
        params.put("token_request_options", "CAA4AVAB");
        params.put("check_email", "1");
        params.put("Token", aasToken);
        params.put("client_sig", signature);
        params.put("callerPkg", getGmsCorePackageName());
        params.put("system_partition", "1");
        params.put("_opt_is_called_from_account_manager", "1");
        params.put("is_called_from_account_manager", "1");
        Map<String, String> headers = getAuthHeaders();
        headers.put("app", packageName);
        byte[] responseBytes = client.post(URL, params, headers);
        Map<String, String> response = parseResponse(new String(responseBytes));
        if (response.containsKey("Auth")) {
            return response.get("Auth");
        } else {
            throw new AuthException("Authentication failed! (login)");
        }
    }

    public String generateAASToken(String email, String oauthToken) throws IOException {
        Map<String, String> params = getDefaultLoginParams(email);
        params.put("service", "ac2dm");
        params.put("add_account", "1");
        params.put("get_accountid", "1");
        params.put("ACCESS_TOKEN", "1");
        params.put("callerPkg", getGmsCorePackageName());
        params.put("Token", oauthToken);
        //params.put("droidguard_results", "...");
        Map<String, String> headers = getAuthHeaders();
        headers.put("app", getGmsCorePackageName());
        byte[] responseBytes = client.post(URL, params, headers);
        Map<String, String> response = parseResponse(new String(responseBytes));
        if (response.containsKey("Token")) {
            return response.get("Token");
        } else {
            throw new AuthException("Authentication failed! (login aas)");
        }
    }

    /**
     * login methods use this
     * Most likely not all of these are required, but the Market app sends them, so we will too
     * <p>
     * client_sig is SHA1 digest of encoded certificate on
     * GoogleLoginService(package name : com.google.android.gsf) system APK.
     * But google doesn't seem to care of value of this parameter.
     */
    protected Map<String, String> getDefaultLoginParams(String email) {
        Map<String, String> params = new HashMap<>();
        params.put("sdk_version", String.valueOf(this.deviceInfoProvider.getSdkVersion()));
        params.put("Email", email);
        params.put("google_play_services_version", String.valueOf(this.deviceInfoProvider.getPlayServicesVersion()));
        params.put("device_country", this.locale.getCountry().toLowerCase());
        params.put("lang", this.locale.getLanguage().toLowerCase());
        params.put("callerSig", "38918a453d07199354f8b19af05ec6562ced5788");
        return params;
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", this.deviceInfoProvider.getAuthUserAgentString());
        return headers;
    }
}
