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

package app.morphe.extension.youtube.patches.player.requests

import androidx.annotation.GuardedBy
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.morphe.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.morphe.extension.shared.innertube.requests.InnerTubeRoutes.GET_VIDEO_ACTION_BUTTON
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.Utils
import app.morphe.extension.youtube.patches.player.ActionButtonsPatch.ActionButton
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

class ActionButtonRequest private constructor(
    private val videoId: String,
    private val requestHeader: Map<String, String>,
) {
    private val future: Future<Array<ActionButton>> = Utils.submitOnBackgroundThread {
        fetch(videoId, requestHeader)
    }

    val array: Array<ActionButton>
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
            } catch (ex: TimeoutException) {
                Logger.printInfo(
                    { "getArray timed out" },
                    ex
                )
            } catch (ex: InterruptedException) {
                Logger.printException(
                    { "getArray interrupted" },
                    ex
                )
                Thread.currentThread().interrupt() // Restore interrupt status flag.
            } catch (ex: ExecutionException) {
                Logger.printException(
                    { "getArray failure" },
                    ex
                )
            }

            return emptyArray()
        }

    companion object {
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        @GuardedBy("itself")
        val cache: MutableMap<String, ActionButtonRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, ActionButtonRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, ActionButtonRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        fun fetchRequestIfNeeded(videoId: String, requestHeader: Map<String, String>) {
            Objects.requireNonNull(videoId)
            synchronized(cache) {
                if (!cache.containsKey(videoId)) {
                    cache[videoId] = ActionButtonRequest(videoId, requestHeader)
                }
            }
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): ActionButtonRequest? {
            synchronized(cache) {
                return cache[videoId]
            }
        }

        private fun handleConnectionError(toastMessage: String, ex: Exception?) {
            Logger.printInfo({ toastMessage }, ex)
        }

        private fun sendRequest(videoId: String, requestHeader: Map<String, String>): JSONObject? {
            Objects.requireNonNull(videoId)

            val startTime = System.currentTimeMillis()
            // '/next' endpoint does not require PoToken.
            val clientType = YouTubeClient.ClientType.ANDROID
            val clientTypeName = clientType.name
            Logger.printDebug { "Fetching playlist request for: $videoId, using client: $clientTypeName" }

            try {
                // Since [THANKS] button and [CLIP] button are shown only with the logged in,
                // Set the [Authorization] field to property to get the correct action buttons.
                val connection = getInnerTubeResponseConnectionFromRoute(
                    GET_VIDEO_ACTION_BUTTON,
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

        private fun parseResponse(json: JSONObject): Array<ActionButton> {
            try {
                val secondaryContentsJsonArray =
                    json.getJSONObject("contents")
                        .getJSONObject("singleColumnWatchNextResults")
                        .getJSONObject("results")
                        .getJSONObject("results")
                        .getJSONArray("contents")

                for (i in 0..<secondaryContentsJsonArray.length()) {
                    val secondaryContentsJsonObject = secondaryContentsJsonArray.getJSONObject(i)
                    if (secondaryContentsJsonObject.has("slimVideoMetadataSectionRenderer")) {
                        val tertiaryContentsJsonArray =
                            secondaryContentsJsonObject
                                .getJSONObject("slimVideoMetadataSectionRenderer")
                                .getJSONArray("contents")

                        val elementRendererJsonObject =
                            tertiaryContentsJsonArray
                                .getJSONObject(tertiaryContentsJsonArray.length() - 1)

                        val buttons =
                            elementRendererJsonObject
                                .getJSONObject("elementRenderer")
                                .getJSONObject("newElement")
                                .getJSONObject("type")
                                .getJSONObject("componentType")
                                .getJSONObject("model")
                                .getJSONObject("videoActionBarModel")
                                .getJSONObject("videoActionBarData")
                                .getJSONArray("buttons")

                        val length = buttons.length()
                        val buttonsArr = Array(length) { ActionButton.UNKNOWN }

                        for (i in 0 until length) {
                            val jsonObjectString = buttons.get(i).toString()
                            for (b in ActionButton.entries) {
                                if (b.identifiers.any { jsonObjectString.contains(it) }) {
                                    buttonsArr[i] = b
                                    break
                                }
                            }
                        }

                        // Still, the response includes the [LIVE_CHAT] button.
                        // In the Android YouTube client, this button moved to the comments.
                        return buttonsArr.filter { it.setting != null }.toTypedArray()
                    }
                }
            } catch (e: JSONException) {
                Logger.printException(
                    { "Fetch failed while processing response data for response: $json" },
                    e
                )
            }

            return emptyArray()
        }

        private fun fetch(
            videoId: String,
            requestHeader: Map<String, String>
        ): Array<ActionButton> {
            val json = sendRequest(videoId, requestHeader)
            if (json != null) {
                return parseResponse(json)
            }

            return emptyArray()
        }
    }
}
