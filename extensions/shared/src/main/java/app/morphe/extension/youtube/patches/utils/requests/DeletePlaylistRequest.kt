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
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.deletePlaylistRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getPlaylistResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.DELETE_PLAYLIST
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

class DeletePlaylistRequest private constructor(
    private val playlistId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<Boolean> = Utils.submitOnBackgroundThread {
        fetch(
            playlistId,
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
        val cache: MutableMap<String, DeletePlaylistRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, DeletePlaylistRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, DeletePlaylistRequest>): Boolean {
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
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(playlistId)
            synchronized(cache) {
                if (!cache.containsKey(playlistId)) {
                    cache[playlistId] = DeletePlaylistRequest(
                        playlistId,
                        requestHeader,
                    )
                }
            }
        }

        @JvmStatic
        fun getRequestForPlaylistId(playlistId: String): DeletePlaylistRequest? {
            synchronized(cache) {
                return cache[playlistId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(
            playlistId: String,
            requestHeader: Map<String, String>,
        ): JSONObject? {
            Objects.requireNonNull(playlistId)

            val startTime = System.currentTimeMillis()
            // 'playlist/delete' endpoint does not require PoToken.
            val clientTypeName = YouTubeClient.ClientType.ANDROID.name
            Logger.printDebug { "Fetching delete playlist request, playlistId: $playlistId, using client: $clientTypeName" }

            try {
                val connection = getPlaylistResponseConnectionFromRoute(
                    DELETE_PLAYLIST,
                    requestHeader,
                )

                val requestBody = deletePlaylistRequestBody(playlistId)

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
                Logger.printDebug { "playlist: " + playlistId + " took: " + (System.currentTimeMillis() - startTime) + "ms" }
            }

            return null
        }

        private fun parseResponse(json: JSONObject): Boolean? {
            try {
                return json.has("command")
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
            requestHeader: Map<String, String>,
        ): Boolean? {
            val json = sendRequest(
                playlistId,
                requestHeader,
            )
            if (json != null) {
                return parseResponse(json)
            }

            return null
        }
    }
}
