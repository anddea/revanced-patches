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

package app.morphe.extension.shared.innertube.requests

import android.os.Build
import app.morphe.extension.shared.innertube.client.YouTubeClient
import app.morphe.extension.shared.innertube.utils.ThrottlingParameterUtils
import app.morphe.extension.shared.requests.Requester
import app.morphe.extension.shared.requests.Route.CompiledRoute
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.StringRef.str
import app.morphe.extension.shared.utils.Utils
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("deprecation")
object InnerTubeRequestBody {
    private const val YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/"
    private const val PLAYLIST_CLIENT_ID = "3"
    private const val PLAYLIST_CLIENT_VERSION = "20.26.46"
    private const val PLAYLIST_PACKAGE_NAME = "com.google.android.youtube"

    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val PAGE_ID_HEADER = "X-Goog-PageId"
    private const val VISITOR_ID_HEADER: String = "X-Goog-Visitor-Id"
    private val REQUEST_HEADER_KEYS = setOf(
        AUTHORIZATION_HEADER,  // Available only to logged-in users.
        PAGE_ID_HEADER,
        VISITOR_ID_HEADER,
    )

    /**
     * TCP connection and HTTP read timeout
     */
    private const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000 // 10 Seconds.

    private val LOCALE: Locale by lazy {
        Utils.getContext().resources.configuration.locale
    }
    private val LOCALE_COUNTRY: String by lazy {
        LOCALE.country
    }
    private val LOCALE_LANGUAGE: String by lazy {
        LOCALE.language
    }
    private val TIME_ZONE: TimeZone = TimeZone.getDefault()
    private val TIME_ZONE_ID: String = TIME_ZONE.id
    private val UTC_OFFSET_MINUTES: Int = TIME_ZONE.getOffset(Date().time) / 60000

    @JvmStatic
    fun createApplicationRequestBody(
        clientType: YouTubeClient.ClientType,
        videoId: String,
        playlistId: String? = null,
        language: String = LOCALE_LANGUAGE,
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("deviceMake", clientType.deviceMake)
            client.put("deviceModel", clientType.deviceModel)
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientType.clientVersion)
            client.put("osName", clientType.osName)
            client.put("osVersion", clientType.osVersion)
            if (clientType.androidSdkVersion != null) {
                client.put("androidSdkVersion", clientType.androidSdkVersion)
            }
            client.put("hl", language)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", "$UTC_OFFSET_MINUTES")

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("racyCheckOk", true)
            innerTubeBody.put("videoId", videoId)

            if (playlistId != null) {
                innerTubeBody.put("playlistId", playlistId)
            }
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create application innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun createJSRequestBody(
        clientType: YouTubeClient.ClientType,
        videoId: String,
        isGVS: Boolean = false,
        isInlinePlayback: Boolean = false,
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", ThrottlingParameterUtils.getClientVersion(clientType))
            client.put("platform", clientType.clientPlatform)
            client.put("clientScreen", clientType.clientScreen)
            client.put("hl", LOCALE_LANGUAGE)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", UTC_OFFSET_MINUTES.toString())

            if (clientType.name.startsWith("TV")) {
                val configInfo = JSONObject()
                configInfo.put("appInstallData", "")
                client.put("configInfo", configInfo)
            }

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("racyCheckOk", true)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("videoId", videoId)

            val user = JSONObject()
            user.put("lockedSafetyMode", false)
            context.put("user", user)

            if (isGVS) {
                val contentPlaybackContext = JSONObject()
                if (clientType.refererFormat != null) {
                    contentPlaybackContext.put(
                        "referer",
                        String.format(clientType.refererFormat, videoId)
                    )
                }
                contentPlaybackContext.put("html5Preference", "HTML5_PREF_WANTS")
                if (isInlinePlayback) {
                    // https://iter.ca/post/yt-adblock/
                    contentPlaybackContext.put("isInlinePlaybackNoAd", true)
                }
                val signatureTimestamp =
                    ThrottlingParameterUtils.getSignatureTimestamp()
                if (signatureTimestamp != null) {
                    contentPlaybackContext.put("signatureTimestamp", signatureTimestamp)
                }

                val devicePlaybackCapabilities = JSONObject()
                devicePlaybackCapabilities.put("supportsVp9Encoding", true)
                devicePlaybackCapabilities.put("supportXhr", false)

                val playbackContext = JSONObject()
                playbackContext.put("contentPlaybackContext", contentPlaybackContext)
                playbackContext.put("devicePlaybackCapabilities", devicePlaybackCapabilities)

                innerTubeBody.put("playbackContext", playbackContext)
            }
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create js innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun androidInnerTubeBody(
        clientType: YouTubeClient.ClientType = YouTubeClient.ClientType.ANDROID,
        clientVersion: String = clientType.clientVersion,
    ): JSONObject {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("deviceMake", clientType.deviceMake)
            client.put("deviceModel", clientType.deviceModel)
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientVersion)
            client.put("osName", clientType.osName)
            client.put("osVersion", clientType.osVersion)
            client.put("androidSdkVersion", clientType.androidSdkVersion)
            client.put("hl", LOCALE_LANGUAGE)
            client.put("gl", LOCALE_COUNTRY)
            client.put("timeZone", TIME_ZONE_ID)
            client.put("utcOffsetMinutes", UTC_OFFSET_MINUTES.toString())

            val context = JSONObject()
            context.put("client", client)

            innerTubeBody.put("context", context)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("racyCheckOk", true)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create android innerTubeBody" }, e)
        }

        return innerTubeBody
    }

    /**
     * Playlist mutation endpoints reject older Android clients, so queue manager requests
     * use Morphe's known-good Android 20.26.46 client identity even on older target APKs.
     */
    private fun playlistInnerTubeBody(): JSONObject =
        androidInnerTubeBody(clientVersion = PLAYLIST_CLIENT_VERSION)

    @JvmStatic
    fun createPlaylistRequestBody(
        videoId: String,
    ): ByteArray = createPlaylistRequestBody(listOf(videoId))

    /**
     * Builds the queue playlist request with all initial videos in display order.
     */
    @JvmStatic
    fun createPlaylistRequestBody(
        videoIds: List<String>,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("params", "CAQ%3D")
            // TODO: Implement an AlertDialog that allows changing the title of the playlist.
            innerTubeBody.put("title", str("revanced_queue_manager_queue"))

            val videoIdsJson = JSONArray()
            videoIds.forEach { videoIdsJson.put(it) }
            innerTubeBody.put("videoIds", videoIdsJson)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create create/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getSetVideoIdRequestBody(
        videoId: String,
        playlistId: String,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("videoId", videoId)
            innerTubeBody.put("playlistId", playlistId)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create get set video id innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun deletePlaylistRequestBody(
        playlistId: String,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create delete/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun editPlaylistRequestBody(
        videoId: String,
        playlistId: String,
        setVideoId: String?,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)

            val actionsObject = JSONObject()
            if (setVideoId != null && setVideoId.isNotEmpty()) {
                actionsObject.put("action", "ACTION_REMOVE_VIDEO")
                actionsObject.put("setVideoId", setVideoId)
            } else {
                actionsObject.put("action", "ACTION_ADD_VIDEO")
                actionsObject.put("addedVideoId", videoId)
            }

            val actionsArray = JSONArray()
            actionsArray.put(0, actionsObject)
            innerTubeBody.put("actions", actionsArray)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create edit/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getPlaylistsRequestBody(
        playlistId: String,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)
            innerTubeBody.put("excludeWatchLater", false)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create get/playlists innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun savePlaylistRequestBody(
        playlistId: String,
        libraryId: String,
    ): ByteArray {
        val innerTubeBody = playlistInnerTubeBody()

        try {
            innerTubeBody.put("playlistId", playlistId)

            val actionsObject = JSONObject()
            actionsObject.put("action", "ACTION_ADD_PLAYLIST")
            actionsObject.put("addedFullListId", libraryId)

            val actionsArray = JSONArray()
            actionsArray.put(0, actionsObject)
            innerTubeBody.put("actions", actionsArray)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create save/playlist innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getPlaylistResponseConnectionFromRoute(
        route: CompiledRoute,
        requestHeader: Map<String, String>? = null,
        connectTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
        readTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
    ): HttpURLConnection {
        val userAgent = String.format(
            Locale.US,
            "%s/%s (Linux; U; Android %s; %s; %s Build/%s)",
            PLAYLIST_PACKAGE_NAME,
            PLAYLIST_CLIENT_VERSION,
            Build.VERSION.RELEASE,
            Locale.getDefault(),
            Build.MODEL,
            Build.ID,
        )

        val connection = getInnerTubeResponseConnectionFromRoute(
            route = route,
            userAgent = userAgent,
            clientId = PLAYLIST_CLIENT_ID,
            clientVersion = PLAYLIST_CLIENT_VERSION,
            requestHeader = null,
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
        )

        if (requestHeader != null) {
            for ((key, value) in requestHeader) {
                if (!value.isNullOrEmpty()) {
                    connection.setRequestProperty(key, value)
                }
            }
        }

        return connection
    }

    @JvmStatic
    fun getInnerTubeResponseConnectionFromRoute(
        route: CompiledRoute,
        clientType: YouTubeClient.ClientType,
        requestHeader: Map<String, String>? = null,
        connectTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
        readTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
    ) = getInnerTubeResponseConnectionFromRoute(
        route = route,
        userAgent = clientType.userAgent,
        clientId = clientType.id.toString(),
        clientVersion = clientType.clientVersion,
        supportsCookies = clientType.supportsCookies,
        requestHeader = requestHeader,
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
    )

    @Throws(IOException::class)
    fun getInnerTubeResponseConnectionFromRoute(
        route: CompiledRoute,
        userAgent: String,
        clientId: String,
        clientVersion: String,
        supportsCookies: Boolean = true,
        requestHeader: Map<String, String>? = null,
        connectTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
        readTimeout: Int = CONNECTION_TIMEOUT_MILLISECONDS,
    ): HttpURLConnection {
        val connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route)

        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("X-YouTube-Client-Name", clientId)
        connection.setRequestProperty("X-YouTube-Client-Version", clientVersion)
        connection.setRequestProperty("X-GOOG-API-FORMAT-VERSION", "2")

        connection.useCaches = false
        connection.doOutput = true

        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout

        if (requestHeader != null) {
            for (key in REQUEST_HEADER_KEYS) {
                if (!supportsCookies && StringUtils.equalsAny(
                        key,
                        AUTHORIZATION_HEADER,
                        PAGE_ID_HEADER,
                    )
                ) {
                    continue
                }
                val value = requestHeader[key]
                if (value != null) {
                    connection.setRequestProperty(key, value)
                }
            }
        }

        return connection
    }

}
