package app.revanced.extension.youtube.patches.player.requests

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.patches.client.YouTubeAppClient
import app.revanced.extension.shared.patches.spoof.requests.PlayerRoutes
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import app.revanced.extension.youtube.patches.player.ActionButtonsPatch.ActionButton
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ActionButtonRequest private constructor(
    private val videoId: String,
    private val playerHeaders: Map<String, String>,
) {
    private val future: Future<Array<ActionButton>> = Utils.submitOnBackgroundThread {
        fetch(videoId, playerHeaders)
    }

    val array: Array<ActionButton>
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getArray timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getArray interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getArray failure" },
                    ex
                )
            }

            return emptyArray()
        }

    companion object {
        /**
         * TCP connection and HTTP read timeout.
         */
        private const val HTTP_TIMEOUT_MILLISECONDS = 10 * 1000

        /**
         * Any arbitrarily large value, but must be at least twice [HTTP_TIMEOUT_MILLISECONDS]
         */
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        @GuardedBy("itself")
        val cache: MutableMap<String, ActionButtonRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, ActionButtonRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, ActionButtonRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        fun fetchRequestIfNeeded(videoId: String, playerHeaders: Map<String, String>) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = ActionButtonRequest(videoId, playerHeaders)
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): ActionButtonRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private val REQUEST_HEADER_KEYS = arrayOf(
            "Authorization",  // Available only to logged-in users.
            "X-GOOG-API-FORMAT-VERSION",
            "X-Goog-Visitor-Id"
        )

        private fun sendRequest(videoId: String, playerHeaders: Map<String, String>): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            // '/next' request does not require PoToken.
            val clientType = YouTubeAppClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playlist request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(
                    PlayerRoutes.GET_VIDEO_ACTION_BUTTON,
                    clientType
                )
                connection.connectTimeout = HTTP_TIMEOUT_MILLISECONDS
                connection.readTimeout = HTTP_TIMEOUT_MILLISECONDS

                // Since [THANKS] button and [CLIP] button are shown only with the logged in,
                // Set the [Authorization] field to property to get the correct action buttons.
                for (key in REQUEST_HEADER_KEYS) {
                    var value = playerHeaders[key]
                    if (value != null) {
                        connection.setRequestProperty(key, value)
                    }
                }

                val requestBody =
                    PlayerRoutes.createApplicationRequestBody(
                        clientType = clientType,
                        videoId = videoId
                    )

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                if (responseCode == 200) return Requester.parseJSONObject(connection)

                handleConnectionError(
                    (clientTypeName + " not available with response code: "
                            + responseCode + " message: " + connection.responseMessage),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "sendApplicationRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseResponse(json: JSONObject): Array<ActionButton> {
            try {
                val secondaryContentsJsonObject =
                    json.getJSONObject("contents")
                        .getJSONObject("singleColumnWatchNextResults")
                        .getJSONObject("results")
                        .getJSONObject("results")
                        .getJSONArray("contents")
                        .get(0)

                if (secondaryContentsJsonObject is JSONObject) {
                    val tertiaryContentsJsonArray =
                        secondaryContentsJsonObject
                            .getJSONObject("slimVideoMetadataSectionRenderer")
                            .getJSONArray("contents")

                    val elementRendererJsonObject =
                        tertiaryContentsJsonArray
                            .get(tertiaryContentsJsonArray.length() - 1)

                    if (elementRendererJsonObject is JSONObject) {
                        val buttons =
                            elementRendererJsonObject
                                .getJSONObject("elementRenderer")
                                .getJSONObject("newElement")
                                .getJSONObject("type")
                                .getJSONObject("componentType")
                                .getJSONObject("model")
                                .getJSONObject("videoActionBarModel")
                                .getJSONArray("buttons")

                        val length = buttons.length()
                        val buttonsArr = Array<ActionButton>(length) { ActionButton.UNKNOWN }

                        for (i in 0 until length) {
                            val jsonObjectString = buttons.get(i).toString()
                            for (b in ActionButton.entries) {
                                if (b.identifier != null && jsonObjectString.contains(b.identifier)) {
                                    buttonsArr[i] = b
                                }
                            }
                        }

                        // Still, the response includes the [LIVE_CHAT] button.
                        // In the Android YouTube client, this button moved to the comments.
                        return buttonsArr.filter { it.setting != null }.toTypedArray()
                    }
                }
            } catch (e: JSONException) {
                val jsonForMessage = json.toString().substring(3000)
                Logger.printException(
                    { "Fetch failed while processing response data for response: $jsonForMessage" },
                    e
                )
            }

            return emptyArray()
        }

        private fun fetch(videoId: String, playerHeaders: Map<String, String>): Array<ActionButton> {
            val json = sendRequest(videoId, playerHeaders)
            if (json != null) {
                return parseResponse(json)
            }

            return emptyArray()
        }
    }
}
