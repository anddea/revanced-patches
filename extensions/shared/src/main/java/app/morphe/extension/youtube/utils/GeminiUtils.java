/*
 * Copyright (C) 2025 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
 */

package app.morphe.extension.youtube.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;
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

public class GeminiUtils {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final String BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String GEMINI_MODEL = "gemini-3-flash-preview";
    private static final String GEMINI_FALLBACK_MODEL = "gemini-2.5-flash";
    private static final String ACTION = ":generateContent?key=";

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Initiates an asynchronous request to the Gemini API to generate a summary for the given video URL.
     *
     * @param videoUrl The publicly accessible URL of the video to summarize.
     * @param apiKey   The Gemini API key.
     * @param callback The {@link Callback} to receive the summary result or an error message.
     */
    public static void getVideoSummary(@NonNull String videoUrl, @NonNull String apiKey, @NonNull Callback callback) {
        String langName = getLanguageName();
        String prompt = "Summarize the key points of this video in " + langName + ". Skip any preamble, intro phrases, or explanations — output only the summary.";
        Logger.printDebug(() -> "GeminiUtils (SUMMARY): Sending Prompt: " + prompt);
        generateContent(videoUrl, apiKey, prompt, GEMINI_MODEL, callback, true);
    }

    /**
     * Initiates an asynchronous request to the Gemini API to generate a transcription for the given video URL.
     *
     * @param videoUrl The publicly accessible URL of the video to transcribe.
     * @param apiKey   The Gemini API key.
     * @param callback The {@link Callback} to receive the transcription result or an error message.
     */
    public static void getVideoTranscription(@NonNull String videoUrl, @NonNull String apiKey, @NonNull Callback callback) {
        String langName = getLanguageName();
        String prompt = "Transcribe this video precisely in " + langName + ", including spoken words, written words in the video and significant sounds. Provide timestamps for each segment in the format [HH:MM:SS.mmm - HH:MM:SS.mmm]: Text. Skip any preamble, intro phrases, or explanations — output only the transcription.";
        Logger.printDebug(() -> "GeminiUtils (TRANSCRIPTION): Sending Prompt: " + prompt);
        generateContent(videoUrl, apiKey, prompt, GEMINI_MODEL, callback, true);
    }

    /**
     * Initiates an asynchronous request to the Gemini API to translate the text content
     * within a Yandex-formatted subtitle JSON string to a specified target language.
     *
     * @param yandexJson     The non-null JSON string containing subtitles in the Yandex format
     *                       (typically an array of objects or an object containing a "subtitles" array,
     *                       where objects have "startMs", "endMs"/"durationMs", and "text" keys).
     *                       While basic JSON validity isn't checked here, grossly invalid input
     *                       might lead to Gemini errors or unexpected results.
     * @param targetLangName The non-null display name (in English, e.g., "Spanish", "German")
     *                       of the language to translate the subtitle text into. This name is used
     *                       directly in the prompt sent to the Gemini API. Use a reliable method
     *                       like {@link java.util.Locale#getDisplayLanguage(Locale)} to obtain this.
     * @param apiKey         The non-null Google AI API key (Gemini API key).
     * @param callback       The non-null {@link Callback} interface instance to receive the
     *                       result. {@link Callback#onSuccess(String)} will be called with the
     *                       translated JSON string, and {@link Callback#onFailure(String)}
     *                       will be called if an error occurs during the API request,
     *                       response parsing, or if the result format is invalid.
     */
    public static void translateYandexJson(
            @NonNull String yandexJson,
            @NonNull String targetLangName,
            @NonNull String apiKey,
            @NonNull Callback callback) {
        String prompt = "Translate ONLY the string values associated with the \"text\" keys within the following JSON subtitle data to " + targetLangName + ". Preserve the exact JSON structure, including all keys (like \"startMs\", \"endMs\", \"durationMs\") and their original numeric values. Output ONLY the fully translated JSON data, without any introductory text, explanations, comments, or markdown formatting (like ```json ... ```).\n\nInput JSON:\n" + yandexJson;

        Logger.printDebug(() -> "GeminiUtils (JSON TRANSLATE): Sending Translation Prompt for target '" + targetLangName + "'.");
        generateContent(null, apiKey, prompt, GEMINI_MODEL, callback, true);
    }

    /**
     * Makes an asynchronous POST request to the Gemini API's generateContent endpoint.
     *
     * @param videoUrl      The publicly accessible URL of the video (nullable).
     * @param apiKey        The Gemini API key.
     * @param textPrompt    The specific text prompt to send.
     * @param model         The Gemini model to use.
     * @param callback      The {@link Callback} to handle the success or failure response.
     * @param allowFallback If true, a failure will trigger a retry with the fallback model.
     */
    private static void generateContent(@Nullable String videoUrl, @NonNull String apiKey, @NonNull String textPrompt, @NonNull String model, @NonNull Callback callback, boolean allowFallback) {
        executor.submit(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(BASE_API_URL + model + ACTION + apiKey);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(6000000);

                JSONObject requestBody = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray partsArray = new JSONArray();

                if (videoUrl != null) {
                    Logger.printDebug(() -> "GeminiUtils: Constructing payload WITH video part.");
                    JSONObject fileData = new JSONObject()
                            .put("mimeType", "video/mp4")
                            .put("fileUri", videoUrl);
                    JSONObject videoPart = new JSONObject().put("fileData", fileData);
                    partsArray.put(videoPart);

                    JSONObject textPart = new JSONObject().put("text", textPrompt);
                    partsArray.put(textPart);
                } else {
                    Logger.printDebug(() -> "GeminiUtils: Constructing payload with ONLY text part.");
                    JSONObject textPartOnly = new JSONObject().put("text", textPrompt);
                    partsArray.put(textPartOnly);
                }

                content.put("parts", partsArray);
                contentsArray.put(content);
                requestBody.put("contents", contentsArray);

                /* Uncomment to enable safety settings
                JSONObject safetySetting_harassment = new JSONObject().put("category", "HARM_CATEGORY_HARASSMENT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_hate = new JSONObject().put("category", "HARM_CATEGORY_HATE_SPEECH").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_sex = new JSONObject().put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONObject safetySetting_danger = new JSONObject().put("category", "HARM_CATEGORY_DANGEROUS_CONTENT").put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                JSONArray safetySettingsArray = new JSONArray()
                        .put(safetySetting_harassment)
                        .put(safetySetting_hate)
                        .put(safetySetting_sex)
                        .put(safetySetting_danger);
                requestBody.put("safetySettings", safetySettingsArray);
                */

                // Configure generation options based on the model
                JSONObject generationConfig = new JSONObject();
                JSONObject thinkingConfig;

                if (model.equals(GEMINI_MODEL)) {
                    // Gemini 3 (Thinking Model) Configuration
                    // Gemini 3 does not support turning thinking fully "off", but "minimal" acts as the disabled state.
                    thinkingConfig = new JSONObject()
                            .put("thinkingLevel", "minimal")
                            .put("includeThoughts", false);
                } else {
                    // Fallback Model (Gemini 2.5 Flash) Configuration
                    thinkingConfig = new JSONObject().put("thinkingBudget", 0);
                }
                generationConfig.put("thinkingConfig", thinkingConfig);

                if (generationConfig.length() > 0) {
                    requestBody.put("generationConfig", generationConfig);
                }

                String jsonInputString = requestBody.toString();

                String logIdentifier = (videoUrl != null) ? "VIDEO" : "TEXT/JSON";
                Logger.printDebug(() -> "GeminiUtils (" + logIdentifier + " - " + model + "): Sending Payload. Prompt starts: " + textPrompt.substring(0, Math.min(textPrompt.length(), 200)) + "...");

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
                                    "API response missing valid candidates. Full Response: " + responseString.substring(0, Math.min(responseString.length(), 500)) + "...";
                            Logger.printException(() -> "Gemini API Error: " + finalBlockReason);
                            mainThreadHandler.post(() -> callback.onFailure(finalBlockReason));
                            return;
                        }

                        String resultText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        final String finalResult = sanitizeJsonOutput(resultText);
                        Logger.printDebug(() -> "Gemini RAW result (Sanitized): " + finalResult.substring(0, Math.min(finalResult.length(), 300)) + "...");

                        if (videoUrl == null) {
                            boolean looksLikeJson = finalResult.startsWith("[") || finalResult.startsWith("{");
                            if (!looksLikeJson) {
                                Logger.printInfo(() -> "Gemini JSON translation result doesn't look like valid JSON!");
                                Logger.printInfo(() -> "Start of content: " + finalResult.substring(0, Math.min(finalResult.length(), 50)));
                                mainThreadHandler.post(() -> callback.onFailure("Translation result format error. Expected JSON."));
                                return;
                            }
                        }

                        Logger.printDebug(() -> "Gemini RAW result received: " + finalResult.substring(0, Math.min(finalResult.length(), 300)) + "...");

                        if (videoUrl == null) {
                            boolean looksLikeJson = finalResult.startsWith("[") || finalResult.startsWith("{");
                            if (!looksLikeJson) {
                                Logger.printInfo(() -> "Gemini JSON translation result doesn't look like valid JSON!");
                                mainThreadHandler.post(() -> callback.onFailure("Translation result format error. Expected JSON."));
                                return;
                            }
                        }

                        mainThreadHandler.post(() -> callback.onSuccess(finalResult));
                    } catch (JSONException jsonEx) {
                        Logger.printException(() -> "Gemini API Response JSON Parsing Error. Full Response: " + responseString.substring(0, Math.min(responseString.length(), 500)) + "...", jsonEx);
                        String blockReason = extractBlockReason(jsonResponse);
                        final String finalError = blockReason != null ? "Content blocked: " + blockReason :
                                "Failed to parse result from API response. Check logs.";
                        mainThreadHandler.post(() -> callback.onFailure(finalError));
                    }
                } else {
                    if (allowFallback) {
                        Logger.printDebug(() -> "GeminiUtils: Primary model " + model + " failed (HTTP " + responseCode + "). Fallback to " + GEMINI_FALLBACK_MODEL);
                        generateContent(videoUrl, apiKey, textPrompt, GEMINI_FALLBACK_MODEL, callback, false);
                        return;
                    }

                    String errorMessage = "HTTP Error: " + responseCode;
                    String errorDetails = " - " + responseString.substring(0, Math.min(responseString.length(), 200)) + "...";
                    try {
                        JSONObject errorResponse = new JSONObject(responseString);
                        if (errorResponse.has("error") && errorResponse.getJSONObject("error").has("message")) {
                            errorMessage = errorResponse.getJSONObject("error").getString("message");
                        } else {
                            errorMessage += errorDetails;
                        }
                    } catch (Exception jsonEx) {
                        errorMessage += errorDetails;
                    }
                    Logger.printException(() -> "Gemini API Error (" + responseCode + "). Response: " + responseString);
                    final String finalError = errorMessage;
                    mainThreadHandler.post(() -> callback.onFailure(finalError));
                }

            } catch (java.net.SocketTimeoutException e) {
                if (allowFallback) {
                    Logger.printDebug(() -> "GeminiUtils: Primary model " + model + " timed out. Fallback to " + GEMINI_FALLBACK_MODEL);
                    generateContent(videoUrl, apiKey, textPrompt, GEMINI_FALLBACK_MODEL, callback, false);
                    return;
                }

                Logger.printException(() -> "Gemini API request timed out (" + model + ")", e);
                final String timeoutMsg = "Request timed out after " + (connection != null ? connection.getReadTimeout() / 1000 : "?") + " seconds.";
                mainThreadHandler.post(() -> callback.onFailure(timeoutMsg));
            } catch (InterruptedException e) {
                Logger.printInfo(() -> "Gemini task explicitly cancelled.");
                mainThreadHandler.post(() -> callback.onFailure("Operation cancelled."));
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (allowFallback) {
                    Logger.printDebug(() -> "GeminiUtils: Primary model " + model + " network failed. Fallback to " + GEMINI_FALLBACK_MODEL);
                    generateContent(videoUrl, apiKey, textPrompt, GEMINI_FALLBACK_MODEL, callback, false);
                    return;
                }

                Logger.printException(() -> "Gemini API request IO failed (" + model + ")", e);
                final String ioErrorMsg = e.getMessage() != null ? "Network error: " + e.getMessage() : "Unknown network error";
                mainThreadHandler.post(() -> callback.onFailure(ioErrorMsg));
            } catch (Exception e) {
                if (allowFallback) {
                    Logger.printDebug(() -> "GeminiUtils: Primary model " + model + " failed with exception. Fallback to " + GEMINI_FALLBACK_MODEL);
                    generateContent(videoUrl, apiKey, textPrompt, GEMINI_FALLBACK_MODEL, callback, false);
                    return;
                }

                Logger.printException(() -> "Gemini API request failed (" + model + ")", e);
                final String genericErrorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error during request setup";
                mainThreadHandler.post(() -> callback.onFailure(genericErrorMsg));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
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
                java.io.InputStream inputStream = null;
                try {inputStream = connection.getInputStream();} catch (IOException ignored) {}
                if (inputStream != null) {
                    Logger.printInfo(() -> "HTTP error " + responseCode + " but errorStream was null. Reading from inputStream instead.");
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
                    String details = promptFeedback.optString("blockReasonMessage", "");
                    if (details.isEmpty() && promptFeedback.has("safetyRatings")) {
                        String safetyDetail = getSafetyBlockDetail(promptFeedback.getJSONArray("safetyRatings"));
                        if (safetyDetail != null) details = safetyDetail;
                    }
                    return reason + (details.isEmpty() ? "" : ": " + details);
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
                } else {return "No candidates returned";}
            }
        } catch (JSONException e) {Logger.printException(() -> "Error extracting block reason", e);}
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
        int severityLevel = 0; // 0: OK, 1: Medium, 2: High, 3: Blocked

        for (int i = 0; i < safetyRatings.length(); i++) {
            JSONObject rating = safetyRatings.getJSONObject(i);
            String category = rating.optString("category", "Unknown Category");
            String probability = rating.optString("probability", "UNKNOWN");
            boolean blocked = rating.optBoolean("blocked", false);

            int currentSeverity = 0;
            if (blocked) currentSeverity = 3;
            else if (probability.endsWith("HIGH")) currentSeverity = 2;
            else if (probability.equals("MEDIUM")) currentSeverity = 1;

            if (currentSeverity > severityLevel) {
                severityLevel = currentSeverity;
                mostSevereCategory = category;
                mostSevereProbability = blocked ? "BLOCKED" : probability;
                if (blocked) break; // Found blocked, highest severity
            }
        }

        if (severityLevel >= 1) {return mostSevereCategory + " - " + mostSevereProbability;}
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
            Logger.printException(() -> "Failed to get language locale from settings, using system default.", e);
        }

        // Fallback to system default locale
        try {
            Locale defaultLocale = Locale.getDefault();
            if (!TextUtils.isEmpty(defaultLocale.getLanguage())) {
                return defaultLocale.getDisplayLanguage(Locale.ENGLISH);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to get system default language name, using 'English'.", e);
        }

        // Absolute fallback
        return "English";
    }

    /**
     * Cleans the Gemini response to remove potential Markdown formatting.
     */
    private static String sanitizeJsonOutput(String text) {
        if (text == null) return "";
        text = text.trim();

        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
        }
        if (text.endsWith("```")) {
            text = text.replaceAll("\\s*```$", "");
        }
        return text.trim();
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
