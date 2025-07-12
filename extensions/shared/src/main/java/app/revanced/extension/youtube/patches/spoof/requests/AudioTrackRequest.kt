package app.revanced.extension.youtube.patches.spoof.requests

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import app.revanced.extension.shared.innertube.client.YouTubeAppClient
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.revanced.extension.shared.innertube.requests.InnerTubeRoutes.GET_STREAMING_DATA_JSON
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
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

class AudioTrackRequest private constructor(
    private val videoId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<MutableMap<String, Pair<String, Boolean>>?> =
        Utils.submitOnBackgroundThread {
            fetch(
                videoId,
                requestHeader,
            )
        }

    val stream: MutableMap<String, Pair<String, Boolean>>?
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
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 5 * 1000L // 5 seconds

        @GuardedBy("itself")
        val cache: MutableMap<String, AudioTrackRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, AudioTrackRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, AudioTrackRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun fetchRequestIfNeeded(videoId: String, requestHeader: Map<String, String>) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = AudioTrackRequest(videoId, requestHeader)
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): AudioTrackRequest? {
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
            Objects.requireNonNull(requestHeader)

            val startTime = System.currentTimeMillis()
            // '/player' endpoint requires a PoToken, and if there is no PoToken in the RequestBody, there is a playback issue after 1:10.
            // This is not a problem, as it is only used to get the list of audio tracks, not for streaming purposes.
            val clientType = YouTubeAppClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching audiotrack request for: $videoId" }

            try {
                val connection =
                    getInnerTubeResponseConnectionFromRoute(
                        GET_STREAMING_DATA_JSON,
                        clientType,
                        requestHeader
                    )

                val requestBody = createApplicationRequestBody(
                    clientType = clientType,
                    videoId = videoId,
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

        private fun parseResponse(json: JSONObject): MutableMap<String, Pair<String, Boolean>>? {
            try {
                val streamingData = json.getJSONObject("streamingData")
                val adaptiveFormats = streamingData.getJSONArray("adaptiveFormats")
                val audioTracksMap: MutableMap<String, Pair<String, Boolean>> =
                    LinkedHashMap(adaptiveFormats.length())
                for (i in 0..<adaptiveFormats.length()) {
                    val format = adaptiveFormats.get(i)

                    if (format is JSONObject && format.has("audioTrack")) {
                        val audioTrack = format.getJSONObject("audioTrack")
                        val displayName = audioTrack.getString("displayName")
                        val audioIsDefault = audioTrack.getBoolean("audioIsDefault")
                        val id = audioTrack.getString("id")
                        if (displayName != null && id != null) {
                            audioTracksMap.putIfAbsent(displayName, Pair(id, audioIsDefault))
                        }
                    }
                }
                if (audioTracksMap.isNotEmpty()) {
                    return audioTracksMap
                }
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing response data for response: $json" },
                    e
                )
            }

            return null
        }

        private fun fetch(
            videoId: String,
            requestHeader: Map<String, String>
        ): MutableMap<String, Pair<String, Boolean>>? {
            val json = sendRequest(videoId, requestHeader)
            if (json != null) {
                return parseResponse(json)
            }

            return null
        }
    }
}
