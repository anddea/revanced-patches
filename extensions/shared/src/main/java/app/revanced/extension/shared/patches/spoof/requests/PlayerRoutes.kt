package app.revanced.extension.shared.patches.spoof.requests

import app.revanced.extension.shared.patches.client.YouTubeAppClient
import app.revanced.extension.shared.patches.client.YouTubeWebClient
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.requests.Route
import app.revanced.extension.shared.requests.Route.CompiledRoute
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import org.apache.commons.lang3.StringUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("deprecation")
object PlayerRoutes {
    @JvmField
    val GET_CATEGORY: CompiledRoute = Route(
        Route.Method.POST,
        "player" +
                "?prettyPrint=false" +
                "&fields=microformat.playerMicroformatRenderer.category"
    ).compile()

    @JvmField
    val GET_PLAYLIST_PAGE: CompiledRoute = Route(
        Route.Method.POST,
        "next" +
                "?prettyPrint=false" +
                "&fields=contents.singleColumnWatchNextResults.playlist.playlist"
    ).compile()

    @JvmField
    val GET_STREAMING_DATA: CompiledRoute = Route(
        Route.Method.POST,
        "player" +
                "?fields=streamingData" +
                "&alt=proto"
    ).compile()

    @JvmField
    val GET_VIDEO_DETAILS: CompiledRoute = Route(
        Route.Method.POST,
        "player" +
                "?prettyPrint=false" +
                "&fields=videoDetails.channelId"
    ).compile()

    private const val YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/"

    /**
     * TCP connection and HTTP read timeout
     */
    private const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000 // 10 Seconds.

    private val LOCALE: Locale = Utils.getContext().resources
        .configuration.locale
    private val LOCALE_COUNTRY: String = LOCALE.country
    private val LOCALE_LANGUAGE: String = LOCALE.language
    private val TIME_ZONE: TimeZone = TimeZone.getDefault()
    private val TIME_ZONE_ID: String = TIME_ZONE.id
    private val UTC_OFFSET_MINUTES: Int = TIME_ZONE.getOffset(Date().time) / 60000

    @JvmStatic
    fun createApplicationRequestBody(
        clientType: YouTubeAppClient.ClientType,
        videoId: String,
        playlistId: String? = null,
        botGuardPoToken: String = "",
        visitorId: String = "",
        setLocale: Boolean = false,
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
                if (clientType.gmscoreVersionCode != null) {
                    client.put("gmscoreVersionCode", clientType.gmscoreVersionCode)
                }
            }
            client.put(
                "hl",
                if (setLocale) {
                    BaseSettings.SPOOF_STREAMING_DATA_LANGUAGE.get().language
                } else {
                    LOCALE_LANGUAGE
                }
            )
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

            if (!StringUtils.isAnyEmpty(botGuardPoToken, visitorId)) {
                val serviceIntegrityDimensions = JSONObject()
                serviceIntegrityDimensions.put("poToken", botGuardPoToken)
                innerTubeBody.put("serviceIntegrityDimensions", serviceIntegrityDimensions)
            }
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create application innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun createWebInnertubeBody(
        clientType: YouTubeWebClient.ClientType,
        videoId: String
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientType.clientVersion)
            val context = JSONObject()
            context.put("client", client)

            val lockedSafetyMode = JSONObject()
            lockedSafetyMode.put("lockedSafetyMode", false)
            val user = JSONObject()
            user.put("user", lockedSafetyMode)

            innerTubeBody.put("context", context)
            innerTubeBody.put("contentCheckOk", true)
            innerTubeBody.put("racyCheckOk", true)
            innerTubeBody.put("videoId", videoId)
        } catch (e: JSONException) {
            Logger.printException({ "Failed to create web innerTubeBody" }, e)
        }

        return innerTubeBody.toString().toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun getPlayerResponseConnectionFromRoute(
        route: CompiledRoute,
        clientType: YouTubeAppClient.ClientType
    ): HttpURLConnection {
        return getPlayerResponseConnectionFromRoute(
            route,
            clientType.userAgent,
            clientType.id.toString(),
            clientType.clientVersion
        )
    }

    @JvmStatic
    fun getPlayerResponseConnectionFromRoute(
        route: CompiledRoute,
        clientType: YouTubeWebClient.ClientType
    ): HttpURLConnection {
        return getPlayerResponseConnectionFromRoute(
            route,
            clientType.userAgent,
            clientType.id.toString(),
            clientType.clientVersion,
        )
    }

    @Throws(IOException::class)
    fun getPlayerResponseConnectionFromRoute(
        route: CompiledRoute,
        userAgent: String,
        clientId: String,
        clientVersion: String
    ): HttpURLConnection {
        val connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route)

        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("X-YouTube-Client-Name", clientId)
        connection.setRequestProperty("X-YouTube-Client-Version", clientVersion)

        connection.useCaches = false
        connection.doOutput = true

        connection.connectTimeout = CONNECTION_TIMEOUT_MILLISECONDS
        connection.readTimeout = CONNECTION_TIMEOUT_MILLISECONDS
        return connection
    }

}