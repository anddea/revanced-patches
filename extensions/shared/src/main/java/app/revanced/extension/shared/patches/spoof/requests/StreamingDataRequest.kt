package app.revanced.extension.shared.patches.spoof.requests

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.patches.client.AppClient
import app.revanced.extension.shared.patches.client.AppClient.availableClientTypes
import app.revanced.extension.shared.patches.spoof.requests.PlayerRoutes.GET_STREAMING_DATA
import app.revanced.extension.shared.patches.spoof.requests.PlayerRoutes.createApplicationRequestBody
import app.revanced.extension.shared.patches.spoof.requests.PlayerRoutes.getPlayerResponseConnectionFromRoute
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Video streaming data.  Fetching is tied to the behavior YT uses,
 * where this class fetches the streams only when YT fetches.
 *
 * Effectively the cache expiration of these fetches is the same as the stock app,
 * since the stock app would not use expired streams and therefor
 * the extension replace stream hook is called only if YT
 * did use its own client streams.
 */
class StreamingDataRequest private constructor(
    videoId: String, playerHeaders: Map<String, String>, visitorId: String,
    botGuardPoToken: String, droidGuardPoToken: String
) {
    private val videoId: String
    private val future: Future<ByteBuffer?>

    init {
        Objects.requireNonNull(playerHeaders)
        this.videoId = videoId
        this.future = Utils.submitOnBackgroundThread {
            fetch(
                videoId,
                playerHeaders,
                visitorId,
                botGuardPoToken,
                droidGuardPoToken
            )
        }
    }

    fun fetchCompleted(): Boolean {
        return future.isDone
    }

    val stream: ByteBuffer?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getStream timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getStream interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getStream failure" },
                    ex
                )
            }

            return null
        }

    override fun toString(): String {
        return "StreamingDataRequest{videoId='$videoId'}"
    }

    companion object {
        private var CLIENT_ORDER_TO_USE: Array<AppClient.ClientType>
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val VISITOR_ID_HEADER = "X-Goog-Visitor-Id"
        private val REQUEST_HEADER_KEYS = arrayOf(
            AUTHORIZATION_HEADER,  // Available only to logged-in users.
            "X-GOOG-API-FORMAT-VERSION",
            VISITOR_ID_HEADER
        )
        private var lastSpoofedClientType: AppClient.ClientType? = null


        /**
         * TCP connection and HTTP read timeout.
         */
        private const val HTTP_TIMEOUT_MILLISECONDS = 10 * 1000

        /**
         * Any arbitrarily large value, but must be at least twice [.HTTP_TIMEOUT_MILLISECONDS]
         */
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000
        @GuardedBy("itself")
        val cache: MutableMap<String, StreamingDataRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, StreamingDataRequest>(100) {
                /**
                 * Cache limit must be greater than the maximum number of videos open at once,
                 * which theoretically is more than 4 (3 Shorts + one regular minimized video).
                 * But instead use a much larger value, to handle if a video viewed a while ago
                 * is somehow still referenced.  Each stream is a small array of Strings
                 * so memory usage is not a concern.
                 */
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, StreamingDataRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        val lastSpoofedClientName: String
            get() = lastSpoofedClientType
                ?.friendlyName
                ?: "Unknown"

        init {
            val allClientTypes: Array<AppClient.ClientType> = availableClientTypes
            val preferredClient = BaseSettings.SPOOF_STREAMING_DATA_TYPE.get()

            CLIENT_ORDER_TO_USE = allClientTypes
            if (ArrayUtils.indexOf(allClientTypes, preferredClient) >= 0) {
                CLIENT_ORDER_TO_USE[0] = preferredClient
                var i = 1
                for (c in allClientTypes) {
                    if (c != preferredClient) {
                        CLIENT_ORDER_TO_USE[i++] = c
                    }
                }
            }
        }

        @JvmStatic
        fun fetchRequest(
            videoId: String, fetchHeaders: Map<String, String>, visitorId: String,
            botGuardPoToken: String, droidGuardPoToken: String
        ) {
            // Always fetch, even if there is an existing request for the same video.
            cache[videoId] =
                StreamingDataRequest(
                    videoId,
                    fetchHeaders,
                    visitorId,
                    botGuardPoToken,
                    droidGuardPoToken
                )
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): StreamingDataRequest? {
            return cache[videoId]
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun send(
            clientType: AppClient.ClientType, videoId: String, playerHeaders: Map<String, String>,
            visitorId: String, botGuardPoToken: String, droidGuardPoToken: String
        ): HttpURLConnection? {
            Objects.requireNonNull(clientType)
            Objects.requireNonNull(videoId)
            Objects.requireNonNull(playerHeaders)

            val startTime = System.currentTimeMillis()
            Logger.printDebug { "Fetching video streams for: $videoId using client: $clientType" }

            try {
                val connection = getPlayerResponseConnectionFromRoute(GET_STREAMING_DATA, clientType.userAgent)
                connection.connectTimeout = HTTP_TIMEOUT_MILLISECONDS
                connection.readTimeout = HTTP_TIMEOUT_MILLISECONDS

                val usePoToken = clientType.requirePoToken && !StringUtils.isAnyEmpty(botGuardPoToken, visitorId)

                for (key in REQUEST_HEADER_KEYS) {
                    var value = playerHeaders[key]
                    if (value != null) {
                        if (key == AUTHORIZATION_HEADER) {
                            if (!clientType.supportsCookies) {
                                Logger.printDebug { "Not including request header: $key" }
                                continue
                            }
                        }
                        if (key == VISITOR_ID_HEADER && usePoToken) {
                            val originalVisitorId: String = value
                            Logger.printDebug { "Original visitor id:\n$originalVisitorId" }
                            Logger.printDebug { "Replaced visitor id:\n$visitorId" }
                            value = visitorId
                        }

                        connection.setRequestProperty(key, value)
                    }
                }

                val requestBody: ByteArray
                if (usePoToken) {
                    requestBody = createApplicationRequestBody(
                        clientType = clientType,
                        videoId = videoId,
                        botGuardPoToken = botGuardPoToken,
                        visitorId = visitorId
                    )
                    if (droidGuardPoToken.isNotEmpty()) {
                        Logger.printDebug { "Original poToken (droidGuardPoToken):\n$droidGuardPoToken" }
                    }
                    Logger.printDebug { "Replaced poToken (botGuardPoToken):\n$botGuardPoToken" }
                } else {
                    requestBody =
                        createApplicationRequestBody(clientType = clientType, videoId = videoId)
                }
                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                if (responseCode == 200) return connection

                // This situation likely means the patches are outdated.
                // Use a toast message that suggests updating.
                handleConnectionError(
                    ("Playback error (App is outdated?) " + clientType + ": "
                            + responseCode + " response: " + connection.responseMessage),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "send failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun fetch(
            videoId: String, playerHeaders: Map<String, String>, visitorId: String,
            botGuardPoToken: String, droidGuardPoToken: String
        ): ByteBuffer? {
            lastSpoofedClientType = null

            // Retry with different client if empty response body is received.
            for (clientType in CLIENT_ORDER_TO_USE) {
                if (clientType.requireAuth &&
                    playerHeaders[AUTHORIZATION_HEADER] == null) {
                    Logger.printDebug { "Skipped login-required client (incognito mode or not logged in)\nClient: $clientType\nVideo: $videoId" }
                    continue
                }
                send(
                    clientType,
                    videoId,
                    playerHeaders,
                    visitorId,
                    botGuardPoToken,
                    droidGuardPoToken
                )?.let { connection ->
                    try {
                        // gzip encoding doesn't response with content length (-1),
                        // but empty response body does.
                        if (connection.contentLength == 0) {
                            Logger.printDebug { "Received empty response\nClient: $clientType\nVideo: $videoId" }
                        } else {
                            BufferedInputStream(connection.inputStream).use { inputStream ->
                                ByteArrayOutputStream().use { stream ->
                                    val buffer = ByteArray(2048)
                                    var bytesRead: Int
                                    while ((inputStream.read(buffer).also { bytesRead = it }) >= 0) {
                                        stream.write(buffer, 0, bytesRead)
                                    }
                                    lastSpoofedClientType = clientType
                                    return ByteBuffer.wrap(stream.toByteArray())
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        Logger.printException({ "Fetch failed while processing response data" }, ex)
                    }
                }
            }

            handleConnectionError("Could not fetch any client streams", null)
            return null
        }
    }
}
