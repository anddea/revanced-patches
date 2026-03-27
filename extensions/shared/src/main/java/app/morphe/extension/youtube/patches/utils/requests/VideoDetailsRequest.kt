package app.morphe.extension.youtube.patches.utils.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_VIDEO_DETAILS
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.StringRef.str
import app.morphe.extension.shared.utils.Utils
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

class VideoDetailsRequest private constructor(
    private val videoId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<String> = Utils.submitOnBackgroundThread {
        fetch(
            videoId,
            requestHeader,
        )
    }

    val message: String?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getMessage timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getMessage interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getMessage failure" },
                    ex
                )
            }

            return null
        }

    companion object {
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        @GuardedBy("itself")
        val cache: MutableMap<String, VideoDetailsRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, VideoDetailsRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, VideoDetailsRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        fun clear() {
            synchronized(cache) {
                cache.clear()
            }
        }

        @JvmStatic
        fun fetchRequestIfNeeded(
            videoId: String,
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = VideoDetailsRequest(
                        videoId,
                        requestHeader,
                    )
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): VideoDetailsRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(
            videoId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching video details request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_VIDEO_DETAILS,
                    clientType,
                    requestHeader,
                )

                val requestBody = createApplicationRequestBody(
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

        private fun parseResponse(json: JSONObject): String? {
            try {
                val videoDetailsJson = json.getJSONObject("videoDetails")
                val author = videoDetailsJson.getString("author")
                val description = videoDetailsJson.getString("shortDescription")
                val title = videoDetailsJson.getString("title")

                if (!author.isNullOrEmpty() && !title.isNullOrEmpty()) {
                    return if (description.isNullOrEmpty()) {
                        str(
                            "revanced_queue_manager_video_information_no_description_user_dialog_message",
                            author,
                            title
                        )
                    } else {
                        str(
                            "revanced_queue_manager_video_information_user_dialog_message",
                            author,
                            title,
                            description
                        )
                    }
                }
            } catch (e: JSONException) {
                val jsonForMessage = json.toString()
                Logger.printException(
                    { "Fetch failed while processing response data for response: $jsonForMessage" },
                    e
                )
            }

            return null
        }

        private fun fetch(
            videoId: String,
            requestHeader: Map<String, String>,
        ): String? {
            val json = sendRequest(videoId, requestHeader)
            if (json != null) {
                return parseResponse(json)
            }

            return null
        }
    }
}
