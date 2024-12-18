package app.revanced.extension.shared.returnyoutubedislike.requests;

import static app.revanced.extension.shared.returnyoutubedislike.requests.ReturnYouTubeDislikeRoutes.getRYDConnectionFromRoute;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import app.revanced.extension.shared.requests.Requester;
import app.revanced.extension.shared.returnyoutubedislike.ReturnYouTubeDislike;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public class ReturnYouTubeDislikeApi {
    /**
     * {@link #fetchVotes(String)} TCP connection timeout
     */
    private static final int API_GET_VOTES_TCP_TIMEOUT_MILLISECONDS = 2 * 1000; // 2 Seconds.

    /**
     * {@link #fetchVotes(String)} HTTP read timeout.
     * To locally debug and force timeouts, change this to a very small number (ie: 100)
     */
    private static final int API_GET_VOTES_HTTP_TIMEOUT_MILLISECONDS = 4 * 1000; // 4 Seconds.

    /**
     * Default connection and response timeout for voting and registration.
     * <p>
     * Voting and user registration runs in the background and has has no urgency
     * so this can be a larger value.
     */
    private static final int API_REGISTER_VOTE_TIMEOUT_MILLISECONDS = 60 * 1000; // 60 Seconds.

    /**
     * Response code of a successful API call
     */
    private static final int HTTP_STATUS_CODE_SUCCESS = 200;

    /**
     * Indicates a client rate limit has been reached and the client must back off.
     */
    private static final int HTTP_STATUS_CODE_RATE_LIMIT = 429;

    /**
     * How long to wait until API calls are resumed, if the API requested a back off.
     * No clear guideline of how long to wait until resuming.
     */
    private static final int BACKOFF_RATE_LIMIT_MILLISECONDS = 10 * 60 * 1000; // 10 Minutes.

    /**
     * How long to wait until API calls are resumed, if any connection error occurs.
     */
    private static final int BACKOFF_CONNECTION_ERROR_MILLISECONDS = 2 * 60 * 1000; // 2 Minutes.

    /**
     * If non zero, then the system time of when API calls can resume.
     */
    private static volatile long timeToResumeAPICalls; // must be volatile, since different threads read/write to this

    /**
     * If the last API getVotes call failed for any reason (including server requested rate limit).
     * Used to prevent showing repeat connection toasts when the API is down.
     */
    private static volatile boolean lastApiCallFailed;

    public static boolean toastOnConnectionError = false;

    private ReturnYouTubeDislikeApi() {
    } // utility class

    /**
     * Clears any backoff rate limits in effect.
     * Should be called if RYD is turned on/off.
     */
    public static void resetRateLimits() {
        if (lastApiCallFailed || timeToResumeAPICalls != 0) {
            Logger.printDebug(() -> "Reset rate limit");
        }
        lastApiCallFailed = false;
        timeToResumeAPICalls = 0;
    }

    /**
     * @return True, if api rate limit is in effect.
     */
    private static boolean checkIfRateLimitInEffect(String apiEndPointName) {
        if (timeToResumeAPICalls == 0) {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (now > timeToResumeAPICalls) {
            timeToResumeAPICalls = 0;
            return false;
        }
        Logger.printDebug(() -> "Ignoring api call " + apiEndPointName + " as rate limit is in effect");
        return true;
    }

    /**
     * @return True, if a client rate limit was requested
     */
    private static boolean checkIfRateLimitWasHit(int httpResponseCode) {
        return httpResponseCode == HTTP_STATUS_CODE_RATE_LIMIT;
    }

    private static void updateRateLimitAndStats(boolean connectionError, boolean rateLimitHit) {
        if (connectionError && rateLimitHit) {
            throw new IllegalArgumentException();
        }
        if (connectionError) {
            timeToResumeAPICalls = System.currentTimeMillis() + BACKOFF_CONNECTION_ERROR_MILLISECONDS;
            lastApiCallFailed = true;
        } else if (rateLimitHit) {
            Logger.printDebug(() -> "API rate limit was hit. Stopping API calls for the next "
                    + BACKOFF_RATE_LIMIT_MILLISECONDS + " seconds");
            timeToResumeAPICalls = System.currentTimeMillis() + BACKOFF_RATE_LIMIT_MILLISECONDS;
            if (!lastApiCallFailed && toastOnConnectionError) {
                Utils.showToastLong(str("revanced_ryd_failure_client_rate_limit_requested"));
            }
            lastApiCallFailed = true;
        } else {
            lastApiCallFailed = false;
        }
    }

    private static void handleConnectionError(@NonNull String toastMessage, @Nullable Exception ex) {
        if (!lastApiCallFailed && toastOnConnectionError) {
            Utils.showToastShort(toastMessage);
        }
        if (ex != null) {
            Logger.printInfo(() -> toastMessage, ex);
        }
    }

    /**
     * @return NULL if fetch failed, or if a rate limit is in effect.
     */
    @Nullable
    public static RYDVoteData fetchVotes(String videoId) {
        Utils.verifyOffMainThread();
        Objects.requireNonNull(videoId);

        if (checkIfRateLimitInEffect("fetchVotes")) {
            return null;
        }
        Logger.printDebug(() -> "Fetching votes for: " + videoId);

        try {
            HttpURLConnection connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.GET_DISLIKES, videoId);
            // request headers, as per https://returnyoutubedislike.com/docs/fetching
            // the documentation says to use 'Accept:text/html', but the RYD browser plugin uses 'Accept:application/json'
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "keep-alive"); // keep-alive is on by default with http 1.1, but specify anyways
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            connection.setConnectTimeout(API_GET_VOTES_TCP_TIMEOUT_MILLISECONDS); // timeout for TCP connection to server
            connection.setReadTimeout(API_GET_VOTES_HTTP_TIMEOUT_MILLISECONDS); // timeout for server response

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                connection.disconnect(); // rate limit hit, should disconnect
                updateRateLimitAndStats(false, true);
                return null;
            }

            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                // Do not disconnect, the same server connection will likely be used again soon.
                JSONObject json = Requester.parseJSONObject(connection);
                try {
                    RYDVoteData votingData = new RYDVoteData(json);
                    updateRateLimitAndStats(false, false);
                    Logger.printDebug(() -> "Voting data fetched: " + votingData);
                    return votingData;
                } catch (JSONException ex) {
                    Logger.printException(() -> "Failed to parse video: " + videoId + " json: " + json, ex);
                    // fall thru to update statistics
                }
            } else {
                handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode), null);
            }
            connection.disconnect(); // something went wrong, might as well disconnect
        } catch (
                SocketTimeoutException ex) { // connection timed out, response timeout, or some other network error
            handleConnectionError((str("revanced_ryd_failure_connection_timeout")), ex);
        } catch (IOException ex) {
            handleConnectionError((str("revanced_ryd_failure_generic", ex.getMessage())), ex);
        } catch (Exception ex) {
            // should never happen
            Logger.printException(() -> "fetchVotes failure", ex);
        }

        updateRateLimitAndStats(true, false);
        return null;
    }

    /**
     * @return The newly created and registered user id.  Returns NULL if registration failed.
     */
    @Nullable
    public static String registerAsNewUser() {
        Utils.verifyOffMainThread();
        try {
            if (checkIfRateLimitInEffect("registerAsNewUser")) {
                return null;
            }
            String userId = randomString();
            Logger.printDebug(() -> "Trying to register new user");

            HttpURLConnection connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.GET_REGISTRATION, userId);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS);

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                connection.disconnect(); // disconnect, as no more connections will be made for a little while
                return null;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                String challenge = json.getString("challenge");
                int difficulty = json.getInt("difficulty");

                String solution = solvePuzzle(challenge, difficulty);
                return confirmRegistration(userId, solution);
            }
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode), null);
            connection.disconnect();
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "registration failed"), ex);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to register user", ex); // should never happen
        }
        return null;
    }

    @Nullable
    private static String confirmRegistration(String userId, String solution) {
        Utils.verifyOffMainThread();
        Objects.requireNonNull(userId);
        Objects.requireNonNull(solution);
        try {
            if (checkIfRateLimitInEffect("confirmRegistration")) {
                return null;
            }
            Logger.printDebug(() -> "Trying to confirm registration with solution: " + solution);

            HttpURLConnection connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.CONFIRM_REGISTRATION, userId);
            applyCommonPostRequestSettings(connection);

            String jsonInputString = "{\"solution\": \"" + solution + "\"}";
            byte[] body = jsonInputString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                connection.disconnect(); // disconnect, as no more connections will be made for a little while
                return null;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                Logger.printDebug(() -> "Registration confirmation successful");
                return userId;
            }
            // Something went wrong, might as well disconnect.
            String response = Requester.parseStringAndDisconnect(connection);
            Logger.printInfo(() -> "Failed to confirm registration for user: " + userId
                    + " solution: " + solution + " responseCode: " + responseCode + " response: '" + response + "''");
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode), null);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "confirm registration failed"), ex);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to confirm registration for user: " + userId
                    + "solution: " + solution, ex);
        }
        return null;
    }

    public static void sendVote(String userId, String videoId, ReturnYouTubeDislike.Vote vote) {
        Utils.verifyOffMainThread();
        Objects.requireNonNull(videoId);
        Objects.requireNonNull(vote);

        try {
            if (userId == null) return;

            if (checkIfRateLimitInEffect("sendVote")) {
                return;
            }
            Logger.printDebug(() -> "Trying to vote for video: " + videoId + " with vote: " + vote);

            HttpURLConnection connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.SEND_VOTE);
            applyCommonPostRequestSettings(connection);

            String voteJsonString = "{\"userId\": \"" + userId + "\", \"videoId\": \"" + videoId + "\", \"value\": \"" + vote.value + "\"}";
            byte[] body = voteJsonString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                connection.disconnect(); // disconnect, as no more connections will be made for a little while
                return;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONObject json = Requester.parseJSONObject(connection);
                String challenge = json.getString("challenge");
                int difficulty = json.getInt("difficulty");

                String solution = solvePuzzle(challenge, difficulty);
                confirmVote(videoId, userId, solution);
                return;
            }

            Logger.printInfo(() -> "Failed to send vote for video: " + videoId + " vote: " + vote
                    + " response code was: " + responseCode);
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode), null);
            connection.disconnect(); // something went wrong, might as well disconnect
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "send vote failed"), ex);
        } catch (Exception ex) {
            // should never happen
            Logger.printException(() -> "Failed to send vote for video: " + videoId + " vote: " + vote, ex);
        }
    }

    private static void confirmVote(String videoId, String userId, String solution) {
        Utils.verifyOffMainThread();
        Objects.requireNonNull(videoId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(solution);

        try {
            if (checkIfRateLimitInEffect("confirmVote")) {
                return;
            }
            Logger.printDebug(() -> "Trying to confirm vote for video: " + videoId + " solution: " + solution);
            HttpURLConnection connection = getRYDConnectionFromRoute(ReturnYouTubeDislikeRoutes.CONFIRM_VOTE);
            applyCommonPostRequestSettings(connection);

            String jsonInputString = "{\"userId\": \"" + userId + "\", \"videoId\": \"" + videoId + "\", \"solution\": \"" + solution + "\"}";
            byte[] body = jsonInputString.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            final int responseCode = connection.getResponseCode();
            if (checkIfRateLimitWasHit(responseCode)) {
                connection.disconnect(); // disconnect, as no more connections will be made for a little while
                return;
            }
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                Logger.printDebug(() -> "Vote confirm successful for video: " + videoId);
                return;
            }
            // Something went wrong, might as well disconnect.
            String response = Requester.parseStringAndDisconnect(connection);
            Logger.printInfo(() -> "Failed to confirm vote for video: " + videoId
                    + " solution: " + solution + " responseCode: " + responseCode + " response: '" + response + "'");
            handleConnectionError(str("revanced_ryd_failure_connection_status_code", responseCode), null);
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_ryd_failure_connection_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_ryd_failure_generic", "confirm vote failed"), ex);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to confirm vote for video: " + videoId
                    + " solution: " + solution, ex); // should never happen
        }
    }

    private static void applyCommonPostRequestSettings(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS); // timeout for TCP connection to server
        connection.setReadTimeout(API_REGISTER_VOTE_TIMEOUT_MILLISECONDS); // timeout for server response
    }


    private static String solvePuzzle(String challenge, int difficulty) {
        byte[] decodedChallenge = Base64.decode(challenge, Base64.NO_WRAP);

        byte[] buffer = new byte[20];
        System.arraycopy(decodedChallenge, 0, buffer, 4, 16);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex); // should never happen
        }

        final int maxCount = (int) (Math.pow(2, difficulty + 1) * 5);
        for (int i = 0; i < maxCount; i++) {
            buffer[0] = (byte) i;
            buffer[1] = (byte) (i >> 8);
            buffer[2] = (byte) (i >> 16);
            buffer[3] = (byte) (i >> 24);
            byte[] messageDigest = md.digest(buffer);

            if (countLeadingZeroes(messageDigest) >= difficulty) {
                return Base64.encodeToString(new byte[]{buffer[0], buffer[1], buffer[2], buffer[3]}, Base64.NO_WRAP);
            }
        }

        // should never be reached
        throw new IllegalStateException("Failed to solve puzzle challenge: " + challenge + " of difficulty: " + difficulty);
    }

    // https://stackoverflow.com/a/157202
    private static String randomString() {
        String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();

        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 36; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    private static int countLeadingZeroes(byte[] uInt8View) {
        int zeroes = 0;
        int value;
        for (byte b : uInt8View) {
            value = b & 0xFF;
            if (value == 0) {
                zeroes += 8;
            } else {
                int count = 1;
                if (value >>> 4 == 0) {
                    count += 4;
                    value <<= 4;
                }
                if (value >>> 6 == 0) {
                    count += 2;
                    value <<= 2;
                }
                zeroes += count - (value >>> 7);
                break;
            }
        }
        return zeroes;
    }
}
