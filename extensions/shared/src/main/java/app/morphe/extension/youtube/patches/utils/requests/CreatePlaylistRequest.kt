/*
 * Copyright (C) 2025-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.utils.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createPlaylistRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getSetVideoIdRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getPlaylistResponseConnectionFromRoute
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
                Logger.printDebug { "getPlaylistId: waiting for future result" }
                val result = future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
                Logger.printDebug { "getPlaylistId: future returned: $result" }
                return result
            } catch (ex: TimeoutException) {
                Logger.printInfo({ "getPlaylistId timed out" }, ex)
            } catch (ex: InterruptedException) {
                Logger.printException({ "getPlaylistId interrupted" }, ex)
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException({ "getPlaylistId failure" }, ex)
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
            Logger.printDebug { "fetchRequestIfNeeded called for videoId: $videoId" }
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    Logger.printDebug { "fetchRequestIfNeeded: creating new CreatePlaylistRequest for videoId: $videoId" }
                    cache[videoId] = CreatePlaylistRequest(
                        videoId,
                        requestHeader,
                    )
                } else {
                    Logger.printDebug { "fetchRequestIfNeeded: cache already contains CreatePlaylistRequest for videoId: $videoId" }
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): CreatePlaylistRequest? {
            synchronized(cache) {
                val req = cache[videoId]
                Logger.printDebug { "getRequestForVideoId videoId: $videoId, returned: $req" }
                return req
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
            val clientTypeName = YouTubeClient.ClientType.ANDROID.name
            Logger.printInfo { "sendCreatePlaylistRequest for videoId: $videoId, using client: $clientTypeName" }

            try {
                val connection = getPlaylistResponseConnectionFromRoute(
                    CREATE_PLAYLIST,
                    requestHeader,
                )

                val requestBody = createPlaylistRequestBody(videoId = videoId)

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                Logger.printInfo { "sendCreatePlaylistRequest responseCode: $responseCode" }
                if (responseCode == 200) {
                    val json = Requester.parseJSONObject(connection)
                    Logger.printDebug { "sendCreatePlaylistRequest success response JSON: $json" }
                    return json
                }

                val errorBody = try { Requester.parseErrorString(connection) } catch (e: Exception) { "" }
                handleConnectionError(
                    (clientTypeName + " not available with response code: "
                            + responseCode + " message: " + connection.responseMessage + ", errorBody: " + errorBody),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "sendCreatePlaylistRequest failed" }, ex)
            } finally {
                Logger.printDebug {
                    "sendCreatePlaylistRequest for video: " + videoId + " took: " +
                            (System.currentTimeMillis() - startTime) + "ms"
                }
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
            val clientTypeName = YouTubeClient.ClientType.ANDROID.name
            Logger.printInfo { "sendSetVideoIdRequest for playlistId: $playlistId, videoId: $videoId, using client: $clientTypeName" }

            try {
                val connection = getPlaylistResponseConnectionFromRoute(
                    GET_SET_VIDEO_ID,
                    requestHeader,
                )

                val requestBody = getSetVideoIdRequestBody(
                    videoId = videoId,
                    playlistId = playlistId
                )

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                Logger.printInfo { "sendSetVideoIdRequest responseCode: $responseCode" }
                if (responseCode == 200) {
                    val json = Requester.parseJSONObject(connection)
                    Logger.printDebug { "sendSetVideoIdRequest success response JSON: $json" }
                    return json
                }

                val errorBody = try { Requester.parseErrorString(connection) } catch (e: Exception) { "" }
                handleConnectionError(
                    (clientTypeName + " not available with response code: "
                            + responseCode + " message: " + connection.responseMessage + ", errorBody: " + errorBody),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "sendSetVideoIdRequest failed" }, ex)
            } finally {
                Logger.printDebug {
                    "sendSetVideoIdRequest for playlist: " + playlistId + " took: " +
                            (System.currentTimeMillis() - startTime) + "ms"
                }
            }

            return null
        }

        private fun parseCreatePlaylistResponse(json: JSONObject): String? {
            try {
                val playlistId = json.getString("playlistId")
                Logger.printDebug { "parseCreatePlaylistResponse parsed playlistId: $playlistId" }
                return playlistId
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
                    val setVideoId = secondaryContentsJsonObject
                        .getJSONObject("playlistPanelVideoRenderer")
                        .getString("playlistSetVideoId")
                    Logger.printDebug { "parseSetVideoIdResponse parsed setVideoId: $setVideoId" }
                    return setVideoId
                } else {
                    Logger.printInfo { "parseSetVideoIdResponse: secondaryContentsJsonObject is not a JSONObject" }
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
            Logger.printDebug { "fetch starting for videoId: $videoId" }
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
                            Logger.printInfo { "fetch successful. playlistId: $playlistId, setVideoId: $setVideoId" }
                            return Pair(playlistId, setVideoId)
                        } else {
                            Logger.printInfo { "fetch failed: setVideoId is null" }
                        }
                    } else {
                        Logger.printInfo { "fetch failed: setVideoIdJson is null" }
                    }
                } else {
                    Logger.printInfo { "fetch failed: playlistId is null" }
                }
            } else {
                Logger.printInfo { "fetch failed: createPlaylistJson is null" }
            }

            return null
        }
    }
}
