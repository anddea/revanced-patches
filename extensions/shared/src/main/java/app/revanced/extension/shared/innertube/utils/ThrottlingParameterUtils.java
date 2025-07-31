package app.revanced.extension.shared.innertube.utils;

import android.util.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.revanced.extension.shared.innertube.utils.mediaservicecore.PlayerDataExtractor;
import app.revanced.extension.shared.innertube.utils.javatube.Cipher;
import okhttp3.*;

import org.apache.commons.lang3.StringUtils;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

/**
 * The functions used in this class are referenced below:
 * - <a href="https://github.com/felipeucelli/JavaTube/blob/ec9011fa2ed584b867d276e683c421059b87bec5/src/main/java/com/github/felipeucelli/javatube/Youtube.java">JavaTube</a>
 * - <a href="https://github.com/TeamNewPipe/NewPipeExtractor/blob/68b4c9acbae2d167e7b1209bb6bf0ae086dd427e/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeJavaScriptExtractor.java">NewPipeExtractor</a>
 */
public class ThrottlingParameterUtils {
    /**
     * Regular expression pattern to find the signature timestamp.
     */
    private static final Pattern SIGNATURE_TIMESTAMP_PATTERN = Pattern.compile("signatureTimestamp[=:](\\d+)");
    /**
     * Regular expression pattern to find the 'n' parameter in streamingUrl.
     */
    private static final Pattern THROTTLING_PARAM_N_PATTERN = Pattern.compile("[&?]n=([^&]+)");
    /**
     * Regular expression pattern to find the 's' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_S_PATTERN = Pattern.compile("s=([^&]+)");
    /**
     * Regular expression pattern to find the 'url' parameter in signatureCipher.
     */
    private static final Pattern THROTTLING_PARAM_URL_PATTERN = Pattern.compile("&url=([^&]+)");
    /**
     * Format of JavaScript url (TV).
     */
    private static final String PLAYER_JS_TV_URL_FORMAT =
            "https://www.youtube.com/s/player/%s/tv-player-es6.vflset/tv-player-es6.js";
    /**
     * Format of JavaScript url (Web).
     */
    private static final String PLAYER_JS_WEB_URL_FORMAT =
            "https://www.youtube.com/s/player/%s/player_ias.vflset/en_GB/base.js";
    /**
     * Path of javascript url containing global function.
     */
    private static final String PLAYER_JS_GLOBAL_FUNCTIONS_URL_PATH = "69b31e11";
    /**
     * Regular expression pattern to find variables used in JavaScript url.
     */
    private static final Pattern PLAYER_JS_IDENTIFIER_PATTERN =
            Pattern.compile("player\\\\/([a-z0-9]{8})\\\\/");
    /**
     * Url used to find variables used in JavaScript url.
     */
    private static final String IFRAME_API_URL =
            "https://www.youtube.com/iframe_api";
    /**
     * User-agent of the TV client being used by yt-dlp.
     */
    private static final String USER_AGENT_CHROMIUM =
            "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version";
    private static final String HTTPS = "https:";

    /**
     * Class used to deobfuscate, powered by JavaTube.
     */
    @Nullable
    private volatile static Cipher cipher = null;
    /**
     * Class used to deobfuscate, powered by SmartTube.
     */
    @Nullable
    private volatile static PlayerDataExtractor extractor = null;
    /**
     * Javascript contents.
     */
    @Nullable
    private volatile static String playerJs = null;
    /**
     * Url of javascript.
     */
    @Nullable
    private volatile static String playerJsUrl = null;
    /**
     * Field value included when sending a request.
     */
    @Nullable
    private volatile static String signatureTimestamp = null;

    private volatile static String playerJsUrlFormat = null;
    private volatile static boolean isInitialized = false;
    private volatile static boolean useV8JsEngine = false;

    /**
     * Typically, there are 10 to 30 available formats for a video.
     * Each format has a different streaming url, but the 'n' parameter in the response is the same.
     * If the obfuscated 'n' parameter and the deobfuscated 'n' parameter are put in a Map,
     * the remaining 9 to 29 streaming urls can be deobfuscated quickly using the values put in the Map.
     */
    private static final Map<String, String> nParamMap = new LinkedHashMap<>() {
        private static final int NUMBER_OF_N_PARAM = 50;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_N_PARAM;
        }
    };

    public static void initializeJavascript(boolean fetchPlayerJs, boolean useJ2V8) {
        if (isInitialized) {
            return;
        }
        if (!Utils.isNetworkConnected()) {
            return;
        }
        isInitialized = true;
        playerJsUrlFormat = useJ2V8
                ? PLAYER_JS_TV_URL_FORMAT
                : PLAYER_JS_WEB_URL_FORMAT;
        useV8JsEngine = useJ2V8;

        if (!fetchPlayerJs) {
            playerJsUrl = String.format(playerJsUrlFormat, PLAYER_JS_GLOBAL_FUNCTIONS_URL_PATH);
        }

        if (useJ2V8) {
            extractor = getExtractor();
        } else {
            cipher = getCipher();
        }
        playerJs = getPlayerJs();
        playerJsUrl = getPlayerJsUrl();
        signatureTimestamp = getSignatureTimestamp();
    }

    @Nullable
    private static String setSignatureTimestamp() {
        try {
            String playerJs = getPlayerJs();
            if (playerJs != null) {
                Matcher matcher = SIGNATURE_TIMESTAMP_PATTERN.matcher(playerJs);
                if (matcher.find()) {
                    String signatureTimestamp = matcher.group(1);
                    if (StringUtils.isNotEmpty(signatureTimestamp)) {
                        Logger.printDebug(() -> "signatureTimestamp: " + signatureTimestamp);
                        return signatureTimestamp;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setSignatureTimestamp failed", ex);
        }
        Logger.printDebug(() -> "signatureTimestamp not found");
        return null;
    }

    @Nullable
    public static String getSignatureTimestamp() {
        if (signatureTimestamp == null){
            signatureTimestamp = setSignatureTimestamp();
        }
        return signatureTimestamp;
    }

    @Nullable
    private static String setPlayerJsUrl() {
        String iframeContent = fetch(IFRAME_API_URL);
        if (iframeContent != null) {
            Matcher matcher = PLAYER_JS_IDENTIFIER_PATTERN.matcher(iframeContent);
            if (matcher.find()) {
                return cleanJavaScriptUrl(
                        String.format(playerJsUrlFormat, matcher.group(1))
                );
            }
        }
        Logger.printDebug(() -> "iframeContent not found");
        return null;
    }

    @Nullable
    private static String getPlayerJsUrl() {
        if (playerJsUrl == null){
            playerJsUrl = setPlayerJsUrl();
        }
        return playerJsUrl;
    }

    @Nullable
    private static String setPlayerJs() {
        String playerJsUrl = getPlayerJsUrl();
        if (playerJsUrl != null) {
            return fetch(playerJsUrl);
        }
        return null;
    }

    @Nullable
    private static String getPlayerJs() {
        if (playerJs == null) {
            playerJs = setPlayerJs();
        }
        return playerJs;
    }

    @Nullable
    private static Cipher setCipher() {
        String playerJs = getPlayerJs();
        String playerJsUrl = getPlayerJsUrl();
        if (playerJs != null && playerJsUrl != null) {
            return new Cipher(playerJs, playerJsUrl);
        }
        return null;
    }

    @Nullable
    private static Cipher getCipher() {
        if (cipher == null) {
            cipher = setCipher();
        }
        return cipher;
    }

    @Nullable
    private static PlayerDataExtractor setExtractor() {
        String playerJs = getPlayerJs();
        if (playerJs != null) {
            return new PlayerDataExtractor(playerJs);
        }
        return null;
    }

    @Nullable
    private static PlayerDataExtractor getExtractor() {
        if (extractor == null) {
            extractor = setExtractor();
        }
        return extractor;
    }

    @NonNull
    private static String cleanJavaScriptUrl(@NonNull String javaScriptPlayerUrl) {
        if (javaScriptPlayerUrl.startsWith("//")) {
            // https part has to be added manually if the URL is protocol-relative
            return HTTPS + javaScriptPlayerUrl;
        } else if (javaScriptPlayerUrl.startsWith("/")) {
            // https://www.youtube.com part has to be added manually if the URL is relative to
            // YouTube's domain
            return HTTPS + "//www.youtube.com" + javaScriptPlayerUrl;
        } else {
            return javaScriptPlayerUrl;
        }
    }

    private static void handleConnectionError(String toastMessage, @Nullable Exception ex) {
        Logger.printDebug(() -> toastMessage, ex);
    }

    @Nullable
    private static String fetch(@NonNull String url) {
        try {
            return Utils.submitOnBackgroundThread(() -> fetchUrl(url)).get();
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printDebug(() -> "Could not fetch url: " + url, ex);
        }

        return null;
    }

    @Nullable
    private static String fetchUrl(@NonNull String uri) {
        final long startTime = System.currentTimeMillis();
        Logger.printDebug(() -> "fetching url: " + uri);

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .header("Accept-Language", "en-US,en")
                    .header("User-Agent", USER_AGENT_CHROMIUM)
                    .build();

            try (Response response = client.newCall(request).execute())  {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        return responseBody.string();
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
            Logger.printException(() -> "fetching url failed", ex);
        } finally {
            Logger.printDebug(() -> "fetched url: " + uri + " took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static String decodeURL(String s) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
    }

    /**
     * Convert signatureCipher to streaming url with obfuscated 'n' parameter.
     * <p>
     * @param videoId           Current video id.
     * @param signatureCipher   The 'signatureCipher' included in the response.
     * @return                  Streaming url with obfuscated 'n' parameter.
     */
    @Nullable
    public static String getUrlWithThrottlingParameterObfuscated(@NonNull String videoId, @NonNull String signatureCipher) {
        try {
            if (useV8JsEngine) {
                PlayerDataExtractor extractor = getExtractor();
                if (extractor != null) {
                    Matcher paramSMatcher = THROTTLING_PARAM_S_PATTERN.matcher(signatureCipher);
                    Matcher paramUrlMatcher = THROTTLING_PARAM_URL_PATTERN.matcher(signatureCipher);
                    if (paramSMatcher.find() && paramUrlMatcher.find()) {
                        // The 's' parameter from signatureCipher.
                        String sParam = paramSMatcher.group(1);
                        // The 'url' parameter from signatureCipher.
                        String urlParam = paramUrlMatcher.group(1);
                        if (StringUtils.isNotEmpty(sParam) && StringUtils.isNotEmpty(urlParam)) {
                            // The 'sig' parameter converted by javascript rules.
                            String decodedSigParm = extractor.extractSig(decodeURL(sParam));
                            if (StringUtils.isNotEmpty(decodedSigParm)) {
                                String decodedUriParm = decodeURL(urlParam);
                                Logger.printDebug(() -> "Converted signatureCipher to obfuscatedUrl, videoId: " + videoId);
                                return decodedUriParm + "&sig=" + decodedSigParm;
                            }
                        }
                    }
                }
            } else {
                Cipher cipher = getCipher();
                if (cipher != null) {
                    Matcher paramSMatcher = THROTTLING_PARAM_S_PATTERN.matcher(signatureCipher);
                    Matcher paramUrlMatcher = THROTTLING_PARAM_URL_PATTERN.matcher(signatureCipher);
                    if (paramSMatcher.find() && paramUrlMatcher.find()) {
                        // The 's' parameter from signatureCipher.
                        String sParam = paramSMatcher.group(1);
                        // The 'url' parameter from signatureCipher.
                        String urlParam = paramUrlMatcher.group(1);
                        if (StringUtils.isNotEmpty(sParam) && StringUtils.isNotEmpty(urlParam)) {
                            // The 'sig' parameter converted by javascript rules.
                            String decodedSigParm = cipher.getSignature(decodeURL(sParam));
                            if (StringUtils.isNotEmpty(decodedSigParm)) {
                                String decodedUriParm = decodeURL(urlParam);
                                Logger.printDebug(() -> "Converted signatureCipher to obfuscatedUrl, videoId: " + videoId);
                                return decodedUriParm + "&sig=" + decodedSigParm;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterObfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to convert signatureCipher, videoId: " + videoId);
        return null;
    }

    /**
     * Deobfuscates the obfuscated 'n' parameter to a valid streaming url.
     * <p>
     * @param videoId       Current video id.
     * @param obfuscatedUrl Streaming url with obfuscated 'n' parameter.
     * @return              Deobfuscated streaming url.
     */
    @Nullable
    public static String getUrlWithThrottlingParameterDeobfuscated(@NonNull String videoId, @Nullable String obfuscatedUrl) {
        try {
            // Obfuscated url is empty.
            if (StringUtils.isEmpty(obfuscatedUrl)) {
                Logger.printDebug(() -> "obfuscatedUrl is empty, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // The 'n' parameter from obfuscatedUrl.
            String obfuscatedNParams = getThrottlingParameterFromStreamingUrl(obfuscatedUrl);

            // The 'n' parameter is null or empty.
            if (StringUtils.isEmpty(obfuscatedNParams)) {
                Logger.printDebug(() -> "'n' parameter not found in obfuscated streaming url, videoId: " + videoId);
                return obfuscatedUrl;
            }

            // If the deobfuscated 'n' parameter is in the Map, return it.
            String deobfuscatedNParam = nParamMap.get(obfuscatedNParams);
            if (deobfuscatedNParam != null) {
                Logger.printDebug(() -> "Cached 'n' parameter found, videoId: " + videoId + ", deobfuscatedNParams: " + deobfuscatedNParam);
                return replaceNParam(obfuscatedUrl, obfuscatedNParams, deobfuscatedNParam);
            }

            // Deobfuscate the 'n' parameter.
            Pair<String, String> deobfuscatedNParamPairs = decodeNParam(obfuscatedUrl, obfuscatedNParams);
            String deobfuscatedUrl = deobfuscatedNParamPairs.first;
            String deobfuscatedNParams = deobfuscatedNParamPairs.second;
            if (!deobfuscatedNParams.isEmpty()) {
                // If the 'n' parameter obfuscation was successful, put it in the map.
                nParamMap.putIfAbsent(obfuscatedNParams, deobfuscatedNParams);
                Logger.printDebug(() -> "Deobfuscated the 'n' parameter, videoId: " + videoId + ", deobfuscatedNParams: " + deobfuscatedNParams);
                return deobfuscatedUrl;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "getUrlWithThrottlingParameterDeobfuscated failed", ex);
        }

        Logger.printDebug(() -> "Failed to obfuscate 'n' parameter, videoId: " + videoId);
        return obfuscatedUrl;
    }

    /**
     * Extract the 'n' parameter from the streaming Url.
     * <p>
     * @param streamingUrl  The streaming url.
     * @return              The 'n' parameter.
     */
    @Nullable
    private static String getThrottlingParameterFromStreamingUrl(@NonNull String streamingUrl) {
        if (streamingUrl.contains("&n=") || streamingUrl.contains("?n=")) {
            final Matcher matcher = THROTTLING_PARAM_N_PATTERN.matcher(streamingUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    /**
     * Replace the 'n' parameter.
     * @param obfuscatedUrl         Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams     Obfuscated 'n' parameter.
     * @param deObfuscatedNParams   Deobfuscated 'n' parameter.
     * @return                      Deobfuscated streaming url.
     */
    @NonNull
    private static String replaceNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams, @NonNull String deObfuscatedNParams) {
        return obfuscatedUrl.replaceFirst("n=" + obfuscatedNParams, "n=" + deObfuscatedNParams);
    }

    /**
     * Deobfuscate the 'n' parameter.
     * <p>
     * @param obfuscatedUrl     Streaming url with obfuscated 'n' parameter.
     * @param obfuscatedNParams Obfuscated 'n' parameter.
     * @return                  Deobfuscated Pair(Deobfuscated streaming url, Deobfuscated 'n' parameter).
     */
    @NonNull
    private static Pair<String, String> decodeNParam(@NonNull String obfuscatedUrl, @NonNull String obfuscatedNParams) {
        try {
            String deObfuscatedNParams = "";
            if (useV8JsEngine) {
                PlayerDataExtractor extractor = getExtractor();
                if (extractor != null) {
                    // The 'n' parameter deobfuscated by javascript rules.
                    deObfuscatedNParams = extractor.extractNSig(obfuscatedNParams);
                }
            } else {
                Cipher cipher = getCipher();
                if (cipher != null) {
                    // The 'n' parameter deobfuscated by javascript rules.
                    deObfuscatedNParams = cipher.getNParam(obfuscatedNParams);
                }
            }
            if (StringUtils.isNotEmpty(deObfuscatedNParams)) {
                String deObfuscatedUrl = replaceNParam(obfuscatedUrl, obfuscatedNParams, deObfuscatedNParams);
                return new Pair<>(deObfuscatedUrl, deObfuscatedNParams);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "decodeNSig failed", ex);
        }

        return new Pair<>(obfuscatedUrl, "");
    }
}