package app.revanced.extension.youtube.patches.video.requests

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import app.revanced.extension.shared.patches.client.AppClient
import app.revanced.extension.shared.patches.client.WebClient
import app.revanced.extension.shared.patches.spoof.requests.PlayerRoutes
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import app.revanced.extension.youtube.shared.VideoInformation
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MusicRequest private constructor(private val videoId: String, private val checkCategory: Boolean) {
    /**
     * Time this instance and the fetch future was created.
     */
    private val timeFetched = System.currentTimeMillis()
    private val future: Future<Boolean> = Utils.submitOnBackgroundThread {
        fetch(
            videoId,
            checkCategory,
        )
    }

    fun isExpired(now: Long): Boolean {
        val timeSinceCreation = now - timeFetched
        if (timeSinceCreation > CACHE_RETENTION_TIME_MILLISECONDS) {
            return true
        }

        // Only expired if the fetch failed (API null response).
        return (fetchCompleted() && stream == null)
    }

    /**
     * @return if the fetch call has completed.
     */
    private fun fetchCompleted(): Boolean {
        return future.isDone
    }

    val stream: Boolean?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS]
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

    companion object {
        /**
         * How long to keep fetches until they are expired.
         */
        private const val CACHE_RETENTION_TIME_MILLISECONDS = 60 * 1000L // 1 Minute

        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000L // 20 seconds

        @GuardedBy("itself")
        private val cache: MutableMap<String, MusicRequest> = HashMap()

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun fetchRequestIfNeeded(videoId: String, checkCategory: Boolean) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                val now = System.currentTimeMillis()
                cache.values.removeIf { request: MusicRequest ->
                    val expired = request.isExpired(now)
                    if (expired) Logger.printDebug { "Removing expired stream: " + request.videoId }
                    expired
                }
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = MusicRequest(videoId, checkCategory)
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): MusicRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendApplicationRequest(videoId: String): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            val clientType = AppClient.ClientType.ANDROID_VR
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playlist request for: $videoId using client: $clientTypeName" }

            try {
                val connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(PlayerRoutes.GET_PLAYLIST_PAGE, clientType.userAgent)
                val requestBody =
                    PlayerRoutes.createApplicationRequestBody(clientType, videoId, "RD$videoId")

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

        private fun sendWebRequest(videoId: String): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            val clientType = WebClient.ClientType.MWEB
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playability request for: $videoId using client: $clientTypeName" }

            try {
                val connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(PlayerRoutes.GET_CATEGORY, clientType.userAgent)
                val requestBody =
                    PlayerRoutes.createWebInnertubeBody(clientType, videoId)

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
                Logger.printException({ "sendWebRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseApplicationResponse(playlistJson: JSONObject): Boolean {
            try {
                val playerParams: String? = (playlistJson
                    .getJSONObject("contents")
                    .getJSONObject("singleColumnWatchNextResults")
                    .getJSONObject("playlist")
                    .getJSONObject("playlist")
                    .getJSONArray("contents")[0] as JSONObject)
                    .getJSONObject("playlistPanelVideoRenderer")
                    .getJSONObject("navigationEndpoint")
                    .getJSONObject("watchEndpoint")
                    .getString("playerParams")

                return VideoInformation.isMixPlaylistsOpenedByUser(playerParams!!)
            } catch (e: JSONException) {
                Logger.printDebug { "Fetch failed while processing Application response data for response: $playlistJson" }
            }

            return false
        }

        private fun parseWebResponse(microFormatJson: JSONObject): Boolean {
            try {
                return microFormatJson
                    .getJSONObject("playerMicroformatRenderer")
                    .getJSONObject("category")
                    .getString("status")
                    .equals("Music")
            } catch (e: JSONException) {
                Logger.printDebug { "Fetch failed while processing Web response data for response: $microFormatJson" }
            }

            return false
        }

        private fun fetch(videoId: String, checkCategory: Boolean): Boolean {
            if (checkCategory) {
                val microFormatJson = sendWebRequest(videoId)
                if (microFormatJson != null) {
                    return parseWebResponse(microFormatJson)
                }
            } else {
                val playlistJson = sendApplicationRequest(videoId)
                if (playlistJson != null) {
                    return parseApplicationResponse(playlistJson)
                }
            }

            return false
        }
    }
}
