/*
 * Copyright (C) 2024-2026 anddea
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

package app.morphe.extension.youtube.patches.video.requests

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient.ClientType
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createJSRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_CATEGORY
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_PLAYLIST_ENDPOINT
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.Utils
import app.morphe.extension.youtube.shared.VideoInformation
import app.morphe.extension.youtube.patches.video.PlaybackSpeedPatch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.Objects

class MusicRequest private constructor(
    private val videoId: String,
    private val checkCategory: Boolean,
    private val requestHeader: Map<String, String>,
) {
    /**
     * The asynchronously fetched music classification, or null while unavailable.
     * A successful music result notifies the playback-speed patch without blocking callers.
     */
    @Volatile
    var stream: Boolean? = null
        private set

    init {
        Utils.submitOnBackgroundThread {
            try {
                val result = fetch(
                    videoId,
                    checkCategory,
                    requestHeader,
                )
                stream = result
                if (result) {
                    PlaybackSpeedPatch.musicRequestCompleted(videoId)
                }
                result
            } catch (ex: Exception) {
                Logger.printException({ "fetch failed" }, ex)
                false
            }
        }
    }

    companion object {

        @GuardedBy("itself")
        val cache: MutableMap<String, MusicRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, MusicRequest>(20) {
                private val CACHE_LIMIT = 10

                override fun removeEldestEntry(eldest: Map.Entry<String, MusicRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun fetchRequestIfNeeded(
            videoId: String,
            checkCategory: Boolean,
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = MusicRequest(
                        videoId,
                        checkCategory,
                        requestHeader
                    )
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

        private fun sendApplicationRequest(
            clientType: ClientType,
            videoId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(videoId)
            Objects.requireNonNull(requestHeader)

            val startTime = System.currentTimeMillis()
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playlist request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    route = GET_PLAYLIST_ENDPOINT,
                    clientType = clientType,
                    requestHeader = requestHeader
                )
                val requestBody =
                    createApplicationRequestBody(
                        clientType = clientType,
                        videoId = videoId,
                        playlistId = "RD$videoId",
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

        private fun sendWebRequest(videoId: String): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            val clientType = ClientType.MWEB
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching microformat request for: $videoId, using client: $clientTypeName" }

            try {
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_CATEGORY,
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
                Logger.printException({ "sendWebRequest failed" }, ex)
            } finally {
                Logger.printDebug { "video: " + videoId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseApplicationResponse(
            clientType: ClientType,
            playlistJson: JSONObject,
        ): Boolean {
            try {
                val singleColumnWatchNextResultsJsonObject: JSONObject =
                    playlistJson
                        .getJSONObject("contents")
                        .getJSONObject("singleColumnWatchNextResults")

                if (!singleColumnWatchNextResultsJsonObject.has("playlist")) {
                    return false
                }

                val playlistJsonObject: JSONObject? =
                    singleColumnWatchNextResultsJsonObject
                        .getJSONObject("playlist")
                        .getJSONObject("playlist")

                val currentStreamJsonObject = playlistJsonObject
                    ?.getJSONArray("contents")
                    ?.get(0)

                if (currentStreamJsonObject !is JSONObject) {
                    return false
                }

                val navigationEndpointJsonObject =
                    currentStreamJsonObject
                        .getJSONObject("playlistPanelVideoRenderer")
                        .getJSONObject("navigationEndpoint")

                val watchEndpointJsonObject: JSONObject? =
                    if (clientType == ClientType.ANDROID
                        && navigationEndpointJsonObject.has("coWatchWatchEndpointWrapperCommand")
                    ) { // Android
                        navigationEndpointJsonObject
                            .getJSONObject("coWatchWatchEndpointWrapperCommand")
                            .getJSONObject("watchEndpoint")
                            .getJSONObject("watchEndpoint")
                    } else if (clientType == ClientType.ANDROID_VR_NO_AUTH
                        && navigationEndpointJsonObject.has("watchEndpoint")
                    ) { // Android VR
                        navigationEndpointJsonObject
                            .getJSONObject("watchEndpoint")
                    } else {
                        null
                    }

                if (watchEndpointJsonObject == null || !watchEndpointJsonObject.has("playerParams"))
                    return false

                val playerParams: String? = watchEndpointJsonObject.getString("playerParams")
                return playerParams != null && VideoInformation.isMixPlaylistsOpenedByUser(
                    playerParams
                )
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing Application response data for response: $playlistJson" },
                    e
                )
            }

            return false
        }

        private fun parseWebResponse(microFormatJson: JSONObject): Boolean {
            try {
                return microFormatJson
                    .getJSONObject("microformat")
                    .getJSONObject("playerMicroformatRenderer")
                    .getString("category")
                    .equals("Music")
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing Web response data for response: $microFormatJson" },
                    e
                )
            }

            return false
        }

        private fun fetch(
            videoId: String,
            checkCategory: Boolean,
            requestHeader: Map<String, String>,
        ): Boolean {
            if (checkCategory) {
                val microFormatJson = sendWebRequest(videoId)
                if (microFormatJson != null) {
                    return parseWebResponse(microFormatJson)
                }
            } else {
                for (clientType in arrayOf(ClientType.ANDROID_VR_NO_AUTH, ClientType.ANDROID)) {
                    val playlistJson = sendApplicationRequest(
                        clientType,
                        videoId,
                        requestHeader
                    )
                    if (playlistJson != null) {
                        return parseApplicationResponse(clientType, playlistJson)
                    }
                }
            }

            return false
        }
    }
}
