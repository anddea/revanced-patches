/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3384
 * https://github.com/MorpheApp/morphe-patches/pull/3447
 *
 * Original author(s):
 * - Morphe contributors (https://github.com/MorpheApp/morphe-patches)
 * - COOLak (https://github.com/COOLak)
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.originaltitles;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import app.morphe.extension.shared.patches.LithoRelayoutPatch;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.youtube.patches.dearrow.DeArrowBrandingRequest;
import app.morphe.extension.youtube.patches.dearrow.DeArrowBrandingRequest.DeArrowTitle;
import app.morphe.extension.youtube.patches.utils.requests.ChannelIdRoutes;

/**
 * Fetches the titles that replace the titles shown by YouTube.
 * <p>
 * Original titles are fetched from the public oEmbed endpoint, which always returns the title
 * as set by the uploader. Titles that oEmbed does not return, such as the titles of the videos
 * whose embedding is disabled, are fetched from the player endpoint without an account.
 * DeArrow titles are fetched with {@link DeArrowBrandingRequest}, and if DeArrow has no title
 * then the original title is used if original titles are restored.
 * Casual mode of DeArrow also needs the original title, to check if the casual votes are for
 * the current original title.
 */
final class OriginalTitleRequest {

    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 5000;

    /**
     * Time before a title that failed to fetch because of network errors is fetched again.
     */
    private static final long FAILED_FETCH_RETRY_MILLISECONDS = 30_000;

    /**
     * Maximum number of times an original title that failed to fetch because of network errors or temporary
     * errors of the server is fetched again while the title is shown as loading. After that, the translated
     * title is shown, so a failing network does not keep loading, and the title is fetched again when loaded again.
     */
    private static final int MAX_FAILED_FETCH_RETRIES = 3;

    /**
     * Video id -> title. A null title means the video has no available title,
     * such as a private video, or the title failed to fetch because of network errors.
     */
    private static final Map<String, CompletableFuture<String>> cache =
            Collections.synchronizedMap(Utils.createSizeRestrictedMap(1000));

    /**
     * Original title and name of the channel of a video, as returned by the oEmbed endpoint,
     * or by the player endpoint if oEmbed does not return the title.
     */
    record OriginalVideo(String title, String channelName) {
    }

    /**
     * Video id -> original title and channel name, of the original titles that were fetched.
     * The title that replaces the title of the video can be a DeArrow title instead.
     */
    private static final Map<String, OriginalVideo> originalVideos =
            Collections.synchronizedMap(Utils.createSizeRestrictedMap(1000));

    /**
     * Video id -> time when the title that failed to fetch can be fetched again.
     */
    private static final Map<String, Long> retryTimes =
            Collections.synchronizedMap(Utils.createSizeRestrictedMap(1000));

    /**
     * Video id -> number of times the original title failed to fetch in a row
     * because of network errors or temporary errors of the server.
     */
    private static final Map<String, Integer> failedFetchCounts =
            Collections.synchronizedMap(Utils.createSizeRestrictedMap(1000));

    static CompletableFuture<String> fetch(String videoId) {
        synchronized (cache) {
            CompletableFuture<String> future = cache.get(videoId);
            Long retryTime = retryTimes.get(videoId);
            if (future == null || (retryTime != null && System.currentTimeMillis() >= retryTime)) {
                retryTimes.remove(videoId);
                String fetchedTitle = future != null && future.isDone() && !future.isCompletedExceptionally()
                        ? future.getNow(null)
                        : null;
                // Only the DeArrow title failed to fetch if the original title did not fail,
                // and was fetched or is not restored. The DeArrow title is fetched again, and the titles
                // fetched before are kept meanwhile, so the title is not shown as loading again
                // and the original title is not fetched again.
                if (fetchedTitle != null && !failedFetchCounts.containsKey(videoId)) {
                    CompletableFuture<String> fetchedFuture = future;
                    Utils.runOnBackgroundThread(() -> {
                        try {
                            DeArrowTitle deArrow = DeArrowBrandingRequest.fetchTitle(videoId);
                            OriginalVideo original = originalVideos.get(videoId);
                            String originalTitle = original != null ? original.title() : fetchedTitle;
                            if (originalTitle == null && deArrow.needsOriginalTitle()) {
                                originalTitle = fetchOriginalTitle(videoId);
                            }
                            String title = deArrow.titleFor(originalTitle);
                            if (title != null) {
                                // Not replaced if the titles were fetched again meanwhile.
                                cache.replace(videoId, fetchedFuture, CompletableFuture.completedFuture(title));
                            }
                        } catch (DeArrowBrandingRequest.DeArrowException ex) {
                            // The DeArrow title is fetched again later,
                            // and the titles fetched before are used meanwhile.
                            retryTimes.put(videoId, System.currentTimeMillis() + FAILED_FETCH_RETRY_MILLISECONDS);
                        }
                    });
                    return future;
                }
                future = CompletableFuture.supplyAsync(() -> fetchTitle(videoId), Utils::runOnBackgroundThread);
                cache.put(videoId, future);
            }
            return future;
        }
    }

    /**
     * Starts fetching the title if needed, and does not wait for it.
     *
     * @return The original title, or null if not yet available.
     */
    @Nullable
    static String getIfAvailable(String videoId) {
        return fetch(videoId).getNow(null);
    }

    /**
     * Does not start fetching the title.
     *
     * @return The original title and the name of the channel of the video,
     *         or null if the original title was not fetched.
     */
    @Nullable
    static OriginalVideo getOriginalIfFetched(String videoId) {
        return originalVideos.get(videoId);
    }

    /**
     * @return If the title is not yet fetched, or the original title failed to fetch and is fetched again
     *         until the maximum number of retries, so the title is shown as loading meanwhile.
     */
    static boolean isPending(String videoId) {
        CompletableFuture<String> future = cache.get(videoId);
        if (future == null || !future.isDone()) {
            return true;
        }
        Integer failures = failedFetchCounts.get(videoId);
        Long retryTime = retryTimes.get(videoId);
        return failures != null && failures <= MAX_FAILED_FETCH_RETRIES
                && retryTime != null && System.currentTimeMillis() < retryTime;
    }

    /**
     * @return Time until the title that failed to fetch can be fetched again, or 0 if it can be fetched.
     */
    static long retryRemainingMilliseconds(String videoId) {
        Long retryTime = retryTimes.get(videoId);
        return retryTime == null ? 0 : Math.max(0, retryTime - System.currentTimeMillis());
    }

    /**
     * The original title is fetched again after the requests to the server are no longer paused.
     * The loading texts are laid out again then, which fetches the title again only for the texts
     * that are still loaded, such as the elements that are not scrolled away.
     */
    private static void originalTitleFailed(String videoId) {
        final long delay = Math.max(FAILED_FETCH_RETRY_MILLISECONDS, RequestBackoff.pauseRemainingMilliseconds());
        retryTimes.put(videoId, System.currentTimeMillis() + delay);
        if (failedFetchCounts.merge(videoId, 1, Integer::sum) <= MAX_FAILED_FETCH_RETRIES) {
            Utils.runOnMainThreadDelayed(LithoRelayoutPatch::relayoutOutdatedTexts, delay);
        }
    }

    @Nullable
    private static String fetchTitle(String videoId) {
        String deArrowTitle = null;
        String originalTitle = null;
        boolean originalFetched = false;
        if (RestoreOriginalTitlesPatch.USE_DEARROW) {
            try {
                DeArrowTitle deArrow = DeArrowBrandingRequest.fetchTitle(videoId);
                if (deArrow.needsOriginalTitle()) {
                    originalTitle = fetchOriginalTitle(videoId);
                    originalFetched = true;
                }
                deArrowTitle = deArrow.titleFor(originalTitle);
                if (deArrowTitle != null) {
                    return deArrowTitle;
                }
            } catch (DeArrowBrandingRequest.DeArrowException ex) {
                // The DeArrow title is fetched again later, and the original title is used meanwhile.
                retryTimes.put(videoId, System.currentTimeMillis() + FAILED_FETCH_RETRY_MILLISECONDS);
            }
        }
        if (!RestoreOriginalTitlesPatch.RESTORE_ORIGINAL) {
            return null;
        }
        return originalFetched ? originalTitle : fetchOriginalTitle(videoId);
    }

    @Nullable
    private static String fetchOriginalTitle(String videoId) {
        try {
            //noinspection CharsetObjectCanBeUsed
            String url = "https://www.youtube.com/oembed?format=json&url="
                    + URLEncoder.encode("https://www.youtube.com/watch?v=" + videoId, StandardCharsets.UTF_8.name());

            HttpURLConnection connection = Requester.openConnection(url);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MILLISECONDS);

            final int responseCode = connection.getResponseCode();
            if (responseCode == Requester.HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                String title = json.optString("title");
                if (title.isEmpty()) {
                    return null;
                }
                return originalTitleFetched(videoId, title, json.optString("author_name"));
            }
            Logger.printDebug(() -> "oEmbed request failed for: " + videoId + " code: " + responseCode);

            // oEmbed does not return the title of some videos,
            // such as the videos whose embedding is disabled by the uploader (code 401).
            return fetchPlayerTitle(videoId);
        } catch (IOException ex) {
            Logger.printInfo(() -> "Could not fetch original title of: " + videoId, ex);
            originalTitleFailed(videoId);
        } catch (Exception ex) {
            Logger.printException(() -> "fetchOriginalTitle failure", ex);
        }
        return null;
    }

    /**
     * Fetches the original title from the player endpoint without an account,
     * which returns the title as set by the uploader, and not the translated title.
     */
    @Nullable
    private static String fetchPlayerTitle(String videoId) throws IOException, JSONException {
        if (RequestBackoff.isPaused()) {
            originalTitleFailed(videoId);
            return null;
        }
        byte[] requestBody = ChannelIdRoutes.createBody(videoId);
        HttpURLConnection connection = ChannelIdRoutes.getConnection(ChannelIdRoutes.GET_TITLE);
        connection.setFixedLengthStreamingMode(requestBody.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(requestBody);
        }

        final int responseCode = connection.getResponseCode();
        if (responseCode != Requester.HTTP_STATUS_CODE_SUCCESS) {
            if (RequestBackoff.isTemporaryError(responseCode)) {
                // Fetched again later, as the server limits the requests or is unavailable.
                RequestBackoff.onTemporaryError(connection, responseCode);
                originalTitleFailed(videoId);
                return null;
            }
            Logger.printDebug(() -> "Player title request failed for: " + videoId + " code: " + responseCode);
            return null;
        }
        JSONObject videoDetails = Requester.parseJSONObject(connection).optJSONObject("videoDetails");
        String title = videoDetails == null ? "" : videoDetails.optString("title");
        if (title.isEmpty()) {
            Logger.printDebug(() -> "Player response has no title for: " + videoId);
            return null;
        }
        Logger.printDebug(() -> "Original title fetched from the player endpoint for: " + videoId);
        return originalTitleFetched(videoId, title, videoDetails.optString("author"));
    }

    private static String originalTitleFetched(String videoId, String title, String channelName) {
        failedFetchCounts.remove(videoId);
        originalVideos.put(videoId, new OriginalVideo(title, channelName.trim()));
        TitleLayouts.originalTitleFetched(videoId, title);
        return title;
    }
}
