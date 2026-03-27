package app.morphe.extension.youtube.patches.utils.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.editPlaylistRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.EDIT_PLAYLIST
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

class EditPlaylistRequest private constructor(
    private val videoId: String,
    private val playlistId: String,
    private val setVideoId: String?,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<String> = Utils.submitOnBackgroundThread {
        fetch(
            videoId,
            playlistId,
            setVideoId,
            requestHeader,
        )
    }

    val result: String?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getResult timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getResult interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getResult failure" },
                    ex
                )
            }

            return null
        }

    companion object {
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        @GuardedBy("itself")
        val cache: MutableMap<String, EditPlaylistRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, EditPlaylistRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, EditPlaylistRequest>): Boolean {
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
        fun clearVideoId(videoId: String) {
            synchronized(cache) {
                cache.remove(videoId)
            }
        }

        @JvmStatic
        fun fetchRequestIfNeeded(
            videoId: String,
            playlistId: String,
            setVideoId: String?,
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = EditPlaylistRequest(
                        videoId,
                        playlistId,
                        setVideoId,
                        requestHeader,
                    )
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): EditPlaylistRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(
            videoId: String,
            playlistId: String,
            setVideoId: String?,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            // 'browse/edit_playlist' endpoint does not require PoToken.
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching edit playlist request, videoId: $videoId, playlistId: $playlistId, setVideoId: $setVideoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    EDIT_PLAYLIST,
                    clientType,
                    requestHeader,
                )

                val requestBody = editPlaylistRequestBody(
                    videoId = videoId,
                    playlistId = playlistId,
                    setVideoId = setVideoId
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

        private fun parseResponse(json: JSONObject, remove: Boolean): String? {
            try {
                if (json.getString("status") == "STATUS_SUCCEEDED") {
                    if (remove) {
                        return ""
                    }
                    val playlistEditResultsJSONObject =
                        json.getJSONArray("playlistEditResults").get(0)

                    if (playlistEditResultsJSONObject is JSONObject) {
                        return playlistEditResultsJSONObject
                            .getJSONObject("playlistEditVideoAddedResultData")
                            .getString("setVideoId")
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
            playlistId: String,
            setVideoId: String?,
            requestHeader: Map<String, String>,
        ): String? {
            val json = sendRequest(
                videoId,
                playlistId,
                setVideoId,
                requestHeader,
            )
            if (json != null) {
                return parseResponse(json, setVideoId != null && setVideoId.isNotEmpty())
            }

            return null
        }
    }
}
