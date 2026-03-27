package app.morphe.extension.youtube.patches.utils.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createPlaylistRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.CREATE_PLAYLIST
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_SET_VIDEO_ID
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.utils.Logger
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

class CreatePlaylistRequest private constructor(
    private val videoId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<Pair<String, String>> = Utils.submitOnBackgroundThread {
        fetch(
            videoId,
            requestHeader,
        )
    }

    val playlistId: Pair<String, String>?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getPlaylistId timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getPlaylistId interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getPlaylistId failure" },
                    ex
                )
            }

            return null
        }

    companion object {
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        @GuardedBy("itself")
        val cache: MutableMap<String, CreatePlaylistRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, CreatePlaylistRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, CreatePlaylistRequest>): Boolean {
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
                    cache[videoId] = CreatePlaylistRequest(
                        videoId,
                        requestHeader,
                    )
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): CreatePlaylistRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendCreatePlaylistRequest(
            videoId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            // 'playlist/create' endpoint does not require PoToken.
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching create playlist request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    CREATE_PLAYLIST,
                    clientType,
                    requestHeader,
                )

                val requestBody = createPlaylistRequestBody(videoId = videoId)

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
                Logger.printException({ "sendCreatePlaylistRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun sendSetVideoIdRequest(
            videoId: String,
            playlistId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(playlistId)

            val startTime = System.currentTimeMillis()
            // 'playlist/create' endpoint does not require PoToken.
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching set video id request for: $playlistId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_SET_VIDEO_ID,
                    clientType,
                    requestHeader,
                )

                val requestBody = createApplicationRequestBody(
                    clientType = clientType,
                    videoId = videoId,
                    playlistId = playlistId
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
                Logger.printException({ "sendSetVideoIdRequest failed" }, ex)
            } finally {
                Logger.printDebug { "playlist: " + playlistId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseCreatePlaylistResponse(json: JSONObject): String? {
            try {
                return json.getString("playlistId")
            } catch (e: JSONException) {
                val jsonForMessage = json.toString()
                Logger.printException(
                    { "Fetch failed while processing response data for response: $jsonForMessage" },
                    e
                )
            }

            return null
        }

        private fun parseSetVideoIdResponse(json: JSONObject): String? {
            try {
                val secondaryContentsJsonObject =
                    json.getJSONObject("contents")
                        .getJSONObject("singleColumnWatchNextResults")
                        .getJSONObject("playlist")
                        .getJSONObject("playlist")
                        .getJSONArray("contents")
                        .get(0)

                if (secondaryContentsJsonObject is JSONObject) {
                    return secondaryContentsJsonObject
                        .getJSONObject("playlistPanelVideoRenderer")
                        .getString("playlistSetVideoId")
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
        ): Pair<String, String>? {
            val createPlaylistJson = sendCreatePlaylistRequest(
                videoId,
                requestHeader,
            )
            if (createPlaylistJson != null) {
                val playlistId = parseCreatePlaylistResponse(createPlaylistJson)
                if (playlistId != null) {
                    val setVideoIdJson = sendSetVideoIdRequest(
                        videoId,
                        playlistId,
                        requestHeader,
                    )
                    if (setVideoIdJson != null) {
                        val setVideoId = parseSetVideoIdResponse(setVideoIdJson)
                        if (setVideoId != null) {
                            return Pair(playlistId, setVideoId)
                        }
                    }
                }
            }

            return null
        }
    }
}
