package app.revanced.extension.youtube.utils;

import androidx.annotation.NonNull;

import app.revanced.extension.youtube.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import app.revanced.extension.shared.utils.Logger;

public class GeminiUtils {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL_NAME = "gemini-2.0-flash";
    private static final String ACTION = ":generateContent?key=";

    public interface SummaryCallback {
        void onSuccess(String summary);
        void onFailure(String error);
    }

    public static void getVideoSummary(@NonNull String videoUrl, @NonNull String apiKey, @NonNull SummaryCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_API_URL + MODEL_NAME + ACTION + apiKey);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000); // 30 seconds connect timeout
                connection.setReadTimeout(300000); // 300 seconds / 5 minutes read timeout (some videos may take some time to parse)

                // region JSON payload
                String lang = Settings.REVANCED_LANGUAGE.get().getLanguage();
                String textPrompt = "Summarize the key points of this video in " + lang + " (language code). " +
                                    "Skip any preamble, intro phrases, or explanations — output only the summary.";
                JSONObject fileData = new JSONObject()
                        .put("mimeType", "video/*")
                        .put("fileUri", videoUrl);
                JSONObject videoPart = new JSONObject().put("fileData", fileData);
                JSONObject textPart = new JSONObject().put("text", textPrompt);
                JSONArray partsArray = new JSONArray().put(videoPart).put(textPart);
                JSONObject contents = new JSONObject().put("parts", partsArray);
                JSONObject requestBody = new JSONObject().put("contents", new JSONArray().put(contents));
                String jsonInputString = requestBody.toString();
                // endregion JSON payload

                Logger.printDebug(() -> "GeminiUtils: Sending JSON Payload: " + jsonInputString);

                // Send request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                BufferedReader reader = getBufferedReader(responseCode, connection, response);
                reader.close();

                // Process response
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    try {
                        String summary = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        callback.onSuccess(summary.trim());
                    } catch (JSONException jsonEx) {
                        Logger.printException(() -> "Gemini API Response JSON Parsing Error. Full Response: " + response, jsonEx);
                        String blockReason = extractBlockReason(jsonResponse);
                        if (blockReason != null) {
                            callback.onFailure("Content blocked: " + blockReason);
                        } else {
                            callback.onFailure("Failed to parse summary from API response. Check logs.");
                        }
                    }
                } else {
                    String errorMessage = "HTTP Error: " + responseCode;
                    String detailedError = response.toString();
                    try {
                        JSONObject errorResponse = new JSONObject(detailedError);
                        if (errorResponse.has("error") && errorResponse.getJSONObject("error").has("message")) {
                            errorMessage = errorResponse.getJSONObject("error").getString("message");
                        } else {
                            errorMessage += " - " + detailedError;
                        }
                    } catch (Exception jsonEx) {
                        errorMessage += " - " + detailedError;
                    }
                    Logger.printException(() -> "Gemini API Error Response: " + detailedError);
                    callback.onFailure(errorMessage);
                }

            } catch (java.net.SocketTimeoutException e) {
                Logger.printException(() -> "Gemini API request timed out", e);
                callback.onFailure("Request timed out after 5 minutes.");
            } catch (Exception e) {
                Logger.printException(() -> "Gemini API request failed", e);
                callback.onFailure(e.getMessage() != null ? e.getMessage() : "Unknown network error");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @NotNull
    private static BufferedReader getBufferedReader(int responseCode, HttpURLConnection connection, StringBuilder response) throws IOException {
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        }
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return reader;
    }

    private static String extractBlockReason(JSONObject jsonResponse) {
        try {
            if (jsonResponse.has("promptFeedback") && jsonResponse.getJSONObject("promptFeedback").has("blockReason")) {
                return jsonResponse.getJSONObject("promptFeedback").getString("blockReason");
            }
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.getString("finishReason"))) {
                        return "Blocked due to " + firstCandidate.getString("finishReason");
                    }
                    if (firstCandidate.has("safetyRatings")) {
                        JSONArray safetyRatings = firstCandidate.getJSONArray("safetyRatings");
                        for (int i = 0; i < safetyRatings.length(); i++) {
                            JSONObject rating = safetyRatings.getJSONObject(i);
                            if (rating.has("blocked") && rating.getBoolean("blocked")) {
                                return "Safety Setting (" + rating.optString("category", "Unknown") + ")";
                            }
                            if (rating.has("probability") && rating.getString("probability").endsWith("_HIGH")) {
                                return "High probability harm (" + rating.optString("category", "Unknown") + ")";
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Logger.printException(() -> "Error extracting block reason", e);
        }
        return null;
    }
}
