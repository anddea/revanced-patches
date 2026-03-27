package app.morphe.extension.youtube.patches.general.requests

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createJSRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_VIDEO_DETAILS
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.Utils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ChannelIdRequest private constructor(
    private val videoId: String
) {
    private val future: Future<String> = Utils.submitOnBackgroundThread {
        fetch(videoId)
    }

    val channelId: String?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getChannelId timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getChannelId interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getChannelId failure" },
                    ex
                )
            }

            return null
        }

    companion object {
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000L // 20 seconds

        @GuardedBy("itself")
        val cache: MutableMap<String, ChannelIdRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, ChannelIdRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, ChannelIdRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun fetchRequestIfNeeded(videoId: String) {
            cache[videoId] = ChannelIdRequest(videoId)
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): ChannelIdRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(videoId: String): JSONObject? {
            val startTime = System.currentTimeMillis()
            val clientType = YouTubeClient.ClientType.MWEB
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching video details request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_VIDEO_DETAILS,
                    clientType
                )
                val requestBody = createJSRequestBody(
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
                Logger.printException({ "sendRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseResponse(videoDetailsJson: JSONObject): String? {
            try {
                val videoDetailsJson = videoDetailsJson.getJSONObject("videoDetails")

                // Live streams always open when live ring is clicked.
                // Make sure this video is live streams.
                val isLiveContent = videoDetailsJson.has("isLiveContent") &&
                        videoDetailsJson.getBoolean("isLiveContent")

                // Even if 'isLiveContent' is true, it may be 'UPCOMING' video.
                // Check if the value of 'isUpcoming' is true.
                val isUpcoming = videoDetailsJson.has("isUpcoming") &&
                        videoDetailsJson.getBoolean("isUpcoming")

                // Return the channel id only if the video is live streams and not 'UPCOMING' video.
                if (isLiveContent && !isUpcoming) {
                    return videoDetailsJson.getString("channelId")
                }
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing response data for response: $videoDetailsJson" },
                    e
                )
            }

            return null
        }

        private fun fetch(videoId: String): String? {
            val videoDetailsJson = sendRequest(videoId)
            if (videoDetailsJson != null) {
                return parseResponse(videoDetailsJson)
            }

            return null
        }
    }
}
