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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

public class GeminiUtils {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final String BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String[] GEMINI_MODELS = {
            "gemini-3.1-flash-lite-preview",
            "gemini-3-flash-preview",
            "gemini-3.1-pro-preview",
            "gemini-3-pro-preview",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
    };
    private static final String GENERATE_ACTION = ":generateContent?key=";
    private static final String STREAM_ACTION = ":streamGenerateContent?alt=sse&key=";

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Initiates an asynchronous request to the Gemini API to generate a summary for the given video URL.
     *
     * @param videoUrl The publicly accessible URL of the video to summarize.
     * @param apiKeys  Gemini API keys, checked top-to-bottom for each model.
     * @param callback The {@link Callback} to receive the summary result or an error message.
     * @return The running request future.
     */
    @Nullable
    public static Future<?> getVideoSummary(
            @NonNull String videoUrl,
            @NonNull List<String> apiKeys,
            @NonNull Callback callback
    ) {
        String promptForGemini25 = getPrompt();
        String prompt = promptForGemini25 + " Include timestamp references at the end of each summary section in this format: [MM:SS – MM:SS], or if the video is an hour long or longer: [HH:MM:SS – HH:MM:SS]. Timestamps must be accurate (hours:minutes:seconds) and double-checked. Please verify the correct format is used.";
        Logger.printDebug(() -> "GeminiUtils (SUMMARY): Sending Prompt (Gemini 3.x base): " + prompt);
        return executeRequest(RequestSpec.forPrompt(videoUrl, prompt, promptForGemini25, true, false), apiKeys, callback);
    }

    /**
     * Initiates an asynchronous request to the Gemini API to generate a transcription for the given video URL.
     *
     * @param videoUrl The publicly accessible URL of the video to transcribe.
     * @param apiKeys  Gemini API keys, checked top-to-bottom for each model.
     * @param callback The {@link Callback} to receive the transcription result or an error message.
     * @return The running request future.
     */
    @Nullable
    public static Future<?> getVideoTranscription(
            @NonNull String videoUrl,
            @NonNull List<String> apiKeys,
            @NonNull Callback callback
    ) {
        String langName = getLanguageName();
        String prompt = "Transcribe this video precisely in " + langName + ", including spoken words, written words in the video and significant sounds. Provide timestamps for each segment in the format [HH:MM:SS.mmm - HH:MM:SS.mmm]: Text. Skip any preamble, intro phrases, or explanations - output only the transcription.";
        Logger.printDebug(() -> "GeminiUtils (TRANSCRIPTION): Sending Prompt: " + prompt);
        return executeRequest(RequestSpec.forPrompt(videoUrl, prompt, null, false, false), apiKeys, callback);
    }

    /**
     * Initiates an asynchronous request to the Gemini API to translate Yandex subtitle JSON.
     *
     * @param yandexJson     The Yandex-formatted subtitle JSON.
     * @param targetLangName The target language display name.
     * @param apiKeys        Gemini API keys, checked top-to-bottom for each model.
     * @param callback       Callback receiving the translated JSON or an error.
     * @return The running request future.
     */
    @Nullable
    public static Future<?> translateYandexJson(
            @NonNull String yandexJson,
            @NonNull String targetLangName,
            @NonNull List<String> apiKeys,
            @NonNull Callback callback
    ) {
        String prompt = "Translate ONLY the string values associated with the \"text\" keys within the following JSON subtitle data to " + targetLangName + ". Preserve the exact JSON structure, including all keys (like \"startMs\", \"endMs\", \"durationMs\") and their original numeric values. Output ONLY the fully translated JSON data, without any introductory text, explanations, comments, or markdown formatting (like ```json ... ```).\n\nInput JSON:\n" + yandexJson;

        Logger.printDebug(() -> "GeminiUtils (JSON TRANSLATE): Sending Translation Prompt for target '" + targetLangName + "'.");
        return executeRequest(RequestSpec.forPrompt(null, prompt, null, false, true), apiKeys, callback);
    }

    /**
     * Initiates a streamed multi-turn follow-up chat about a summarized video.
     *
     * @param videoUrl The video URL used as the multimodal context.
     * @param summary  The generated summary that seeds the chat.
     * @param history  Conversation history including the latest user question.
     * @param apiKeys  Gemini API keys, checked top-to-bottom for each model.
     * @param callback Callback receiving streamed chunks, the final answer, or an error.
     * @return The running request future.
     */
    @Nullable
    public static Future<?> chatWithVideo(
            @NonNull String videoUrl,
            @NonNull String summary,
            @NonNull List<ChatMessage> history,
            @NonNull List<String> apiKeys,
            @NonNull Callback callback
    ) {
        return executeRequest(RequestSpec.forChat(videoUrl, summary, history), apiKeys, callback);
    }

    @NonNull
    public static List<String> parseApiKeys(@Nullable String rawValue) {
        LinkedHashSet<String> apiKeys = new LinkedHashSet<>();
        if (rawValue == null) {
            return new ArrayList<>();
        }

        for (String candidate : rawValue.split("[\\r\\n,]+")) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                apiKeys.add(trimmed);
            }
        }
        return new ArrayList<>(apiKeys);
    }

    @NonNull
    public static String maskApiKey(@Nullable String apiKey) {
        if (TextUtils.isEmpty(apiKey)) {
            return "(empty)";
        }

        String trimmed = apiKey.trim();
        int suffixLength = Math.min(6, trimmed.length());
        return "***" + trimmed.substring(trimmed.length() - suffixLength);
    }

    @Nullable
    private static Future<?> executeRequest(
            @NonNull RequestSpec requestSpec,
            @NonNull List<String> apiKeys,
            @NonNull Callback callback
    ) {
        List<String> normalizedApiKeys = sanitizeApiKeys(apiKeys);
        if (normalizedApiKeys.isEmpty()) {
            postFailure(callback, "No Gemini API keys configured.");
            return null;
        }

        return executor.submit(() -> {
            String lastError = "Unknown Gemini request failure.";

            for (int modelIndex = 0; modelIndex < GEMINI_MODELS.length; modelIndex++) {
                String model = GEMINI_MODELS[modelIndex];

                for (int apiKeyIndex = 0; apiKeyIndex < normalizedApiKeys.size(); apiKeyIndex++) {
                    String apiKey = normalizedApiKeys.get(apiKeyIndex);
                    AttemptState attempt = new AttemptState(
                            model,
                            modelIndex,
                            GEMINI_MODELS.length,
                            apiKey,
                            apiKeyIndex,
                            normalizedApiKeys.size()
                    );

                    AttemptResult result = performAttempt(requestSpec, attempt, callback);
                    switch (result.status) {
                        case SUCCESS:
                            assert result.resultText != null;
                            postSuccess(callback, result.resultText, attempt.model);
                            return;

                        case RETRY:
                            lastError = result.errorMessage;
                            logRetry(attempt, normalizedApiKeys, modelIndex, result.errorMessage);
                            continue;

                        case FAILURE:
                            postFailure(callback, result.errorMessage);
                            return;

                        case CANCELLED:
                            postFailure(callback, "Operation cancelled.");
                            Thread.currentThread().interrupt();
                            return;
                    }
                }
            }

            postFailure(callback, lastError);
        });
    }

    @NonNull
    private static AttemptResult performAttempt(
            @NonNull RequestSpec requestSpec,
            @NonNull AttemptState attempt,
            @NonNull Callback callback
    ) {
        HttpURLConnection connection = null;

        try {
            Logger.printInfo(() -> "GeminiUtils: Attempting " + describeAttempt(attempt));

            String effectivePrompt = resolvePromptForModel(attempt.model, requestSpec.promptText, requestSpec.promptForGemini25);
            if (requestSpec.promptText != null && !TextUtils.equals(effectivePrompt, requestSpec.promptText)) {
                Logger.printDebug(() -> "GeminiUtils: Using model-specific prompt for " + describeAttempt(attempt) + ".");
            }

            URL url = new URL(BASE_API_URL + attempt.model + (requestSpec.streaming ? STREAM_ACTION : GENERATE_ACTION) + attempt.apiKey);
            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection, requestSpec.streaming);

            JSONObject requestBody = buildRequestBody(requestSpec, effectivePrompt, attempt.model);
            writeRequestBody(connection, requestBody);

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Gemini task cancelled before reading response.");
            }

            int responseCode = connection.getResponseCode();
            if (requestSpec.streaming) {
                return processStreamingResponse(responseCode, connection, attempt, callback);
            }
            return processStandardResponse(responseCode, connection, requestSpec, attempt);
        } catch (java.net.SocketTimeoutException e) {
            Logger.printException(() -> "Gemini API request timed out (" + describeAttempt(attempt) + ")", e);
            return AttemptResult.retry("Request timed out after " + (connection != null ? connection.getReadTimeout() / 1000 : "?") + " seconds.");
        } catch (InterruptedException e) {
            Logger.printInfo(() -> "Gemini task explicitly cancelled.");
            Thread.currentThread().interrupt();
            return AttemptResult.cancelled();
        } catch (IOException e) {
            Logger.printException(() -> "Gemini API request IO failed (" + describeAttempt(attempt) + ")", e);
            return AttemptResult.retry(e.getMessage() != null ? "Network error: " + e.getMessage() : "Unknown network error");
        } catch (Exception e) {
            Logger.printException(() -> "Gemini API request failed (" + describeAttempt(attempt) + ")", e);
            return AttemptResult.retry(e.getMessage() != null ? e.getMessage() : "Unknown error during request setup");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private static AttemptResult processStandardResponse(
            int responseCode,
            @NonNull HttpURLConnection connection,
            @NonNull RequestSpec requestSpec,
            @NonNull AttemptState attempt
    ) throws IOException, JSONException, InterruptedException {
        String responseString = readResponseBody(responseCode, connection);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorMessage = parseErrorMessage(responseCode, responseString);
            Logger.printException(() -> "Gemini API Error (" + describeAttempt(attempt) + ", HTTP " + responseCode + "). Response: " + responseString);
            return AttemptResult.retry(errorMessage);
        }

        Logger.printInfo(() -> "GeminiUtils: Model succeeded: " + describeAttempt(attempt) + " (HTTP 200)");
        JSONObject jsonResponse = new JSONObject(responseString);

        if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").length() == 0) {
            String blockReason = extractBlockReason(jsonResponse);
            String finalError = blockReason != null
                    ? "Content blocked: " + blockReason
                    : "API response missing valid candidates. Full Response: " + responseString.substring(0, Math.min(responseString.length(), 500)) + "...";
            Logger.printException(() -> "Gemini API Error (" + describeAttempt(attempt) + "): " + finalError);
            return AttemptResult.retry(finalError);
        }

        String resultText = extractTextFromResponse(jsonResponse);
        String finalResult = requestSpec.expectJsonResponse ? sanitizeJsonOutput(resultText) : resultText;
        Logger.printDebug(() -> "Gemini RAW result received (" + describeAttempt(attempt) + "): " + finalResult.substring(0, Math.min(finalResult.length(), 300)) + "...");

        if (requestSpec.expectJsonResponse) {
            boolean looksLikeJson = finalResult.startsWith("[") || finalResult.startsWith("{");
            if (!looksLikeJson) {
                Logger.printInfo(() -> "Gemini JSON translation result doesn't look like valid JSON! (" + describeAttempt(attempt) + ")");
                return AttemptResult.retry("Translation result format error. Expected JSON.");
            }
        }

        return AttemptResult.success(finalResult);
    }

    @NonNull
    private static AttemptResult processStreamingResponse(
            int responseCode,
            @NonNull HttpURLConnection connection,
            @NonNull AttemptState attempt,
            @NonNull Callback callback
    ) {
        boolean emittedPartial = false;
        StringBuilder accumulatedText = new StringBuilder();
        String lastFailureMessage = "Stream ended before returning text.";

        try {
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String responseString = readResponseBody(responseCode, connection);
                String errorMessage = parseErrorMessage(responseCode, responseString);
                Logger.printException(() -> "Gemini API Error (" + describeAttempt(attempt) + ", HTTP " + responseCode + "). Response: " + responseString);
                return AttemptResult.retry(errorMessage);
            }

            Logger.printInfo(() -> "GeminiUtils: Model succeeded: " + describeAttempt(attempt) + " (HTTP 200 stream)");
            try (BufferedReader reader = getBufferedReader(responseCode, connection)) {
                String line;
                StringBuilder eventData = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Gemini task cancelled while reading streamed response.");
                    }

                    if (line.isEmpty()) {
                        if (eventData.length() > 0) {
                            StreamEventResult eventResult = processStreamEvent(eventData.toString(), accumulatedText, attempt, callback);
                            emittedPartial |= eventResult.emittedPartial;
                            if (eventResult.failureMessage != null) {
                                lastFailureMessage = eventResult.failureMessage;
                            }
                            eventData.setLength(0);
                        }
                        continue;
                    }

                    if (line.startsWith("data:")) {
                        eventData.append(line.substring(5).trim());
                    }
                }

                if (eventData.length() > 0) {
                    StreamEventResult eventResult = processStreamEvent(eventData.toString(), accumulatedText, attempt, callback);
                    emittedPartial |= eventResult.emittedPartial;
                    if (eventResult.failureMessage != null) {
                        lastFailureMessage = eventResult.failureMessage;
                    }
                }
            }

            if (accumulatedText.length() > 0) {
                return AttemptResult.success(accumulatedText.toString());
            }
            return AttemptResult.retry(lastFailureMessage);
        } catch (InterruptedException e) {
            Logger.printInfo(() -> "Gemini task explicitly cancelled.");
            Thread.currentThread().interrupt();
            return AttemptResult.cancelled();
        } catch (IOException e) {
            Logger.printException(() -> "Gemini streamed request IO failed (" + describeAttempt(attempt) + ")", e);
            if (emittedPartial) {
                return AttemptResult.failure();
            }
            return AttemptResult.retry(e.getMessage() != null ? "Network error: " + e.getMessage() : "Unknown network error");
        } catch (JSONException e) {
            Logger.printException(() -> "Gemini streamed response parsing failed (" + describeAttempt(attempt) + ")", e);
            if (emittedPartial) {
                return AttemptResult.failure();
            }
            return AttemptResult.retry("Failed to parse streamed response.");
        }
    }

    @NonNull
    private static StreamEventResult processStreamEvent(
            @NonNull String eventData,
            @NonNull StringBuilder accumulatedText,
            @NonNull AttemptState attempt,
            @NonNull Callback callback
    ) throws JSONException {
        if (eventData.isEmpty() || "[DONE]".equals(eventData)) {
            return StreamEventResult.empty();
        }

        JSONObject jsonResponse = new JSONObject(eventData);
        if (jsonResponse.has("error")) {
            JSONObject errorObject = jsonResponse.getJSONObject("error");
            return StreamEventResult.failure(errorObject.optString("message", "Unknown Gemini stream error"));
        }

        JSONArray candidates = jsonResponse.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            String blockReason = extractBlockReason(jsonResponse);
            return StreamEventResult.failure(blockReason != null ? "Content blocked: " + blockReason : "API response missing valid streamed candidates.");
        }

        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.optJSONObject("content");
        StringBuilder deltaText = new StringBuilder();
        if (content != null) {
            JSONArray parts = content.optJSONArray("parts");
            if (parts != null) {
                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part != null) {
                        String text = part.optString("text", "");
                        if (!text.isEmpty()) {
                            deltaText.append(text);
                        }
                    }
                }
            }
        }

        if (deltaText.length() > 0) {
            accumulatedText.append(deltaText);
            postPartial(callback, deltaText.toString(), accumulatedText.toString(), attempt.model);
            return StreamEventResult.partial();
        }

        if (firstCandidate.has("finishReason")) {
            String finishReason = firstCandidate.optString("finishReason", "");
            if (!TextUtils.isEmpty(finishReason) && !"STOP".equals(finishReason) && !"MAX_TOKENS".equals(finishReason)) {
                String blockReason = extractBlockReason(jsonResponse);
                return StreamEventResult.failure(blockReason != null ? "Content blocked: " + blockReason : finishReason);
            }
        }

        return StreamEventResult.empty();
    }

    @NonNull
    private static JSONObject buildRequestBody(
            @NonNull RequestSpec requestSpec,
            @Nullable String effectivePrompt,
            @NonNull String model
    ) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", buildContentsArray(requestSpec, effectivePrompt));

        JSONObject generationConfig = new JSONObject();
        JSONObject thinkingConfig;
        if (model.startsWith("gemini-3")) {
            thinkingConfig = new JSONObject()
                    .put("thinkingLevel", "minimal")
                    .put("includeThoughts", false);
        } else {
            thinkingConfig = new JSONObject().put("thinkingBudget", 0);
        }
        generationConfig.put("thinkingConfig", thinkingConfig);
        requestBody.put("generationConfig", generationConfig);
        return requestBody;
    }

    @NonNull
    private static JSONArray buildContentsArray(
            @NonNull RequestSpec requestSpec,
            @Nullable String effectivePrompt
    ) throws JSONException {
        JSONArray contentsArray = new JSONArray();

        if (requestSpec.isChatRequest()) {
            JSONObject contextContent = new JSONObject();
            contextContent.put("role", "user");
            JSONArray contextParts = new JSONArray();
            contextParts.put(new JSONObject().put("fileData", new JSONObject()
                    .put("mimeType", "video/mp4")
                    .put("fileUri", requestSpec.videoUrl)));
            contextParts.put(new JSONObject().put("text", requestSpec.chatContextPrompt));
            contextContent.put("parts", contextParts);
            contentsArray.put(contextContent);

            for (ChatMessage message : requestSpec.history) {
                contentsArray.put(new JSONObject()
                        .put("role", message.role)
                        .put("parts", new JSONArray().put(new JSONObject().put("text", message.text))));
            }
            return contentsArray;
        }

        JSONObject content = new JSONObject();
        JSONArray partsArray = new JSONArray();

        if (requestSpec.videoUrl != null) {
            Logger.printDebug(() -> "GeminiUtils: Constructing payload WITH video part.");
            partsArray.put(new JSONObject().put("fileData", new JSONObject()
                    .put("mimeType", "video/mp4")
                    .put("fileUri", requestSpec.videoUrl)));
        } else {
            Logger.printDebug(() -> "GeminiUtils: Constructing payload with ONLY text part.");
        }

        if (!TextUtils.isEmpty(effectivePrompt)) {
            partsArray.put(new JSONObject().put("text", effectivePrompt));
        }

        content.put("parts", partsArray);
        contentsArray.put(content);
        return contentsArray;
    }

    private static void configureConnection(@NonNull HttpURLConnection connection, boolean streaming) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", streaming ? "text/event-stream" : "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(6000000);
    }

    private static void writeRequestBody(@NonNull HttpURLConnection connection, @NonNull JSONObject requestBody) throws IOException {
        String jsonInputString = requestBody.toString();
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    @NonNull
    private static String readResponseBody(int responseCode, @NonNull HttpURLConnection connection) throws IOException, InterruptedException {
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
        return response.toString();
    }

    private static void logRetry(
            @NonNull AttemptState attempt,
            @NonNull List<String> apiKeys,
            int modelIndex,
            @NonNull String reason
    ) {
        boolean hasAnotherKey = attempt.apiKeyIndex + 1 < apiKeys.size();
        boolean hasAnotherModel = modelIndex + 1 < GEMINI_MODELS.length;

        if (!hasAnotherKey && !hasAnotherModel) {
            Logger.printDebug(() -> "GeminiUtils: No fallback left after " + describeAttempt(attempt) + " (" + reason + ").");
            return;
        }

        String nextAttemptDescription;
        AttemptState nextAttempt;
        if (hasAnotherKey) {
            nextAttempt = new AttemptState(
                    attempt.model,
                    attempt.modelIndex,
                    attempt.modelCount,
                    apiKeys.get(attempt.apiKeyIndex + 1),
                    attempt.apiKeyIndex + 1,
                    apiKeys.size()
            );
        } else {
            nextAttempt = new AttemptState(
                    GEMINI_MODELS[modelIndex + 1],
                    modelIndex + 1,
                    GEMINI_MODELS.length,
                    apiKeys.get(0),
                    0,
                    apiKeys.size()
            );
        }
        nextAttemptDescription = describeAttempt(nextAttempt);

        Logger.printDebug(() -> "GeminiUtils: " + describeAttempt(attempt) + " failed (" + reason + "). Falling back to " + nextAttemptDescription + ".");
    }

    @NonNull
    private static String parseErrorMessage(int responseCode, @NonNull String responseString) {
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
        return errorMessage;
    }

    @NonNull
    private static String extractTextFromResponse(@NonNull JSONObject jsonResponse) throws JSONException {
        JSONArray parts = jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (part.has("text")) {
                result.append(part.getString("text"));
            }
        }
        return result.toString();
    }

    @NonNull
    private static List<String> sanitizeApiKeys(@NonNull List<String> apiKeys) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String apiKey : apiKeys) {
            if (!TextUtils.isEmpty(apiKey)) {
                String trimmed = apiKey.trim();
                if (!trimmed.isEmpty()) {
                    sanitized.add(trimmed);
                }
            }
        }
        return new ArrayList<>(sanitized);
    }

    @NonNull
    private static String describeAttempt(@NonNull AttemptState attempt) {
        return "model [" + (attempt.modelIndex + 1) + "/" + attempt.modelCount + "] " + attempt.model
                + ", key [" + (attempt.apiKeyIndex + 1) + "/" + attempt.apiKeyCount + "] " + maskApiKey(attempt.apiKey);
    }

    private static void postSuccess(@NonNull Callback callback, @NonNull String result, @Nullable String model) {
        mainThreadHandler.post(() -> callback.onSuccessWithModel(result, model));
    }

    private static void postFailure(@NonNull Callback callback, @NonNull String error) {
        mainThreadHandler.post(() -> callback.onFailure(error));
    }

    private static void postPartial(
            @NonNull Callback callback,
            @NonNull String partialText,
            @NonNull String accumulatedText,
            @Nullable String model
    ) {
        mainThreadHandler.post(() -> callback.onPartial(partialText, accumulatedText, model));
    }

    @NonNull
    private static String resolvePromptForModel(
            @NonNull String model,
            @Nullable String defaultPrompt,
            @Nullable String promptForGemini25
    ) {
        if (TextUtils.isEmpty(defaultPrompt)) {
            return "";
        }
        if (model.startsWith("gemini-2.5") && !TextUtils.isEmpty(promptForGemini25)) {
            return promptForGemini25;
        }
        return defaultPrompt;
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
                try {
                    inputStream = connection.getInputStream();
                } catch (IOException ignored) {
                }
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
            if (jsonResponse.has("promptFeedback")) {
                JSONObject promptFeedback = jsonResponse.getJSONObject("promptFeedback");
                if (promptFeedback.has("blockReason")) {
                    String reason = promptFeedback.getString("blockReason");
                    String details = promptFeedback.optString("blockReasonMessage", "");
                    if (details.isEmpty() && promptFeedback.has("safetyRatings")) {
                        String safetyDetail = getSafetyBlockDetail(promptFeedback.getJSONArray("safetyRatings"));
                        if (safetyDetail != null) {
                            details = safetyDetail;
                        }
                    }
                    return reason + (details.isEmpty() ? "" : ": " + details);
                }
                if (promptFeedback.has("safetyRatings")) {
                    String safetyDetail = getSafetyBlockDetail(promptFeedback.getJSONArray("safetyRatings"));
                    if (safetyDetail != null) {
                        return "SAFETY (" + safetyDetail + ")";
                    }
                }
            }

            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("finishReason")) {
                        String finishReason = firstCandidate.getString("finishReason");
                        if (!"STOP".equals(finishReason) && !"MAX_TOKENS".equals(finishReason)) {
                            if ("SAFETY".equals(finishReason) && firstCandidate.has("safetyRatings")) {
                                String safetyDetail = getSafetyBlockDetail(firstCandidate.getJSONArray("safetyRatings"));
                                if (safetyDetail != null) {
                                    return "SAFETY (" + safetyDetail + ")";
                                }
                            }
                            return finishReason;
                        }
                    }
                    if (firstCandidate.has("safetyRatings")) {
                        String safetyDetail = getSafetyBlockDetail(firstCandidate.getJSONArray("safetyRatings"));
                        if (safetyDetail != null) {
                            return "SAFETY (" + safetyDetail + ")";
                        }
                    }
                } else {
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
     * @return A string detailing the most severe safety issue found, or null if no concerning rating exists.
     * @throws JSONException If parsing the safety rating objects fails.
     */
    @Nullable
    private static String getSafetyBlockDetail(JSONArray safetyRatings) throws JSONException {
        String mostSevereCategory = null;
        String mostSevereProbability = null;
        int severityLevel = 0;

        for (int i = 0; i < safetyRatings.length(); i++) {
            JSONObject rating = safetyRatings.getJSONObject(i);
            String category = rating.optString("category", "Unknown Category");
            String probability = rating.optString("probability", "UNKNOWN");
            boolean blocked = rating.optBoolean("blocked", false);

            int currentSeverity = 0;
            if (blocked) {
                currentSeverity = 3;
            } else if (probability.endsWith("HIGH")) {
                currentSeverity = 2;
            } else if ("MEDIUM".equals(probability)) {
                currentSeverity = 1;
            }

            if (currentSeverity > severityLevel) {
                severityLevel = currentSeverity;
                mostSevereCategory = category;
                mostSevereProbability = blocked ? "BLOCKED" : probability;
                if (blocked) {
                    break;
                }
            }
        }

        if (severityLevel >= 1) {
            return mostSevereCategory + " - " + mostSevereProbability;
        }
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

        try {
            Locale defaultLocale = Locale.getDefault();
            if (!TextUtils.isEmpty(defaultLocale.getLanguage())) {
                return defaultLocale.getDisplayLanguage(Locale.ENGLISH);
            }
        } catch (Exception e) {
            Logger.printException(() -> "Failed to get system default language name, using 'English'.", e);
        }

        return "English";
    }

    /**
     * Cleans the Gemini response to remove potential Markdown formatting.
     */
    private static String sanitizeJsonOutput(String text) {
        if (text == null) {
            return "";
        }
        text = text.trim();

        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
        }
        if (text.endsWith("```")) {
            text = text.replaceAll("\\s*```$", "");
        }
        return text.trim();
    }

    public record ChatMessage(@NonNull String role, @NonNull String text) {
    }

    private record RequestSpec(@Nullable String videoUrl, @Nullable String promptText,
                               @Nullable String promptForGemini25, boolean streaming,
                               boolean expectJsonResponse, @Nullable String chatContextPrompt,
                               @NonNull List<ChatMessage> history) {

        @NonNull
            static RequestSpec forPrompt(
                    @Nullable String videoUrl,
                    @NonNull String promptText,
                    @Nullable String promptForGemini25,
                    boolean streaming,
                    boolean expectJsonResponse
            ) {
                return new RequestSpec(videoUrl, promptText, promptForGemini25, streaming, expectJsonResponse, null, new ArrayList<>());
            }

            @NonNull
            static RequestSpec forChat(
                    @NonNull String videoUrl,
                    @NonNull String summary,
                    @NonNull List<ChatMessage> history
            ) {
                String languageName = getLanguageName();
                String chatPrompt = "You are continuing a conversation about this video. Use the video itself and the summary below to answer follow-up questions in " + languageName + ". Use Markdown when it improves readability. If relevant, cite timestamps in [MM:SS] or [HH:MM:SS]. If an answer is not supported by the video, say so briefly.\n\nSummary:\n" + summary;
                return new RequestSpec(videoUrl, null, null, true, false, chatPrompt, new ArrayList<>(history));
            }

            boolean isChatRequest() {
                return chatContextPrompt != null;
            }
        }

    private record AttemptState(@NonNull String model, int modelIndex, int modelCount,
                                @NonNull String apiKey, int apiKeyIndex, int apiKeyCount) {
    }

    private enum AttemptStatus {
        SUCCESS,
        RETRY,
        FAILURE,
        CANCELLED
    }

    private record AttemptResult(@NonNull AttemptStatus status, @Nullable String resultText,
                                 @NonNull String errorMessage) {

        @NonNull
            private static AttemptResult success(@NonNull String resultText) {
                return new AttemptResult(AttemptStatus.SUCCESS, resultText, "");
            }

            @NonNull
            private static AttemptResult retry(@NonNull String errorMessage) {
                return new AttemptResult(AttemptStatus.RETRY, null, errorMessage);
            }

            @NonNull
            private static AttemptResult failure() {
                return new AttemptResult(AttemptStatus.FAILURE, null, "Stream interrupted after partial response.");
            }

            @NonNull
            private static AttemptResult cancelled() {
                return new AttemptResult(AttemptStatus.CANCELLED, null, "Operation cancelled.");
            }
        }

    private record StreamEventResult(boolean emittedPartial, @Nullable String failureMessage) {
        @NonNull
            private static StreamEventResult partial() {
                return new StreamEventResult(true, null);
            }

            @NonNull
            private static StreamEventResult empty() {
                return new StreamEventResult(false, null);
            }

            @NonNull
            private static StreamEventResult failure(@NonNull String failureMessage) {
                return new StreamEventResult(false, failureMessage);
            }
        }

    /**
     * Callback interface for Gemini API operations (summary, transcription, and chat).
     * Defines methods to handle streamed chunks, successful results, or failures.
     */
    public interface Callback {
        /**
         * Called when the Gemini API request completes successfully.
         *
         * @param result The generated text from the API.
         */
        void onSuccess(String result);

        /**
         * Called when the Gemini API emits a streamed text chunk.
         *
         * @param partialText     The newly received delta text.
         * @param accumulatedText The full text accumulated so far.
         * @param model           The model currently producing the response.
         */
        default void onPartial(String partialText, String accumulatedText, @Nullable String model) {
        }

        /**
         * Called when the Gemini API request completes successfully, with the model used.
         * Default implementation preserves backward compatibility by delegating to {@link #onSuccess(String)}.
         *
         * @param result The generated text from the API.
         * @param model  The exact model that produced the result.
         */
        default void onSuccessWithModel(String result, @Nullable String model) {
            onSuccess(result);
        }

        /**
         * Called when the Gemini API request fails.
         *
         * @param error A message describing the error that occurred.
         */
        void onFailure(String error);
    }

    @NonNull
    private static String getPrompt() {
        String langName = getLanguageName();
        return "Write a spoiler-heavy summary of this video in " + langName + ". Focus on key events in chronological order. Use Markdown to make it readable, and mark important details with bold text. Output only the summary-do not include a preamble, greetings, or phrases like \"Here is the summary.\" If the video contains sponsored segments, exclude them entirely. Add a TL;DR at the beginning that gives only the most important information. Then provide the summary, separated from the TL;DR.";
    }
}
