package app.revanced.extension.shared.innertube.client

import android.annotation.SuppressLint
import android.os.Build
import app.revanced.extension.shared.settings.BaseSettings
import app.revanced.extension.shared.utils.PackageUtils
import org.apache.commons.lang3.ArrayUtils
import java.util.Locale

/**
 * Used to fetch streaming data.
 */
@Suppress("ConstantLocale", "unused")
object YouTubeClient {
    private const val CLIENT_SCREEN_WATCH = "WATCH"
    private const val CLIENT_SCREEN_EMBED = "EMBED"
    private const val CLIENT_PLATFORM_DESKTOP = "DESKTOP"
    private const val CLIENT_PLATFORM_GAME_CONSOLE = "GAME_CONSOLE"
    private const val CLIENT_PLATFORM_MOBILE = "MOBILE"
    private const val CLIENT_PLATFORM_TABLET = "TABLET"
    private const val CLIENT_PLATFORM_TV = "TV"

    private const val CLIENT_REFERER_FORMAT_TV = "https://www.youtube.com/tv#/watch?v=%s"
    private const val CLIENT_REFERER_FORMAT_WEB = "https://www.youtube.com/watch?v=%s"
    private const val CLIENT_REFERER_FORMAT_MWEB = "https://m.youtube.com/watch?v=%s"

    // IOS_DEPRECATED
    private const val PACKAGE_NAME_IOS_DEPRECATED = "com.google.ios.youtube"
    private val CLIENT_VERSION_IOS_DEPRECATED = if (forceAVC())
        "17.40.5"
    else
        "20.20.7"

    private val DEVICE_MODEL_IOS_DEPRECATED = if (forceAVC())
        "iPhone12,5" // 11 Pro Max. (last device with iOS 13)
    else
        "iPhone17,2" // 16 Pro Max.
    private val OS_VERSION_IOS_DEPRECATED = if (forceAVC())
        "13.7.17H35" // Last release of iOS 13.
    else
        "18.5.22F76"
    private val USER_AGENT_VERSION_IOS_DEPRECATED = if (forceAVC())
        "13_7"
    else
        "18_5"
    @SuppressLint("ConstantLocale")
    private val USER_AGENT_IOS_DEPRECATED =
        "$PACKAGE_NAME_IOS_DEPRECATED/$CLIENT_VERSION_IOS_DEPRECATED ($DEVICE_MODEL_IOS_DEPRECATED; U; CPU iOS $USER_AGENT_VERSION_IOS_DEPRECATED like Mac OS X; ${Locale.getDefault()})"

    // IOS UNPLUGGED
    /**
     * Video not playable: Paid / Movie / Playlists / Music.
     * Note: Audio track available.
     */
    private const val PACKAGE_NAME_IOS_UNPLUGGED = "com.google.ios.youtubeunplugged"

    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App Store page of the YouTube TV app](https://apps.apple.com/us/app/youtube-tv/id1193350206/),
     * in the `What’s New` section.
     */
    private val CLIENT_VERSION_IOS_UNPLUGGED = if (forceAVC())
        "6.45"
    else
        "9.33"
    private const val DEVICE_MAKE_IOS_UNPLUGGED = "Apple"
    private const val OS_NAME_IOS_UNPLUGGED = "iOS"

    /**
     * The device machine id for the iPhone 16 Pro Max (iPhone17,2),
     * used to get HDR with AV1 hardware decoding.
     * See [this GitHub Gist](https://gist.github.com/adamawolf/3048717) for more information.
     */
    private val DEVICE_MODEL_IOS_UNPLUGGED = if (forceAVC())
        "iPhone12,5" // 11 Pro Max. (last device with iOS 13)
    else
        "iPhone17,2" // 16 Pro Max.
    private val OS_VERSION_IOS_UNPLUGGED = if (forceAVC())
        "13.7.17H35" // Last release of iOS 13.
    else
        "18.6.2.22G100"
    private val USER_AGENT_VERSION_IOS_UNPLUGGED = if (forceAVC())
        "13_7"
    else
        "18_6_2"

    @SuppressLint("ConstantLocale")
    private val USER_AGENT_IOS_UNPLUGGED =
        "$PACKAGE_NAME_IOS_UNPLUGGED/$CLIENT_VERSION_IOS_UNPLUGGED ($DEVICE_MODEL_IOS_UNPLUGGED; U; CPU iOS $USER_AGENT_VERSION_IOS_UNPLUGGED like Mac OS X; ${Locale.getDefault()})"

    private const val DEVICE_MAKE_APPLE = "Apple"


    // ANDROID
    /**
     * This client can only be used when logged in.
     * Requires a DroidGuard PoToken to play videos longer than 1:00.
     */
    private const val PACKAGE_NAME_ANDROID = "com.google.android.youtube"
    private val CLIENT_VERSION_ANDROID = PackageUtils.getAppVersionName()
    private val USER_AGENT_ANDROID = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID,
        clientVersion = CLIENT_VERSION_ANDROID,
    )


    // ANDROID VR
    /**
     * Video not playable: Kids / Paid / Movie / Private / Age-restricted.
     * Note: Audio track is not available.
     *
     * This client can only be used when logged out.
     *
     * Package name for YouTube VR (Google DayDream): com.google.android.apps.youtube.vr (Deprecated)
     * Package name for YouTube VR (Meta Quests): com.google.android.apps.youtube.vr.oculus
     * Package name for YouTube VR (ByteDance Pico): com.google.android.apps.youtube.vr.pico
     */
    private const val PACKAGE_NAME_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus"

    /**
     * The hardcoded client version of the Android VR app used for InnerTube requests with this client.
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App Store page of the YouTube app](https://www.meta.com/en-us/experiences/2002317119880945/),
     * in the `Additional details` section.
     */
    private val CLIENT_VERSION_ANDROID_VR = if (disableAV1())
        "1.43.32" // Last version of minSdkVersion 24.
    else
        "1.65.10"

    /**
     * The device machine id for the Meta Quest 3, used to get opus codec with the Android VR client.
     * See [this GitLab](https://dumps.tadiphone.dev/dumps/oculus/eureka) for more information.
     */
    private val DEVICE_MODEL_ANDROID_VR = if (disableAV1())
        "Quest"
    else
        "Quest 3"
    private const val DEVICE_MAKE_ANDROID_VR = "Oculus"
    private val OS_VERSION_ANDROID_VR = if (disableAV1())
        "7.1.1"
    else
        "14"
    private val ANDROID_SDK_VERSION_ANDROID_VR = if (disableAV1())
        "25"
    else
        "34"
    private val BUILD_ID_ANDROID_VR = if (disableAV1())
        "NGI77B"
    else
        "UP1A.231005.007.A1"

    private val USER_AGENT_ANDROID_VR = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID_VR,
        clientVersion = CLIENT_VERSION_ANDROID_VR,
        osVersion = OS_VERSION_ANDROID_VR,
        deviceModel = DEVICE_MODEL_ANDROID_VR,
        buildId = BUILD_ID_ANDROID_VR
    )


    // ANDROID CREATOR
    /**
     * Video not playable: Livestream / HDR.
     * Note: Audio track is not available.
     */
    private const val PACKAGE_NAME_ANDROID_CREATOR = "com.google.android.apps.youtube.creator"
    private const val CLIENT_VERSION_ANDROID_CREATOR = "24.01.000"

    /**
     * The device machine id for the Google Pixel 7a.
     * See [this GitLab](https://dumps.tadiphone.dev/dumps/google/lynx) for more information.
     */
    private const val DEVICE_MODEL_ANDROID_CREATOR = "Pixel 7a"
    private const val DEVICE_MAKE_ANDROID_CREATOR = "Google"
    private const val OS_VERSION_ANDROID_CREATOR = "13"
    private const val ANDROID_SDK_VERSION_ANDROID_CREATOR = "33"
    private const val BUILD_ID_ANDROID_CREATOR = "TQ3A.230901.001.C3" // Pixel 7a Verizon

    private val USER_AGENT_ANDROID_CREATOR = androidUserAgent(
        packageName = PACKAGE_NAME_ANDROID_CREATOR,
        clientVersion = CLIENT_VERSION_ANDROID_CREATOR,
        osVersion = OS_VERSION_ANDROID_CREATOR,
        deviceModel = DEVICE_MODEL_ANDROID_CREATOR,
        buildId = BUILD_ID_ANDROID_CREATOR,
    )


    // IPAD OS
    /**
     * Video not playable: Paid / Movie / Private / Age-restricted.
     * Note: Audio track available, AV1 hardware decoding is not supported.
     *
     * It appears that iOSGuard PoToken has been rolled out to YouTube 20+.
     * See: [uYouEnhanced#908](https://github.com/arichornlover/uYouEnhanced/issues/908) and
     * [yt-dlp#14133 (comment)](https://github.com/yt-dlp/yt-dlp/issues/14133#issuecomment-3217679575).
     *
     * Nevertheless, this client was added based on two reports:
     * 1. According to a uYouEnhanced user, spoofing the app version to 19.01.1 is a temporary workaround.
     * 2. Over the past few months, BotGuard PoToken has not been used on WEB_SAFARI (iPadOS).
     *
     * It may still not work for some users.
     *
     * Update:
     * Playback issue occurs when the value of 'iosPlayerConfig.requestIosguardDataAfterPlaybackStarts' in the player response is 'true'.
     * It appears that blocking the '/att/get' endpoint can prevent this, but I'm not sure.
     * (More samples are needed)
     */
    private const val PACKAGE_NAME_IPADOS = "com.google.ios.youtube"

    /**
     * YouTube 19.22.3 is the minimum version that supports the OPUS codec.
     */
    private const val CLIENT_VERSION_IPADOS = "19.22.3"
    private const val DEVICE_MAKE_IPADOS = DEVICE_MAKE_APPLE

    /**
     * The device machine id for the iPad 6th Gen (iPad7,6).
     * See [this GitHub Gist](https://gist.github.com/adamawolf/3048717) for more information.
     *
     * Based on Google's actions to date, PoToken may not be required on devices with very low specs.
     * For example, suppose the User-Agent for a PlayStation 3 (with 256MB of RAM) is used.
     * Accessing 'Web' (https://www.youtube.com) will redirect to 'TV' (https://www.youtube.com/tv).
     * 'TV' target devices with very low specs, such as embedded devices, game consoles, and blu-ray players, so PoToken is not required.
     *
     * For this reason, the device machine id for the iPad 6th Gen (with 2GB of RAM),
     * the lowest spec device capable of running iPadOS 17, was used.
     */
    private const val DEVICE_MODEL_IPADOS = "iPad7,6"
    private const val OS_NAME_IPADOS = "iPadOS"
    /**
     * iPadOS 17 is the minimum version that supports the OPUS codec.
     */
    private const val OS_VERSION_IPADOS = "17.7.10.21H450"
    private const val USER_AGENT_VERSION_IPADOS = "17_7_10"
    private val USER_AGENT_IPADOS =
        "$PACKAGE_NAME_IPADOS/$CLIENT_VERSION_IPADOS ($DEVICE_MODEL_IPADOS; U; CPU iPadOS $USER_AGENT_VERSION_IPADOS like Mac OS X; ${Locale.getDefault()})"


    // VISION OS
    private const val CLIENT_VERSION_VISIONOS = "0.1"
    private const val DEVICE_MAKE_VISIONOS = DEVICE_MAKE_APPLE
    private const val DEVICE_MODEL_VISIONOS = "RealityDevice14,1"
    private const val OS_NAME_VISIONOS = "visionOS"
    private const val OS_VERSION_VISIONS = "1.3.21O771"
    private const val USER_AGENT_VISIONOS =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"


    // TVHTML5
    /**
     * Video not playable: None.
     * Note: Both 'Authorization' and 'Set-Cookie' are supported.
     */
    private const val CLIENT_VERSION_TVHTML5 = "7.20250917.13.00"
    private const val USER_AGENT_TVHTML5 =
        "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; Xbox)"


    // TVHTML5 EMBEDDED
    /**
     * Only embeddable videos available.
     * Note: Both 'Authorization' and 'Set-Cookie' are supported.
     */
    private const val CLIENT_VERSION_TVHTML5_EMBEDDED = "2.0"


    // MWEB
    /**
     * Video not playable: Paid / Movie / Private / Age-restricted.
     * Note: Audio track is not available.
     * Note: Only 'Set-Cookie' is supported.
     */
    private const val CLIENT_VERSION_MWEB = "2.20250918.09.00"
    private const val USER_AGENT_MWEB =
        "Mozilla/5.0 (Android 16; Mobile; rv:140.0) Gecko/140.0 Firefox/140.0"


    /**
     * Same format as Android YouTube User-Agent.
     * Example: 'com.google.android.youtube/20.32.35(Linux; U; Android 15; en_US; SM-S928U1 Build/AP3A.240905.015.A2) gzip'
     * Source: https://whatmyuseragent.com/apps/youtube.
     */
    private fun androidUserAgent(
        packageName: String,
        clientVersion: String,
        osVersion: String? = Build.VERSION.RELEASE,
        deviceModel: String? = Build.MODEL,
        buildId: String? = Build.ID,
    ): String =
        "$packageName/$clientVersion(Linux; U; Android $osVersion; ${Locale.getDefault()}; $deviceModel Build/$buildId) gzip"

    private fun disableAV1(): Boolean {
        return BaseSettings.SPOOF_STREAMING_DATA_VR_DISABLE_AV1.get()
    }

    private fun forceAVC(): Boolean {
        return BaseSettings.SPOOF_STREAMING_DATA_IOS_FORCE_AVC.get()
    }

    private fun useJS(): Boolean {
        return BaseSettings.SPOOF_STREAMING_DATA_USE_JS.get()
    }

    @JvmStatic
    fun availableClientTypes(preferredClient: ClientType): Array<ClientType> {
        val availableClientTypes: Array<ClientType> = if (useJS()) {
            if (preferredClient == ClientType.MWEB) {
                // If playback fails with MWEB, it will fall back to TV.
                ClientType.CLIENT_ORDER_TO_USE_JS_PREFER_TV
            } else {
                // Default order of JS clients.
                ClientType.CLIENT_ORDER_TO_USE_JS
            }
        } else {
            ClientType.CLIENT_ORDER_TO_USE
        }

        if (ArrayUtils.contains(availableClientTypes, preferredClient)) {
            val clientToUse: Array<ClientType?> = arrayOfNulls(availableClientTypes.size)
            clientToUse[0] = preferredClient
            var i = 1
            for (c in availableClientTypes) {
                if (c != preferredClient) {
                    clientToUse[i++] = c
                }
            }
            return clientToUse.filterNotNull().toTypedArray()
        } else {
            BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.resetToDefault()
            return availableClientTypes
        }
    }

    @Suppress("DEPRECATION", "unused")
    enum class ClientType(
        /**
         * [YouTube client type](https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients)
         */
        val id: Int,
        /**
         * Device model, equivalent to [Build.MANUFACTURER] (System property: ro.product.vendor.manufacturer)
         */
        val deviceMake: String = Build.MANUFACTURER,
        /**
         * Device model, equivalent to [Build.MODEL] (System property: ro.product.model)
         */
        val deviceModel: String = Build.MODEL,
        /**
         * Device OS name.
         */
        val osName: String = "Android",
        /**
         * Device OS version, equivalent to [Build.VERSION.RELEASE] (System property: ro.system.build.version.release)
         */
        val osVersion: String = Build.VERSION.RELEASE,
        /**
         * Client user-agent.
         */
        val userAgent: String,
        /**
         * Android SDK version, equivalent to [Build.VERSION.SDK] (System property: ro.build.version.sdk)
         */
        val androidSdkVersion: String? = null,
        /**
         * App version.
         */
        val clientVersion: String,
        /**
         * Client platform enum.
         */
        val clientPlatform: String = CLIENT_PLATFORM_MOBILE,
        /**
         * Client screen enum.
         */
        val clientScreen: String = CLIENT_SCREEN_WATCH,
        /**
         * If the client can access the API logged in.
         * If false, 'Authorization' or 'SessionId' must not be included.
         */
        val supportsCookies: Boolean = true,
        /**
         * Referer of contentPlaybackContext.
         */
        val refererFormat: String? = null,
        /**
         * If the client can only access the API logged in.
         * If true, 'Authorization' or 'SessionId' must be included.
         */
        val requireAuth: Boolean = false,
        /**
         * If the client requires GVS PoToken.
         */
        val requirePoToken: Boolean = false,
        /**
         * The streaming url has an obfuscated 'n' parameter.
         * If true, javascript must be fetched to decrypt the 'n' parameter.
         */
        val requireJS: Boolean = false,
        /**
         * Client name for innertube body.
         */
        val clientName: String,
        /**
         * Friendly name displayed in stats for nerds.
         */
        val friendlyName: String
    ) {
        ANDROID(
            id = 3,
            userAgent = USER_AGENT_ANDROID,
            androidSdkVersion = Build.VERSION.SDK,
            clientVersion = CLIENT_VERSION_ANDROID,
            clientName = "ANDROID",
            friendlyName = "Android"
        ),
        ANDROID_VR(
            id = 28,
            deviceMake = DEVICE_MAKE_ANDROID_VR,
            deviceModel = DEVICE_MODEL_ANDROID_VR,
            osVersion = OS_VERSION_ANDROID_VR,
            userAgent = USER_AGENT_ANDROID_VR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_VR,
            clientVersion = CLIENT_VERSION_ANDROID_VR,
            supportsCookies = false,
            clientName = "ANDROID_VR",
            friendlyName = if (disableAV1())
                "Android VR No AV1"
            else
                "Android VR"
        ),
        ANDROID_CREATOR(
            id = 14,
            deviceMake = DEVICE_MAKE_ANDROID_CREATOR,
            deviceModel = DEVICE_MODEL_ANDROID_CREATOR,
            osVersion = OS_VERSION_ANDROID_CREATOR,
            userAgent = USER_AGENT_ANDROID_CREATOR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_CREATOR,
            clientVersion = CLIENT_VERSION_ANDROID_CREATOR,
            requireAuth = true,
            clientName = "ANDROID_CREATOR",
            friendlyName = "Android Studio"
        ),
        IOS_DEPRECATED(
            id = 5,
            deviceMake = DEVICE_MAKE_IOS_UNPLUGGED,
            deviceModel = DEVICE_MODEL_IOS_DEPRECATED,
            osName = OS_NAME_IOS_UNPLUGGED,
            osVersion = OS_VERSION_IOS_DEPRECATED,
            userAgent = USER_AGENT_IOS_DEPRECATED,
            clientVersion = CLIENT_VERSION_IOS_DEPRECATED,
            supportsCookies = false,
            clientName = "IOS",
            friendlyName = if (forceAVC())
                "iOS Force AVC"
            else
                "iOS"
        ),
        IOS_UNPLUGGED(
            id = 33,
            deviceMake = DEVICE_MAKE_IOS_UNPLUGGED,
            deviceModel = DEVICE_MODEL_IOS_UNPLUGGED,
            osName = OS_NAME_IOS_UNPLUGGED,
            osVersion = OS_VERSION_IOS_UNPLUGGED,
            userAgent = USER_AGENT_IOS_UNPLUGGED,
            clientVersion = CLIENT_VERSION_IOS_UNPLUGGED,
            requireAuth = true,
            clientName = "IOS_UNPLUGGED",
            friendlyName = if (forceAVC())
                "iOS TV Force AVC"
            else
                "iOS TV"
        ),

        // Fallback client, not yet released.
        // PoToken required?
        IPADOS(
            id = 5,
            deviceMake = DEVICE_MAKE_IPADOS,
            deviceModel = DEVICE_MODEL_IPADOS,
            osName = OS_NAME_IPADOS,
            osVersion = OS_VERSION_IPADOS,
            userAgent = USER_AGENT_IPADOS,
            clientVersion = CLIENT_VERSION_IPADOS,
            clientPlatform = CLIENT_PLATFORM_TABLET,
            supportsCookies = false,
            clientName = "IOS",
            friendlyName = "iPadOS"
        ),
        // Unreleased.
        VISIONOS(
            id = 101,
            deviceMake = DEVICE_MAKE_VISIONOS,
            deviceModel = DEVICE_MODEL_VISIONOS,
            osName = OS_NAME_VISIONOS,
            osVersion = OS_VERSION_VISIONS,
            userAgent = USER_AGENT_VISIONOS,
            clientVersion = CLIENT_VERSION_VISIONOS,
            clientPlatform = CLIENT_PLATFORM_DESKTOP,
            supportsCookies = false,
            clientName = "VISIONOS",
            friendlyName = "visionOS"
        ),
        TV(
            id = 7,
            clientVersion = CLIENT_VERSION_TVHTML5,
            clientPlatform = CLIENT_PLATFORM_GAME_CONSOLE,
            userAgent = USER_AGENT_TVHTML5,
            requireJS = true,
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            clientName = "TVHTML5",
            friendlyName = "TV"
        ),

        // Unused client.
        TV_EMBEDDED(
            id = 85,
            clientVersion = CLIENT_VERSION_TVHTML5_EMBEDDED,
            clientPlatform = CLIENT_PLATFORM_TV,
            clientScreen = CLIENT_SCREEN_EMBED,
            userAgent = USER_AGENT_TVHTML5,
            requireJS = true,
            refererFormat = CLIENT_REFERER_FORMAT_TV,
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            friendlyName = "TV Embedded"
        ),
        MWEB(
            id = 2,
            clientVersion = CLIENT_VERSION_MWEB,
            userAgent = USER_AGENT_MWEB,
            requireJS = true,
            requirePoToken = true,
            // Android YouTube app does not support 'Cookie'.
            supportsCookies = false,
            refererFormat = CLIENT_REFERER_FORMAT_MWEB,
            clientName = "MWEB",
            friendlyName = "Mobile Web"
        );

        companion object {
            val CLIENT_ORDER_TO_USE: Array<ClientType> = arrayOf(
                ANDROID_VR,
                VISIONOS,
                ANDROID_CREATOR,
                IPADOS,
                IOS_DEPRECATED,
            )
            val CLIENT_ORDER_TO_USE_JS: Array<ClientType> = arrayOf(
                ANDROID_VR,
                VISIONOS,
                ANDROID_CREATOR,
                IPADOS,
                TV,
                MWEB,
                IOS_DEPRECATED,
            )
            val CLIENT_ORDER_TO_USE_JS_PREFER_TV: Array<ClientType> = arrayOf(
                TV,
                ANDROID_VR,
                VISIONOS,
                ANDROID_CREATOR,
                IPADOS,
                MWEB,
                IOS_DEPRECATED,
            )
        }
    }
}
