package app.revanced.extension.shared.patches.spoof.requests

import app.revanced.extension.shared.patches.client.AppClient
import app.revanced.extension.shared.patches.client.WebClient
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.requests.Route
import app.revanced.extension.shared.requests.Route.CompiledRoute
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.Utils
import org.apache.commons.lang3.StringUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets

@Suppress("deprecation")
object PlayerRoutes {
    @JvmField
    val GET_CATEGORY: CompiledRoute = Route(
        Route.Method.POST,
        "player" +
                "?fields=microformat.playerMicroformatRenderer.category"
    ).compile()

    @JvmField
    val GET_PLAYLIST_PAGE: CompiledRoute = Route(
        Route.Method.POST,
        "next" +
                "?fields=contents.singleColumnWatchNextResults.playlist.playlist"
    ).compile()

    @JvmField
    val GET_STREAMING_DATA: CompiledRoute = Route(
        Route.Method.POST,
        "player" +
                "?fields=streamingData" +
                "&alt=proto"
    ).compile()

    private const val YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/"

    /**
     * TCP connection and HTTP read timeout
     */
    private const val CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000 // 10 Seconds.

    private val LOCALE_LANGUAGE: String = Utils.getContext().resources
        .configuration.locale.language

    @JvmStatic
    fun createApplicationRequestBody(
        clientType: AppClient.ClientType,
        videoId: String,
        playlistId: String? = null,
        botGuardPoToken: String? = null,
        visitorId: String? = null,
    ): ByteArray {
        val innerTubeBody = JSONObject()

        try {
            val client = JSONObject()
            client.put("clientName", clientType.clientName)
            client.put("clientVersion", clientType.clientVersion)
            client.put("deviceModel", clientType.deviceModel)
            client.put("osVersion", clientType.osVersion)
            if (clientType.androidSdkVersion != null) {
                client.put("androidSdkVersion", clientType.androidSdkVersion)
                client.put("osName", "Android")
            } else {
                client.put("deviceMake", "Apple")
                client.put("osName", "iOS")
            }
            if (!clientType.supportsCookies) {
                client.put("hl", LOCALE_LANGUAGE)
            }

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
        clientType: WebClient.ClientType,
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
    @Throws(IOException::class)
    fun getPlayerResponseConnectionFromRoute(route: CompiledRoute, userAgent: String): HttpURLConnection {
        val connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route)

        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", userAgent)

        connection.useCaches = false
        connection.doOutput = true

        connection.connectTimeout = CONNECTION_TIMEOUT_MILLISECONDS
        connection.readTimeout = CONNECTION_TIMEOUT_MILLISECONDS
        return connection
    }
}