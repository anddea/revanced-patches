package app.morphe.extension.youtube.patches.utils.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.savePlaylistRequestBody
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

class SavePlaylistRequest private constructor(
    private val playlistId: String,
    private val libraryId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<Boolean> = Utils.submitOnBackgroundThread {
        fetch(
            playlistId,
            libraryId,
            requestHeader,
        )
    }

    val result: Boolean?
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
        val cache: MutableMap<String, SavePlaylistRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, SavePlaylistRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, SavePlaylistRequest>): Boolean {
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
            playlistId: String,
            libraryId: String,
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(playlistId)
            synchronized(cache) {
                cache[libraryId] = SavePlaylistRequest(
                    playlistId,
                    libraryId,
                    requestHeader,
                )
            }
        }

        @JvmStatic
        fun getRequestForLibraryId(libraryId: String): SavePlaylistRequest? {
            synchronized(cache) {
                return cache[libraryId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(
            playlistId: String,
            libraryId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(playlistId)
            Objects.requireNonNull(libraryId)

            val startTime = System.currentTimeMillis()
            // 'browse/edit_playlist' endpoint does not require PoToken.
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching edit playlist request, playlistId: $playlistId, libraryId: $libraryId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    EDIT_PLAYLIST,
                    clientType,
                    requestHeader,
                )

                val requestBody = savePlaylistRequestBody(libraryId, playlistId)

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
                Logger.printDebug { "playlistId: $playlistId libraryId: $libraryId took: ${(System.currentTimeMillis() - startTime)}ms" }
            }

            return null
        }

        private fun parseResponse(json: JSONObject): Boolean? {
            try {
                return json.getString("status") == "STATUS_SUCCEEDED"
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
            playlistId: String,
            libraryId: String,
            requestHeader: Map<String, String>,
        ): Boolean? {
            val json = sendRequest(
                playlistId,
                libraryId,
                requestHeader,
            )
            if (json != null) {
                return parseResponse(json)
            }

            return null
        }
    }
}
