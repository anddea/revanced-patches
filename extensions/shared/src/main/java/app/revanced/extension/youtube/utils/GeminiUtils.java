package app.revanced.extension.youtube.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class GeminiUtils {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String GEMINI_MODEL = "gemini-2.5-flash-preview-04-17";
    private static final String ACTION = ":generateContent?key=";
    private static final AtomicReference<Future<?>> currentTask = new AtomicReference<>(null);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private static volatile HttpURLConnection currentConnection = null;

    /**
     * Initiates an asynchronous request to the Gemini API to generate a summary for the given video URL.
     * Cancels any previous ongoing task before starting.
     *
     * @param videoUrl The publicly accessible URL of the video to summarize.
     * @param apiKey   The Gemini API key.
     * @param callback The {@link Callback} to receive the summary result or an error message.
     */
    public static void getVideoSummary(@NonNull String videoUrl, @NonNull String apiKey, @NonNull Callback callback) {
        String langName = getLanguageName(); // Use Language Name to get full language name
        String prompt = "Summarize the key points of this video in " + langName + ". Skip any preamble, intro phrases, or explanations — output only the summary.";
        Logger.printDebug(() -> "GeminiUtils (SUMMARY): Sending JSON Payload (Prompt): " + prompt);
        generateContent(videoUrl, apiKey, prompt, callback);
    }

    /**
     * Initiates an asynchronous request to the Gemini API to generate a transcription for the given video URL.
     * Cancels any previous ongoing task before starting.
     *
     * @param videoUrl The publicly accessible URL of the video to transcribe.
     * @param apiKey   The Gemini API key.
     * @param callback The {@link Callback} to receive the transcription result or an error message.
     */
    public static void getVideoTranscription(@NonNull String videoUrl, @NonNull String apiKey, @NonNull Callback callback) {
        String langName = getLanguageName(); // Use language Name to get full language name
        String prompt = "Transcribe this video precisely in " + langName + ", including spoken words, written words in the video and significant sounds. Provide timestamps for each segment in the format [HH:MM:SS.mmm - HH:MM:SS.mmm]: Text. Skip any preamble, intro phrases, or explanations — output only the transcription.";
        Logger.printDebug(() -> "GeminiUtils (TRANSCRIPTION): Sending JSON Payload (Prompt): " + prompt);
        generateContent(videoUrl, apiKey, prompt, callback);
    }

    /**
     * Makes an asynchronous POST request to the Gemini API's generateContent endpoint.
     * Constructs the JSON payload including the video URI and text prompt.
     * Handles the API response, parsing the result or error.
     * Manages the task lifecycle and cancellation via {@link #currentTask} and {@link #currentConnection}.
     *
     * @param videoUrl   The publicly accessible URL of the video.
     * @param apiKey     The Gemini API key.
     * @param textPrompt The specific text prompt to send along with the video.
     * @param callback   The {@link Callback} to handle the success or failure response.
     */
    private static void generateContent(@NonNull String videoUrl, @NonNull String apiKey, @NonNull String textPrompt, @NonNull Callback callback) {
        cancelCurrentTask();

        final AtomicReference<Future<?>> taskRef = new AtomicReference<>();
        Future<?> newTask = executor.submit(() -> {
            HttpURLConnection connection = null;
            currentConnection = null;
            Future<?> taskBeingRun = taskRef.get();

            try {
                URL url = new URL(BASE_API_URL + GEMINI_MODEL + ACTION + apiKey);

                connection = (HttpURLConnection) url.openConnection();
                currentConnection = connection;

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(6000000);

                JSONObject fileData = new JSONObject()
                        .put("mimeType", "video/mp4")
                        .put("fileUri", videoUrl);
                JSONObject videoPart = new JSONObject().put("fileData", fileData);
                JSONObject textPart = new JSONObject().put("text", textPrompt);
                JSONArray partsArray = new JSONArray().put(videoPart).put(textPart);
                JSONObject contents = new JSONObject().put("parts", partsArray);

                JSONObject safetySetting_harassment = new JSONObject().put("category", "HARM_CATEGORY_HARASSMENT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_hate = new JSONObject().put("category", "HARM_CATEGORY_HATE_SPEECH").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_sex = new JSONObject().put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_danger = new JSONObject().put("category", "HARM_CATEGORY_DANGEROUS_CONTENT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONArray safetySettingsArray = new JSONArray()
                        .put(safetySetting_harassment)
                        .put(safetySetting_hate)
                        .put(safetySetting_sex)
                        .put(safetySetting_danger);

                JSONObject thinkingConfig = new JSONObject()
                        .put("thinkingBudget", 0); // Budget 0 to disable thinking

                JSONObject generationConfig = new JSONObject()
                        .put("thinkingConfig", thinkingConfig);

                JSONObject requestBody = new JSONObject()
                        .put("contents", new JSONArray().put(contents))
                        .put("safetySettings", safetySettingsArray)
                        .put("generationConfig", generationConfig);

                String jsonInputString = requestBody.toString();

                Logger.printDebug(() -> "GeminiUtils (" + GEMINI_MODEL + "): Sending JSON Payload: " + jsonInputString.substring(0, Math.min(jsonInputString.length(), 500)) + "...");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Gemini task cancelled before reading response.");
                }

                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = getBufferedReader(responseCode, connection)) {
                    String responseLine;
                    while ((responseLine = reader.readLine()) != null) {
                        response.append(responseLine.trim());
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Gemini task cancelled while reading response.");
                        }
                    }
                }

                String responseString = response.toString();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    try {
                        if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").length() == 0) {
                            String blockReason = extractBlockReason(jsonResponse);
                            final String finalBlockReason = blockReason != null ? "Content blocked: " + blockReason :
                                    "API response missing 'candidates'. Full Response: " + responseString.substring(0, Math.min(responseString.length(), 500)) + "...";
                            mainThreadHandler.post(() -> callback.onFailure(finalBlockReason));
                            return;
                        }

                        String resultText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        final String finalResult = resultText.trim();
                        mainThreadHandler.post(() -> callback.onSuccess(finalResult));
                    } catch (JSONException jsonEx) {
                        Logger.printException(() -> "Gemini API Response JSON Parsing Error. Full Response: " + responseString.substring(0, Math.min(responseString.length(), 500)) + "...", jsonEx);
                        String blockReason = extractBlockReason(jsonResponse);
                        final String finalError = blockReason != null ? "Content blocked: " + blockReason :
                                "Failed to parse result from API response. Check logs.";
                        mainThreadHandler.post(() -> callback.onFailure(finalError));
                    }
                } else {
                    String errorMessage = "HTTP Error: " + responseCode;
                    String errorMessage1 = " - " + responseString.substring(0, Math.min(responseString.length(), 200)) + "...";
                    try {
                        JSONObject errorResponse = new JSONObject(responseString);
                        if (errorResponse.has("error") && errorResponse.getJSONObject("error").has("message")) {
                            errorMessage = errorResponse.getJSONObject("error").getString("message");
                        } else {
                            errorMessage += errorMessage1;
                        }
                    } catch (Exception jsonEx) {
                        errorMessage += errorMessage1;
                    }
                    Logger.printException(() -> "Gemini API Error (" + responseCode + "). Response: " + responseString);
                    final String finalError = errorMessage;
                    mainThreadHandler.post(() -> callback.onFailure(finalError));
                }

            } catch (java.net.SocketTimeoutException e) {
                Logger.printException(() -> "Gemini API request timed out (" + GEMINI_MODEL + ")", e);
                final String timeoutMsg = "Request timed out after " + (connection != null ? connection.getReadTimeout() / 1000 : "?") + " seconds.";
                mainThreadHandler.post(() -> callback.onFailure(timeoutMsg));
            } catch (InterruptedException e) {
                Logger.printInfo(() -> "Gemini task explicitly cancelled.");
                mainThreadHandler.post(() -> callback.onFailure("Operation cancelled."));
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted() || (e.getCause() instanceof InterruptedException)) {
                    Logger.printInfo(() -> "Gemini task explicitly cancelled (IOException).");
                    mainThreadHandler.post(() -> callback.onFailure("Operation cancelled."));
                } else {
                    Logger.printException(() -> "Gemini API request IO failed (" + GEMINI_MODEL + ")", e);
                    final String ioErrorMsg = e.getMessage() != null ? "Network error: " + e.getMessage() : "Unknown network error";
                    mainThreadHandler.post(() -> callback.onFailure(ioErrorMsg));
                }
            } catch (Exception e) {
                Logger.printException(() -> "Gemini API request failed (" + GEMINI_MODEL + ")", e);
                final String genericErrorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error during request setup";
                mainThreadHandler.post(() -> callback.onFailure(genericErrorMsg));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                currentConnection = null;
                currentTask.compareAndSet(taskBeingRun, null);
            }
        });
        taskRef.set(newTask);

        currentTask.set(newTask);
    }

    /**
     * Attempts to cancel the currently running Gemini API task, if any.
     * It cancels the {@link Future} associated with the task and attempts to disconnect the underlying {@link HttpURLConnection}.
     */
    public static void cancelCurrentTask() {
        Future<?> taskToCancel = currentTask.getAndSet(null);
        if (taskToCancel != null) {
            Logger.printDebug(() -> "Attempting to cancel current Gemini task.");
            taskToCancel.cancel(true);
        }

        HttpURLConnection conn = currentConnection;
        if (conn != null) {
            // Disconnect should ideally be done on a separate thread if it blocks,
            // but for simplicity here, we do it directly.
            try {
                // Execute disconnect on the executor thread to avoid blocking caller
                executor.execute(() -> {
                    try {
                        conn.disconnect();
                        Logger.printDebug(() -> "Disconnected current Gemini connection.");
                    } catch (Exception e) {
                        /* ignore disconnect errors */
                        Logger.printException(() -> "Error disconnecting Gemini connection", e);
                    }
                });
            } catch (Exception e) {
                // Ignore if executor is shutting down etc.
            }
        }
        currentConnection = null;
    }

    /**
     * Gets a {@link BufferedReader} for reading the response from an {@link HttpURLConnection}.
     * Handles both successful (2xx) and error responses by choosing the appropriate input stream
     * ({@link HttpURLConnection#getInputStream()} or {@link HttpURLConnection#getErrorStream()}).
     *
     * @param responseCode The HTTP response code received from the connection.
     * @param connection   The active {@link HttpURLConnection}.
     * @return A {@link BufferedReader} ready to read the response body using UTF-8 encoding.
     * @throws IOException If an I/O error occurs while getting the stream, or if no stream is available for an error code.
     */
    @NotNull
    private static BufferedReader getBufferedReader(int responseCode, HttpURLConnection connection) throws IOException {
        InputStreamReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        } else {
            java.io.InputStream errorStream = connection.getErrorStream();
            if (errorStream == null) {
                // Fallback to input stream if error stream is null but response code indicates error
                // This can happen in some network scenarios or server configurations
                java.io.InputStream inputStream = connection.getInputStream();
                if (inputStream != null) {
                    reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                } else {
                    throw new IOException("HTTP error " + responseCode + " with no error or input stream details.");
                }
            } else {
                reader = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
            }
        }
        return new BufferedReader(reader);
    }

    /**
     * Attempts to extract a block reason or other relevant failure information from a Gemini API JSON response.
     * Checks various fields like `promptFeedback`, `candidates.finishReason`, and `safetyRatings`.
     *
     * @param jsonResponse The parsed JSONObject of the Gemini API response.
     * @return A string describing the block reason or failure, or null if no specific reason is found.
     */
    @Nullable
    private static String extractBlockReason(JSONObject jsonResponse) {
        try {
            // Check top-level promptFeedback first (usually indicates input blocking)
            if (jsonResponse.has("promptFeedback")) {
                JSONObject promptFeedback = jsonResponse.getJSONObject("promptFeedback");
                if (promptFeedback.has("blockReason")) {
                    String reason = promptFeedback.getString("blockReason");
                    String details = "";
                    // blockReasonMessage is often more user-friendly than iterating safetyRatings
                    if (promptFeedback.has("blockReasonMessage")) {
                        details = ": " + promptFeedback.getString("blockReasonMessage");
                    } else if (promptFeedback.has("safetyRatings")) {
                        // Fallback to safetyRatings if blockReasonMessage is missing
                        String safetyDetail = getSafetyBlockDetail(promptFeedback.getJSONArray("safetyRatings"));
                        if (safetyDetail != null) details = " (" + safetyDetail + ")";
                    }
                    return reason + details;
                }
                // Even if no blockReason, check safety ratings in promptFeedback
                if (promptFeedback.has("safetyRatings")) {
                    String safetyDetail = getSafetyBlockDetail(promptFeedback.getJSONArray("safetyRatings"));
                    if (safetyDetail != null) return "SAFETY (" + safetyDetail + ")";
                }
            }

            // Check candidate information (usually indicates output blocking or other finish reasons)
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    // Check finishReason first
                    if (firstCandidate.has("finishReason")) {
                        String finishReason = firstCandidate.getString("finishReason");
                        // Only report non-standard finish reasons as errors/blocks
                        if (!"STOP".equals(finishReason) && !"MAX_TOKENS".equals(finishReason)) {
                            // If safety related, try to get more details
                            if ("SAFETY".equals(finishReason) && firstCandidate.has("safetyRatings")) {
                                String safetyDetail = getSafetyBlockDetail(firstCandidate.getJSONArray("safetyRatings"));
                                if (safetyDetail != null) return "SAFETY (" + safetyDetail + ")";
                            }
                            // Return the finish reason itself if not STOP or MAX_TOKENS
                            return finishReason;
                        }
                    }
                    // If finishReason is STOP/MAX_TOKENS or missing, check safety ratings anyway
                    if (firstCandidate.has("safetyRatings")) {
                        String safetyDetail = getSafetyBlockDetail(firstCandidate.getJSONArray("safetyRatings"));
                        if (safetyDetail != null) return "SAFETY (" + safetyDetail + ")";
                    }
                } else {
                    // Handle case where candidates array is present but empty
                    return "No candidates returned";
                }
            }
        } catch (JSONException e) {
            Logger.printException(() -> "Error extracting block reason", e);
        }
        return null;
    }

    /**
     * Extracts the most severe safety concern details (Category and Probability/Blocked status)
     * from a JSONArray of safety ratings provided by the Gemini API.
     * Prioritizes explicitly blocked ratings, then HIGH probability, then MEDIUM probability.
     *
     * @param safetyRatings The JSONArray containing safety rating objects.
     * @return A string detailing the most severe safety issue found (e.g., "HARM_CATEGORY_HARASSMENT - BLOCKED",
     * "HARM_CATEGORY_HATE_SPEECH - HIGH"), or null if no ratings indicate MEDIUM, HIGH, or BLOCKED status.
     * @throws JSONException If parsing the safety rating objects fails.
     */
    @Nullable
    private static String getSafetyBlockDetail(JSONArray safetyRatings) throws JSONException {
        String mostSevereCategory = null;
        String mostSevereProbability = null;
        boolean blockedFound = false;

        for (int i = 0; i < safetyRatings.length(); i++) {
            JSONObject rating = safetyRatings.getJSONObject(i);
            String category = rating.optString("category", "Unknown Category");
            String probability = rating.optString("probability", "UNKNOWN");

            // If explicitly blocked, report that category immediately
            if (rating.optBoolean("blocked", false)) {
                return category + " - BLOCKED";
            }

            // Track the highest probability non-blocked category (MEDIUM or HIGH)
            if (!blockedFound) {
                if (probability.endsWith("_HIGH")) {
                    // If we find a HIGH probability, use it unless we already found another HIGH
                    if (mostSevereProbability == null || !mostSevereProbability.endsWith("_HIGH")) {
                        mostSevereCategory = category;
                        mostSevereProbability = probability;
                    }
                } else if (probability.equals("MEDIUM")) {
                    // If we find MEDIUM, use it only if we haven't found HIGH or MEDIUM yet
                    if (mostSevereProbability == null || mostSevereProbability.equals("NEGLIGIBLE") || mostSevereProbability.equals("UNKNOWN")) {
                        mostSevereCategory = category;
                        mostSevereProbability = probability;
                    }
                }
                // Ignore LOW, NEGLIGIBLE, UNKNOWN if we already have MEDIUM or HIGH
            }
        }

        // If no block, but found MEDIUM or HIGH probability, return that detail
        if (mostSevereCategory != null && (mostSevereProbability.endsWith("_HIGH") || mostSevereProbability.equals("MEDIUM"))) {
            return mostSevereCategory + " - " + mostSevereProbability;
        }

        // Otherwise, no significant safety issue detected by this logic
        return null;
    }

    /**
     * Gets the display name of the language currently selected in the ReVanced settings,
     * falling back to the system default language, and finally to "English" if needed.
     * The language name is returned in English (e.g., "Spanish", "German").
     *
     * @return The non-null display name of the language in English.
     */
    @NonNull
    public static String getLanguageName() {
        try {
            Locale locale = Settings.REVANCED_LANGUAGE.get().getLocale();
            if (locale != null && !TextUtils.isEmpty(locale.getLanguage())) {
                return locale.getDisplayLanguage(Locale.ENGLISH);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to get language code from settings, using system default.", e);
        }

        // Fallback to system default locale
        try {
            Locale defaultLocale = Locale.getDefault();
            if (!TextUtils.isEmpty(defaultLocale.getLanguage())) {
                return defaultLocale.getDisplayLanguage(Locale.ENGLISH);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to get system default language code, using 'English'.", e);
        }

        // Absolute fallback
        return "English";
    }

    /**
     * Callback interface for Gemini API operations (summary, transcription).
     * Defines methods to handle successful results or failures.
     */
    public interface Callback {
        /**
         * Called when the Gemini API request completes successfully.
         *
         * @param result The generated text (summary or transcription) from the API.
         */
        void onSuccess(String result);

        /**
         * Called when the Gemini API request fails.
         *
         * @param error A message describing the error that occurred.
         */
        void onFailure(String error);
    }
}
