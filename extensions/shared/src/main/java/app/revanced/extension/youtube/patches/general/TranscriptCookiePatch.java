package app.revanced.extension.youtube.patches.general;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.chromium.net.UrlRequest;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class TranscriptCookiePatch {

    private static final boolean SET_TRANSCRIPT_COOKIES =
            Settings.SET_TRANSCRIPT_COOKIES.get();

    private static final boolean SET_TRANSCRIPT_COOKIES_ALL =
            SET_TRANSCRIPT_COOKIES && Settings.SET_TRANSCRIPT_COOKIES_ALL.get();

    private static final String TRANSCRIPT_COOKIES =
            Settings.TRANSCRIPT_COOKIES.get();

    private static final String USER_AGENT_CHROME =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";

    private static volatile boolean requireCookies = false;

    /**
     * Injection point.
     */
    public static void checkUrl(String url) {
        if (SET_TRANSCRIPT_COOKIES && !TRANSCRIPT_COOKIES.isEmpty()) {
            requireCookies = false;
            // https://www.youtube.com/api/timedtext?v={video_id}...
            if (StringUtils.isNotEmpty(url) && url.contains("api/timedtext")) {
                if (SET_TRANSCRIPT_COOKIES_ALL) {
                    requireCookies = true;
                } else {
                    try {
                        Uri uri = Uri.parse(url);
                        // 'Auto-translated subtitles' requests include the 'tlang' query parameter.
                        String tlang = uri.getQueryParameter("tlang");
                        requireCookies = StringUtils.isNotEmpty(tlang);
                    } catch (Exception ex) {
                        Logger.printException(() -> "checkUrl failure", ex);
                    }
                }
            }
        }
    }

    /**
     * Injection point.
     */
    public static UrlRequest overrideHeaders(UrlRequest.Builder builder) {
        if (SET_TRANSCRIPT_COOKIES && requireCookies) {
            return builder.addHeader("Cookie", TRANSCRIPT_COOKIES)
                    .addHeader("User-Agent", USER_AGENT_CHROME)
                    .build();
        }

        return builder.build();
    }

}
