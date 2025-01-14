package app.revanced.extension.shared.patches.client

import android.os.Build
import app.revanced.extension.shared.patches.PatchStatus
import app.revanced.extension.shared.settings.BaseSettings
import org.apache.commons.lang3.ArrayUtils

/**
 * Used to fetch streaming data.
 */
object AppClient {
    // IOS
    /**
     * Video not playable: Paid / Movie / Private / Age-restricted
     * Note: Audio track available
     */
    private const val PACKAGE_NAME_IOS = "com.google.ios.youtube"

    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App Store page of the YouTube app](https://apps.apple.com/us/app/youtube-watch-listen-stream/id544007664/),
     * in the `What’s New` section.
     */
    private val CLIENT_VERSION_IOS = if (forceAVC())
        "17.40.5"
    else
        "19.29.1"

    private const val DEVICE_MAKE_IOS = "Apple"
    private const val OS_NAME_IOS = "iOS"

    /**
     * The device machine id for the iPhone 15 Pro Max (iPhone16,2),
     * used to get HDR with AV1 hardware decoding.
     * See [this GitHub Gist](https://gist.github.com/adamawolf/3048717) for more information.
     */
    private val DEVICE_MODEL_IOS = if (forceAVC())
        "iPhone12,5" // 11 Pro Max. (last device with iOS 13)
    else
        "iPhone16,2" // 15 Pro Max.
    private val OS_VERSION_IOS = if (forceAVC())
        "13.7.17H35" // Last release of iOS 13.
    else
        "17.7.2.21H221"
    private val USER_AGENT_VERSION_IOS = if (forceAVC())
        "13_7"
    else
        "17_7_2"
    private val USER_AGENT_IOS = iOSUserAgent(PACKAGE_NAME_IOS, CLIENT_VERSION_IOS)


    // IOS UNPLUGGED
    /**
     * Video not playable: Paid / Movie / Playlists / Music
     * Note: Audio track available
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
        "8.33"
    private val USER_AGENT_IOS_UNPLUGGED =
        iOSUserAgent(PACKAGE_NAME_IOS_UNPLUGGED, CLIENT_VERSION_IOS_UNPLUGGED)


    // ANDROID VR
    /**
     * Video not playable: Kids
     * Note: Audio track is not available
     *
     * Package name for YouTube VR (Google DayDream): com.google.android.apps.youtube.vr (Deprecated)
     * Package name for YouTube VR (Meta Quests): com.google.android.apps.youtube.vr.oculus
     * Package name for YouTube VR (ByteDance Pico 4): com.google.android.apps.youtube.vr.pico
     */
    private const val PACKAGE_NAME_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus"

    /**
     * The hardcoded client version of the Android VR app used for InnerTube requests with this client.
     *
     * It can be extracted by getting the latest release version of the app on
     * [the App Store page of the YouTube app](https://www.meta.com/en-us/experiences/2002317119880945/),
     * in the `Additional details` section.
     */
    private const val CLIENT_VERSION_ANDROID_VR = "1.61.48"

    /**
     * The device machine id for the Meta Quest 3, used to get opus codec with the Android VR client.
     * See [this GitLab](https://dumps.tadiphone.dev/dumps/oculus/eureka) for more information.
     */
    private const val DEVICE_MODEL_ANDROID_VR = "Quest 3"
    private const val DEVICE_MAKE_ANDROID_VR = "Oculus"
    private const val OS_VERSION_ANDROID_VR = "12"

    /**
     * The SDK version for Android 12 is 31,
     * but for some reason the build.props for the `Quest 3` state that the SDK version is 32.
     */
    private const val ANDROID_SDK_VERSION_ANDROID_VR = "32"
    private val USER_AGENT_ANDROID_VR =
        androidUserAgent(PACKAGE_NAME_ANDROID_VR, CLIENT_VERSION_ANDROID_VR, OS_VERSION_ANDROID_VR)


    // ANDROID UNPLUGGED
    /**
     * Video not playable: Playlists / Music
     * Note: Audio track is not available
     */
    private const val PACKAGE_NAME_ANDROID_UNPLUGGED = "com.google.android.apps.youtube.unplugged"
    private const val CLIENT_VERSION_ANDROID_UNPLUGGED = "8.16.0"

    /**
     * The device machine id for the Chromecast with Google TV 4K.
     * See [this GitLab](https://dumps.tadiphone.dev/dumps/google/kirkwood) for more information.
     */
    private const val DEVICE_MODEL_ANDROID_UNPLUGGED = "Google TV Streamer"
    private const val DEVICE_MAKE_ANDROID_UNPLUGGED = "Google"
    private const val OS_VERSION_ANDROID_UNPLUGGED = "14"
    private const val ANDROID_SDK_VERSION_ANDROID_UNPLUGGED = "34"
    private val USER_AGENT_ANDROID_UNPLUGGED = androidUserAgent(
        PACKAGE_NAME_ANDROID_UNPLUGGED,
        CLIENT_VERSION_ANDROID_UNPLUGGED,
        OS_VERSION_ANDROID_UNPLUGGED
    )


    // ANDROID CREATOR
    /**
     * Video not playable: Livestream / HDR
     * Note: Audio track is not available
     */
    private const val PACKAGE_NAME_ANDROID_CREATOR = "com.google.android.apps.youtube.creator"
    private const val CLIENT_VERSION_ANDROID_CREATOR = "23.47.101"

    private val USER_AGENT_ANDROID_CREATOR = androidUserAgent(
        PACKAGE_NAME_ANDROID_CREATOR,
        CLIENT_VERSION_ANDROID_CREATOR
    )


    // ANDROID MUSIC
    /**
     * Video not playable: All videos that can't be played on YouTube Music
     */
    private const val PACKAGE_NAME_ANDROID_MUSIC = "com.google.android.apps.youtube.music"

    /**
     * Older client versions don't seem to require poToken.
     * It is not the default client yet, as it requires sufficient testing.
     */
    private const val CLIENT_VERSION_ANDROID_MUSIC = "4.27.53"
    private val USER_AGENT_ANDROID_MUSIC = androidUserAgent(
        PACKAGE_NAME_ANDROID_MUSIC,
        CLIENT_VERSION_ANDROID_MUSIC
    )


    private fun androidUserAgent(
        packageName: String,
        clientVersion: String,
        osVersion: String? = Build.VERSION.RELEASE
    ): String {
        return packageName +
                "/" +
                clientVersion +
                " (Linux; U; Android " +
                osVersion +
                "; GB) gzip"
    }

    private fun iOSUserAgent(packageName: String, clientVersion: String): String {
        return packageName +
                "/" +
                clientVersion +
                "(" +
                DEVICE_MODEL_IOS +
                "; U; CPU iOS " +
                USER_AGENT_VERSION_IOS +
                " like Mac OS X)"
    }

    private fun forceAVC(): Boolean {
        return BaseSettings.SPOOF_STREAMING_DATA_IOS_FORCE_AVC.get()
    }

    fun availableClientTypes(preferredClient: ClientType): Array<ClientType> {
        val availableClientTypes = if (PatchStatus.SpoofStreamingDataMusic())
            ClientType.CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC
        else
            ClientType.CLIENT_ORDER_TO_USE_YOUTUBE

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
            return availableClientTypes
        }
    }

    @Suppress("DEPRECATION")
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
        val androidSdkVersion: String = Build.VERSION.SDK,
        /**
         * App version.
         */
        val clientVersion: String,
        /**
         * If the client can access the API logged in.
         * If false, 'Authorization' must not be included.
         */
        val supportsCookies: Boolean = true,
        /**
         * If the client can only access the API logged in.
         * If true, 'Authorization' must be included.
         */
        val requireAuth: Boolean = false,
        /**
         * Whether a poToken is required to get playback for more than 1 minute.
         */
        val requirePoToken: Boolean = false,
        /**
         * Friendly name displayed in stats for nerds.
         */
        val friendlyName: String
    ) {
        ANDROID_VR(
            id = 28,
            deviceMake = DEVICE_MAKE_ANDROID_VR,
            deviceModel = DEVICE_MODEL_ANDROID_VR,
            osVersion = OS_VERSION_ANDROID_VR,
            userAgent = USER_AGENT_ANDROID_VR,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_VR,
            clientVersion = CLIENT_VERSION_ANDROID_VR,
            friendlyName = "Android VR"
        ),
        ANDROID_UNPLUGGED(
            id = 29,
            deviceMake = DEVICE_MAKE_ANDROID_UNPLUGGED,
            deviceModel = DEVICE_MODEL_ANDROID_UNPLUGGED,
            osVersion = OS_VERSION_ANDROID_UNPLUGGED,
            userAgent = USER_AGENT_ANDROID_UNPLUGGED,
            androidSdkVersion = ANDROID_SDK_VERSION_ANDROID_UNPLUGGED,
            clientVersion = CLIENT_VERSION_ANDROID_UNPLUGGED,
            requireAuth = true,
            friendlyName = "Android TV"
        ),
        ANDROID_CREATOR(
            id = 14,
            userAgent = USER_AGENT_ANDROID_CREATOR,
            clientVersion = CLIENT_VERSION_ANDROID_CREATOR,
            requireAuth = true,
            friendlyName = "Android Studio"
        ),
        IOS_UNPLUGGED(
            id = 33,
            deviceMake = DEVICE_MAKE_IOS,
            deviceModel = DEVICE_MODEL_IOS,
            osName = OS_NAME_IOS,
            osVersion = OS_VERSION_IOS,
            userAgent = USER_AGENT_IOS_UNPLUGGED,
            clientVersion = CLIENT_VERSION_IOS_UNPLUGGED,
            requireAuth = true,
            friendlyName = if (forceAVC())
                "iOS TV Force AVC"
            else
                "iOS TV"
        ),
        IOS(
            id = 5,
            deviceMake = DEVICE_MAKE_IOS,
            deviceModel = DEVICE_MODEL_IOS,
            osName = OS_NAME_IOS,
            osVersion = OS_VERSION_IOS,
            userAgent = USER_AGENT_IOS,
            clientVersion = CLIENT_VERSION_IOS,
            supportsCookies = false,
            requirePoToken = true,
            friendlyName = if (forceAVC())
                "iOS Force AVC"
            else
                "iOS"
        ),
        ANDROID_MUSIC(
            id = 21,
            userAgent = USER_AGENT_ANDROID_MUSIC,
            clientVersion = CLIENT_VERSION_ANDROID_MUSIC,
            requireAuth = true,
            friendlyName = "Android Music"
        );

        val clientName: String = name

        companion object {
            val CLIENT_ORDER_TO_USE_YOUTUBE: Array<ClientType> = arrayOf(
                ANDROID_VR,
                ANDROID_UNPLUGGED,
                IOS_UNPLUGGED,
                ANDROID_CREATOR,
                IOS,
            )

            internal val CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC: Array<ClientType> = arrayOf(
                ANDROID_VR,
                ANDROID_MUSIC,
            )
        }
    }
}
