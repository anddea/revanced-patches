package app.morphe.extension.music.sponsorblock.requests;

import static app.morphe.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.sponsorblock.SponsorBlockSettings;
import app.morphe.extension.music.sponsorblock.objects.SegmentCategory;
import app.morphe.extension.music.sponsorblock.objects.SponsorSegment;
import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.sponsorblock.requests.SBRoutes;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class SBRequester {
    /**
     * TCP timeout
     */
    private static final int TIMEOUT_TCP_DEFAULT_MILLISECONDS = 7000;

    /**
     * HTTP response timeout
     */
    private static final int TIMEOUT_HTTP_DEFAULT_MILLISECONDS = 10000;

    /**
     * Response code of a successful API call
     */
    private static final int HTTP_STATUS_CODE_SUCCESS = 200;

    private SBRequester() {
    }

    private static void handleConnectionError(@NonNull String toastMessage, @Nullable Exception ex) {
        if (Settings.SB_TOAST_ON_CONNECTION_ERROR.get()) {
            Utils.showToastShort(toastMessage);
        }
        if (ex != null) {
            Logger.printInfo(() -> toastMessage, ex);
        }
    }

    @NonNull
    public static SponsorSegment[] getSegments(@NonNull String videoId) {
        Utils.verifyOffMainThread();
        List<SponsorSegment> segments = new ArrayList<>();
        try {
            HttpURLConnection connection = getConnectionFromRoute(SBRoutes.GET_SEGMENTS, videoId, SegmentCategory.sponsorBlockAPIFetchCategories);
            final int responseCode = connection.getResponseCode();

            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONArray responseArray = Requester.parseJSONArray(connection);
                final long minSegmentDuration = 0;
                for (int i = 0, length = responseArray.length(); i < length; i++) {
                    JSONObject obj = (JSONObject) responseArray.get(i);
                    JSONArray segment = obj.getJSONArray("segment");
                    final long start = (long) (segment.getDouble(0) * 1000);
                    final long end = (long) (segment.getDouble(1) * 1000);

                    String uuid = obj.getString("UUID");
                    final boolean locked = obj.getInt("locked") == 1;
                    String categoryKey = obj.getString("category");
                    SegmentCategory category = SegmentCategory.byCategoryKey(categoryKey);
                    if (category == null) {
                        Logger.printException(() -> "Received unknown category: " + categoryKey); // should never happen
                    } else if ((end - start) >= minSegmentDuration) {
                        segments.add(new SponsorSegment(category, uuid, start, end, locked));
                    }
                }
                Logger.printDebug(() -> {
                    StringBuilder builder = new StringBuilder("Downloaded segments:");
                    for (SponsorSegment segment : segments) {
                        builder.append('\n').append(segment);
                    }
                    return builder.toString();
                });
                runVipCheckInBackgroundIfNeeded();
            } else if (responseCode == 404) {
                // no segments are found.  a normal response
                Logger.printDebug(() -> "No segments found for video: " + videoId);
            } else {
                handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_status", responseCode), null);
                connection.disconnect(); // something went wrong, might as well disconnect
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_generic"), ex);
        } catch (Exception ex) {
            // Should never happen
            Logger.printException(() -> "getSegments failure", ex);
        }

        return segments.toArray(new SponsorSegment[0]);
    }

    public static void runVipCheckInBackgroundIfNeeded() {
        if (!SponsorBlockSettings.userHasSBPrivateId()) {
            return; // User cannot be a VIP. User has never voted, created any segments, or has imported a SB user id.
        }
        long now = System.currentTimeMillis();
        if (now < (Settings.SB_LAST_VIP_CHECK.get() + TimeUnit.DAYS.toMillis(3))) {
            return;
        }
        Utils.runOnBackgroundThread(() -> {
            try {
                JSONObject json = getJSONObject(SponsorBlockSettings.getSBPrivateUserID());
                boolean vip = json.getBoolean("vip");
                Settings.SB_USER_IS_VIP.save(vip);
                Settings.SB_LAST_VIP_CHECK.save(now);
            } catch (IOException ex) {
                Logger.printInfo(() -> "Failed to check VIP (network error)", ex); // info, so no error toast is shown
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to check VIP", ex); // should never happen
            }
        });
    }

    // helpers

    private static HttpURLConnection getConnectionFromRoute(@NonNull Route route, String... params) throws IOException {
        HttpURLConnection connection = Requester.getConnectionFromRoute(Settings.SB_API_URL.get(), route, params);
        connection.setConnectTimeout(TIMEOUT_TCP_DEFAULT_MILLISECONDS);
        connection.setReadTimeout(TIMEOUT_HTTP_DEFAULT_MILLISECONDS);
        return connection;
    }

    private static JSONObject getJSONObject(String... params) throws IOException, JSONException {
        return Requester.parseJSONObject(getConnectionFromRoute(SBRoutes.IS_USER_VIP, params));
    }
}
