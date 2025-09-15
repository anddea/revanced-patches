package app.revanced.extension.shared.patches.spoof.requests

import androidx.annotation.GuardedBy
import app.revanced.extension.shared.innertube.client.YouTubeClient
import app.revanced.extension.shared.innertube.client.YouTubeClient.ClientType
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.createApplicationRequestBody
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.createJSRequestBody
import app.revanced.extension.shared.innertube.requests.InnerTubeRequestBody.getInnerTubeResponseConnectionFromRoute
import app.revanced.extension.shared.innertube.requests.InnerTubeRoutes.GET_AUDIO_TRACK
import app.revanced.extension.shared.innertube.requests.InnerTubeRoutes.GET_STREAMING_DATA
import app.revanced.extension.shared.innertube.utils.PlayerResponseOuterClass.PlayerResponse
import app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils.getAdaptiveFormats
import app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils.getFormats
import app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils.setUrl
import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup
import app.revanced.extension.shared.patches.spoof.StreamingDataOuterClassPatch.parseFrom
import app.revanced.extension.shared.requests.Requester
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.shared.utils.StringRef.str
import app.revanced.extension.shared.utils.Utils
import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData
import com.liskovsoft.youtubeapi.app.PoTokenGate
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.Objects
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * Video streaming data.  Fetching is tied to the behavior YT uses,
 * where this class fetches the streams only when YT fetches.
 *
 * Effectively the cache expiration of these fetches is the same as the stock app,
 * since the stock app would not use expired streams and therefor
 * the extension replace stream hook is called only if YT
 * did use its own client streams.
 */
@Suppress("deprecation")
class StreamingDataRequest private constructor(
    videoId: String,
    requestHeader: Map<String, String>,
    reasonSkipped: String,
) {
    private val videoId: String
    private val future: Future<StreamingData?>

    init {
        Objects.requireNonNull(requestHeader)
        this.videoId = videoId
        this.future = Utils.submitOnBackgroundThread {
            fetch(
                videoId,
                requestHeader,
                reasonSkipped,
            )
        }
    }

    fun fetchCompleted(): Boolean {
        return future.isDone
    }

    val stream: StreamingData?
        get() {
            try {
                return future[MAX_MILLISECONDS_TO_WAIT_FOR_FETCH.toLong(), TimeUnit.MILLISECONDS]
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

    override fun toString(): String {
        return "StreamingDataRequest{videoId='$videoId'}"
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val VISITOR_ID_HEADER: String = "X-Goog-Visitor-Id"
        private const val MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20 * 1000

        private val SPOOF_STREAMING_DATA_DEFAULT_CLIENT: ClientType =
            BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get()
        private val CLIENT_ORDER_TO_USE: Array<ClientType> =
            YouTubeClient.availableClientTypes(SPOOF_STREAMING_DATA_DEFAULT_CLIENT)
        private val liveStreams: ByteArrayFilterGroup =
            ByteArrayFilterGroup(
                null,
                "yt_live_broadcast",
                "yt_premiere_broadcast"
            )
        private var lastSpoofedClient: ClientType? = null

        // When this value is not empty, it is used as the preferred language when creating the RequestBody.
        private var overrideLanguage: String = ""

        @GuardedBy("itself")
        val cache: MutableMap<String, StreamingDataRequest> = Collections.synchronizedMap(
            object : LinkedHashMap<String, StreamingDataRequest>(100) {
                private val CACHE_LIMIT = 50

                override fun removeEldestEntry(eldest: Map.Entry<String, StreamingDataRequest>): Boolean {
                    return size > CACHE_LIMIT // Evict the oldest entry if over the cache limit.
                }
            })

        @JvmStatic
        val lastSpoofedClientName: String
            get() = lastSpoofedClient?.friendlyName ?: "Unknown"

        @JvmStatic
        val lastSpoofedClientIsNoAuth: Boolean
            get() = BooleanUtils.isFalse(lastSpoofedClient?.supportsCookies?: true)

        @JvmStatic
        fun overrideLanguage(language: String) {
            overrideLanguage = language
        }

        @JvmStatic
        fun fetchRequest(
            videoId: String,
            fetchHeaders: Map<String, String>,
            reasonSkipped: String,
        ) {
            // Always fetch, even if there is an existing request for the same video.
            cache[videoId] =
                StreamingDataRequest(
                    videoId,
                    fetchHeaders,
                    reasonSkipped,
                )
        }

        @JvmStatic
        fun getRequestForVideoId(videoId: String): StreamingDataRequest? {
            return cache[videoId]
        }

        private fun handleConnectionError(
            toastMessage: String,
            ex: Exception?,
            showToast: Boolean = false,
        ) {
            if (showToast) Utils.showToastShort(toastMessage)
            Logger.printInfo({ toastMessage }, ex)
        }

        /**
         * Replace with visitorData bound to PoToken.
         */
        private fun replaceVisitorData(
            clientType: ClientType,
            videoId: String,
            requestHeader: Map<String, String>,
        ): Map<String, String> {
            if (clientType.requirePoToken) {
                val finalRequestHeader: MutableMap<String, String> =
                    LinkedHashMap(requestHeader.size)
                val visitorData = PoTokenGate.getVisitorData(videoId)
                for (key in requestHeader.keys) {
                    val value = requestHeader[key]
                    if (value != null) {
                        if (key == VISITOR_ID_HEADER) {
                            if (!visitorData.isNullOrEmpty()) {
                                finalRequestHeader.put(VISITOR_ID_HEADER, visitorData)
                                continue
                            }
                        }
                        finalRequestHeader.put(key, value)
                    }
                }
                return finalRequestHeader
            }
            return requestHeader
        }

        private fun getPlayabilityStatus(
            videoId: String,
            streamBytes: ByteArray
        ): String? {
            val startTime = System.currentTimeMillis()
            try {
                val playerResponse: PlayerResponse? = PlayerResponse.parseFrom(streamBytes)
                if (playerResponse != null) {
                    val playabilityStatus = playerResponse.getPlayabilityStatus()
                    if (playabilityStatus != null) {
                        return playabilityStatus.getStatus().name
                    }
                }
            } catch (ex: Exception) {
                Logger.printException({ "Get playability status failed" }, ex)
            } finally {
                Logger.printDebug { "Get playability status end (videoId: $videoId, took: ${(System.currentTimeMillis() - startTime)} ms)" }
            }

            return null
        }

        private fun getDeobfuscatedUrlArrayList(
            clientType: ClientType,
            videoId: String,
            streamBytes: ByteArray,
        ): Pair<ArrayList<String>?, ArrayList<String>?>? {
            val startTime = System.currentTimeMillis()

            try {
                val playerResponse: PlayerResponse? = PlayerResponse.parseFrom(streamBytes)
                if (playerResponse != null) {
                    val streamingData = playerResponse.getStreamingData()
                    if (streamingData == null) {
                        Logger.printDebug { "StreamingData is null, legacy client will be used" }
                        return null
                    }
                    val adaptiveFormatsCount = streamingData.adaptiveFormatsCount
                    if (adaptiveFormatsCount < 1) {
                        Logger.printDebug { "AdaptiveFormats is empty, legacy client will be used" }
                        return null
                    }

                    val deobfuscatedAdaptiveFormatsArrayList: ArrayList<String> =
                        ArrayList(adaptiveFormatsCount)
                    val requirePoToken = clientType.requirePoToken
                    val isTV = !requirePoToken
                    val sessionPoToken = if (requirePoToken)
                        PoTokenGate.getSessionPoToken(videoId)
                    else
                        null

                    for (i in 0..<adaptiveFormatsCount) {
                        val adaptiveFormats = streamingData.getAdaptiveFormats(i)

                        if (adaptiveFormats != null) {
                            var streamUrl: String?
                            val url: String? = adaptiveFormats.url
                            val signatureCipher: String? = adaptiveFormats.signatureCipher

                            if (!url.isNullOrEmpty()) {
                                streamUrl = url
                            } else if (!signatureCipher.isNullOrEmpty()) {
                                streamUrl =
                                    ThrottlingParameterUtils.getUrlWithThrottlingParameterObfuscated(
                                        videoId,
                                        signatureCipher,
                                        isTV
                                    )
                            } else {
                                // Neither streamingUrl nor signatureCipher are present in the response.
                                // In this case, decoding serverAbrStreamingUrl is required to obtain streamingUrl.
                                // This feature is not yet implemented in RVX.
                                Logger.printDebug { "Neither streamingUrl nor signatureCipher were found, legacy client will be used" }
                                return null
                            }
                            // streamUrl not found, trying to fetch with legacy client.
                            if (streamUrl.isNullOrEmpty()) {
                                Logger.printDebug { "StreamUrl was not found, legacy client will be used" }
                                return null
                            }
                            var deobfuscatedUrl =
                                ThrottlingParameterUtils.getUrlWithThrottlingParameterDeobfuscated(
                                    videoId,
                                    streamUrl,
                                    isTV
                                )
                            if (!deobfuscatedUrl.isNullOrEmpty() && !sessionPoToken.isNullOrEmpty()) {
                                deobfuscatedUrl += "&pot=$sessionPoToken"
                            }
                            if (deobfuscatedUrl.isNullOrEmpty()) {
                                Logger.printDebug { "Failed to decrypt n-sig or signatureCipher, please check if latest regular expressions are being used" }
                                return null
                            }
                            deobfuscatedAdaptiveFormatsArrayList.add(i, deobfuscatedUrl)
                        }
                    }

                    val deobfuscatedFormatsArrayList: ArrayList<String> = ArrayList(0)
                    val formatsCount = streamingData.formatsCount
                    if (formatsCount > 0) {
                        for (i in 0..<formatsCount) {
                            val formats = streamingData.getFormats(i)
                            if (formats == null) {
                                Logger.printDebug { "Formats is empty" }
                                deobfuscatedFormatsArrayList.clear()
                                break
                            }
                            var streamUrl: String?
                            val url: String? = formats.url
                            val signatureCipher: String? = formats.signatureCipher

                            if (!url.isNullOrEmpty()) {
                                streamUrl = url
                            } else if (!signatureCipher.isNullOrEmpty()) {
                                streamUrl =
                                    ThrottlingParameterUtils.getUrlWithThrottlingParameterObfuscated(
                                        videoId,
                                        signatureCipher,
                                        isTV
                                    )
                            } else {
                                // Neither streamingUrl nor signatureCipher are present in the response.
                                // In this case, decoding serverAbrStreamingUrl is required to obtain streamingUrl.
                                // This feature is not yet implemented in RVX.
                                Logger.printDebug { "Neither streamingUrl nor signatureCipher were found" }
                                deobfuscatedFormatsArrayList.clear()
                                break
                            }
                            // streamUrl not found, trying to fetch with legacy client.
                            if (streamUrl.isNullOrEmpty()) {
                                Logger.printDebug { "StreamUrl was not found" }
                                deobfuscatedFormatsArrayList.clear()
                                break
                            }
                            var deobfuscatedUrl =
                                ThrottlingParameterUtils.getUrlWithThrottlingParameterDeobfuscated(
                                    videoId,
                                    streamUrl,
                                    isTV
                                )
                            if (!deobfuscatedUrl.isNullOrEmpty() && !sessionPoToken.isNullOrEmpty()) {
                                deobfuscatedUrl += "&pot=$sessionPoToken"
                            }
                            if (deobfuscatedUrl.isNullOrEmpty()) {
                                Logger.printDebug { "Failed to decrypt n-sig or signatureCipher" }
                                deobfuscatedFormatsArrayList.clear()
                                break
                            }
                            deobfuscatedFormatsArrayList.add(i, deobfuscatedUrl)
                        }
                    }

                    return Pair(
                        deobfuscatedAdaptiveFormatsArrayList,
                        deobfuscatedFormatsArrayList
                    )
                }
            } catch (ex: Exception) {
                Logger.printException({ "Get deobfuscatedUrls failed" }, ex)
            } finally {
                Logger.printDebug { "Get deobfuscatedUrls end (videoId: $videoId, took: ${(System.currentTimeMillis() - startTime)} ms)" }
            }

            return null
        }

        private fun deobfuscateStreamingData(
            deobfuscatedUrlArrayList: ArrayList<String>,
            isAdaptiveFormats: Boolean,
            streamingData: StreamingData
        ): StreamingData {
            val formats = if (isAdaptiveFormats) {
                getAdaptiveFormats(streamingData)
            } else {
                getFormats(streamingData)
            }
            if (formats != null) {
                for (i in 0..<formats.size) {
                    val format = formats[i]
                    val url = deobfuscatedUrlArrayList[i]
                    setUrl(format, url)
                }
            }
            return streamingData
        }

        private fun setAudioTrackLanguage(
            videoId: String,
            requestHeader: Map<String, String>,
        ) {
            Objects.requireNonNull(videoId)
            Objects.requireNonNull(requestHeader)

            for (clientType in arrayOf(ClientType.ANDROID, ClientType.TV_SIMPLY_NO_AUTH, ClientType.ANDROID_VR)) {
                val startTime = System.currentTimeMillis()
                Logger.printDebug { "Fetching audio track for: $videoId using client: $clientType" }

                try {
                    val requestBody = if (clientType.requireJS) {
                        createJSRequestBody(
                            clientType = clientType,
                            videoId = videoId,
                            language = "en",
                            isGVS = false,
                        )
                    } else {
                        createApplicationRequestBody(
                            clientType = clientType,
                            videoId = videoId,
                            language = "en"
                        )
                    }

                    val connection =
                        getInnerTubeResponseConnectionFromRoute(
                            GET_AUDIO_TRACK,
                            clientType,
                            requestHeader
                        )

                    connection.setFixedLengthStreamingMode(requestBody.size)
                    connection.outputStream.write(requestBody)

                    val responseCode = connection.responseCode
                    if (responseCode != 200) {
                        // This situation likely means the patches are outdated.
                        // Use a toast message that suggests updating.
                        handleConnectionError(
                            ("Playback error (App is outdated?) " + clientType + ": "
                                    + responseCode + " response: " + connection.responseMessage),
                            null
                        )
                        return
                    }
                    val json = Requester.parseJSONObject(connection)

                    if (!json.has("captions")) {
                        return
                    }
                    val captions =
                        json.getJSONObject("captions")
                    if (!captions.has("playerCaptionsTracklistRenderer")) {
                        return
                    }
                    val playerCaptionsTracklistRenderer =
                        captions.getJSONObject("playerCaptionsTracklistRenderer")
                    if (!playerCaptionsTracklistRenderer.has("audioTracks")) {
                        return
                    }
                    val audioTracks =
                        playerCaptionsTracklistRenderer.getJSONArray("audioTracks")
                    if (audioTracks.length() < 2) {
                        return
                    }
                    if (!json.has("streamingData")) {
                        return
                    }
                    val streamingData =
                        json.getJSONObject("streamingData")
                    if (!streamingData.has("adaptiveFormats")) {
                        return
                    }
                    val adaptiveFormats =
                        streamingData.getJSONArray("adaptiveFormats")
                    if (adaptiveFormats.length() < 2) {
                        return
                    }
                    for (i in adaptiveFormats.length() - 1 downTo 0) {
                        val adaptiveFormat = adaptiveFormats.getJSONObject(i)

                        if (adaptiveFormat.has("audioTrack")) {
                            val audioTrack = adaptiveFormat.getJSONObject("audioTrack")
                            val displayName = audioTrack.getString("displayName")
                            if (StringUtils.contains(displayName, "original")) {
                                val id = audioTrack.getString("id")
                                val dotIndex = StringUtils.indexOf(id, ".")
                                if (dotIndex > -1) {
                                    val languageCode = id.substring(0, dotIndex)
                                    overrideLanguage = languageCode
                                    return
                                }
                            }
                        }
                    }
                } catch (ex: SocketTimeoutException) {
                    handleConnectionError("Connection timeout", ex)
                } catch (ex: IOException) {
                    handleConnectionError("Network error", ex)
                } catch (ex: Exception) {
                    Logger.printException({ "send failed" }, ex)
                } finally {
                    Logger.printDebug { "Fetching audio track request end (videoId: $videoId, took: ${(System.currentTimeMillis() - startTime)} ms)" }
                }
            }
        }

        private fun send(
            clientType: ClientType,
            videoId: String,
            requestHeader: Map<String, String>,
        ): HttpURLConnection? {
            Objects.requireNonNull(clientType)
            Objects.requireNonNull(videoId)
            Objects.requireNonNull(requestHeader)

            val startTime = System.currentTimeMillis()
            Logger.printDebug { "Fetching video streams for: $videoId using client: $clientType" }

            try {
                val requestBody = if (clientType.requireJS) {
                    // Javascript is used to get the signatureTimestamp
                    createJSRequestBody(
                        clientType = clientType,
                        videoId = videoId,
                        language = overrideLanguage,
                        isGVS = true,
                    )
                } else {
                    createApplicationRequestBody(
                        clientType = clientType,
                        videoId = videoId,
                        language = overrideLanguage
                    )
                }

                val connection =
                    getInnerTubeResponseConnectionFromRoute(
                        GET_STREAMING_DATA,
                        clientType,
                        replaceVisitorData(clientType, videoId, requestHeader)
                    )

                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.write(requestBody)

                val responseCode = connection.responseCode
                if (responseCode == 200) return connection

                // This situation likely means the patches are outdated.
                // Use a toast message that suggests updating.
                handleConnectionError(
                    ("Playback error (App is outdated?) " + clientType + ": "
                            + responseCode + " response: " + connection.responseMessage),
                    null
                )
            } catch (ex: SocketTimeoutException) {
                handleConnectionError("Connection timeout", ex)
            } catch (ex: IOException) {
                handleConnectionError("Network error", ex)
            } catch (ex: Exception) {
                Logger.printException({ "send failed" }, ex)
            } finally {
                Logger.printDebug { "Fetching video streams request end (videoId: $videoId, took: ${(System.currentTimeMillis() - startTime)} ms)" }
            }

            return null
        }

        private fun fetch(
            videoId: String,
            requestHeader: Map<String, String>,
            reasonSkipped: String,
        ): StreamingData? {
            Utils.verifyOffMainThread()
            lastSpoofedClient = null

            // Retry with different client if empty response body is received.
            for (clientType in CLIENT_ORDER_TO_USE) {
                if (clientType.requireAuth &&
                    StringUtils.isEmpty(requestHeader[AUTHORIZATION_HEADER])
                ) {
                    Logger.printDebug { "Skipped login-required client (clientType: $clientType, videoId: $videoId)" }
                    continue
                }
                // Decrypting signatureCipher takes time, so videos start 5 to 15 seconds late.
                // Unlike regular videos, no one would be willing to wait 5 to 15 seconds to play a 10-second Shorts.
                // For this reason, Shorts uses legacy clients that are not protected by signatureCipher.
                if (clientType.requireJS && reasonSkipped.isNotEmpty()) {
                    Logger.printDebug { "Skipped javascript required client (reasonSkipped: $reasonSkipped, clientType: $clientType, videoId: $videoId)" }
                    continue
                }
                if (!clientType.supportsCookies
                    && BaseSettings.DISABLE_AUTO_AUDIO_TRACKS.get()
                    && overrideLanguage.isEmpty()) {
                    setAudioTrackLanguage(videoId, requestHeader)
                }

                val connection = send(clientType, videoId, requestHeader)
                if (connection != null) {
                    try {
                        // gzip encoding doesn't response with content length (-1),
                        // but empty response body does.
                        if (connection.contentLength == 0) {
                            Logger.printDebug { "Received empty response (clientType: $clientType, videoId: $videoId)" }
                        } else {
                            BufferedInputStream(connection.inputStream).use { inputStream ->
                                ByteArrayOutputStream().use { stream ->
                                    val buffer = ByteArray(2048)
                                    var bytesRead: Int
                                    while ((inputStream.read(buffer)
                                            .also { bytesRead = it }) >= 0
                                    ) {
                                        stream.write(buffer, 0, bytesRead)
                                    }
                                    // Android Creator can't play livestreams, but it doesn't have an empty response (no formats available).
                                    // Since it doesn't have an empty response, the app doesn't try to fetch with another client, and it tries to play the livestream.
                                    // However, the response doesn't contain any formats available, so an exception is thrown.
                                    // As a workaround for this issue, if Android Creator is used for fetching, it should check if the video is a livestream.
                                    if (clientType == ClientType.ANDROID_CREATOR
                                        && liveStreams.check(buffer).isFiltered //
                                    ) {
                                        Logger.printDebug { "Ignore Android Studio spoofing as it is a livestream (videoId: $videoId)" }
                                    } else {
                                        lastSpoofedClient = null

                                        // Parses the Proto Buffer and returns StreamingData (GeneratedMessage).
                                        val streamBytes: ByteArray = stream.toByteArray()

                                        val playabilityStatus =
                                            getPlayabilityStatus(videoId, streamBytes)
                                        if (playabilityStatus == "OK") {
                                            var streamingData =
                                                parseFrom(ByteBuffer.wrap(streamBytes))

                                            if (streamingData != null) {
                                                if (clientType.requireJS) {
                                                    // ArrayList containing the deobfuscated streamingUrl
                                                    val arrayLists = getDeobfuscatedUrlArrayList(
                                                        clientType,
                                                        videoId,
                                                        streamBytes
                                                    )
                                                    if (arrayLists != null) {
                                                        // MutableMap containing the deobfuscated streamingUrl.
                                                        // This is used for clients where streamingUrl is obfuscated.
                                                        val deobfuscatedAdaptiveFormatsArrayList =
                                                            arrayLists.first
                                                        val deobfuscatedFormatsArrayList =
                                                            arrayLists.second
                                                        if (!deobfuscatedAdaptiveFormatsArrayList.isNullOrEmpty()) {
                                                            streamingData =
                                                                deobfuscateStreamingData(
                                                                    deobfuscatedUrlArrayList = deobfuscatedAdaptiveFormatsArrayList,
                                                                    isAdaptiveFormats = true,
                                                                    streamingData = streamingData
                                                                )
                                                        }
                                                        if (!deobfuscatedFormatsArrayList.isNullOrEmpty()) {
                                                            streamingData =
                                                                deobfuscateStreamingData(
                                                                    deobfuscatedUrlArrayList = deobfuscatedFormatsArrayList,
                                                                    isAdaptiveFormats = false,
                                                                    streamingData = streamingData
                                                                )
                                                        }

                                                        lastSpoofedClient = clientType
                                                        overrideLanguage = ""
                                                        return streamingData
                                                    }
                                                } else {
                                                    lastSpoofedClient = clientType
                                                    overrideLanguage = ""
                                                    return streamingData
                                                }
                                            } else {
                                                Logger.printDebug { "Ignore empty streamingData, (clientType: $clientType, videoId: $videoId)" }
                                            }
                                        } else if (playabilityStatus == "LIVE_STREAM_OFFLINE") {
                                            Logger.printDebug { "Ignore UPCOMING video (videoId: $videoId)" }
                                            return null
                                        } else {
                                            Logger.printDebug { "Ignore unplayable video, (playabilityStatus: $playabilityStatus, clientType: $clientType, videoId: $videoId)" }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        Logger.printException({ "Fetch failed while processing response data" }, ex)
                    }
                }
            }

            overrideLanguage = ""

            val showToast = BaseSettings.DEBUG_TOAST_ON_ERROR.get()
            handleConnectionError(
                str("revanced_spoof_streaming_data_failed_forbidden"),
                null,
                showToast
            )
            handleConnectionError(
                str("revanced_spoof_streaming_data_failed_forbidden_suggestion"),
                null,
                showToast
            )
            return null
        }
    }
}
