package app.morphe.extension.youtube.patches.general

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import app.morphe.extension.shared.settings.Setting
import app.morphe.extension.shared.settings.Setting.Availability
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.settings.Settings
import org.apache.commons.lang3.StringUtils


@Suppress("unused", "DEPRECATION")
object ChangeStartPagePatch {
    /**
     * Intent action when YouTube is cold started from the launcher.
     *
     * If you don't check this, the hooking will also apply in the following cases:
     * Case 1. The user clicked Shorts button on the YouTube shortcut.
     * Case 2. The user clicked Shorts button on the YouTube widget.
     * In this case, instead of opening Shorts, the start page specified by the user is opened.
     */
    private const val ACTION_MAIN = "android.intent.action.MAIN"

    private const val URL_ACTIVITY_CLASS_DESCRIPTOR =
        "com.google.android.apps.youtube.app.application.Shell_UrlActivity"

    private var START_PAGE = Settings.CHANGE_START_PAGE.get()
    private val ALWAYS_CHANGE_START_PAGE = Settings.CHANGE_START_PAGE_TYPE.get()

    /**
     * There is an issue where the back button on the toolbar doesn't work properly.
     * As a workaround for this issue, instead of overriding the browserId multiple times, just override it once.
     */
    private var appLaunched = false

    @JvmStatic
    fun overrideBrowseId(original: String): String {
        val browseId = START_PAGE.browseId
        if (browseId == null) {
            return original
        }
        if (!ALWAYS_CHANGE_START_PAGE && appLaunched) {
            Logger.printDebug { "Ignore override browseId as the app already launched" }
            return original
        }
        appLaunched = true

        Logger.printDebug { "Changing browseId to $browseId" }
        return browseId
    }

    @JvmStatic
    @SuppressLint("UseKtx")
    fun overrideIntent(intent: Intent) {
        val action = START_PAGE.action
        val url = START_PAGE.url
        if (action == null && url == null) {
            return
        }
        if (!StringUtils.equals(intent.action, ACTION_MAIN)) {
            Logger.printDebug {
                "Ignore override intent action" +
                        " as the current activity is not the entry point of the application"
            }
            return
        }
        if (!ALWAYS_CHANGE_START_PAGE && appLaunched) {
            Logger.printDebug { "Ignore override intent as the app already launched" }
            return
        }
        appLaunched = true

        if (action != null) {
            Logger.printDebug { "Changing intent action to $action" }
            intent.setAction(action)
        } else if (url != null) {
            Logger.printDebug { "Changing url to $url" }
            intent.setAction("android.intent.action.VIEW")
            intent.setData(Uri.parse(url))
            intent.putExtra("alias", URL_ACTIVITY_CLASS_DESCRIPTOR)
        } else {
            START_PAGE = Settings.CHANGE_START_PAGE.defaultValue
            Settings.CHANGE_START_PAGE.resetToDefault()
            Logger.printException { "Unknown start page: $START_PAGE" } // Should never happen
        }
    }

    enum class StartPage(
        val action: String? = null,
        val browseId: String? = null,
        val url: String? = null,
    ) {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL,

        /**
         * Browse id.
         */
        ALL_SUBSCRIPTIONS(browseId = "FEchannels"),
        BROWSE(browseId = "FEguide_builder"),
        EXPLORE(browseId = "FEexplore"),
        HISTORY(browseId = "FEhistory"),
        HYPE(browseId = "FEhype_leaderboard"),
        LIBRARY(browseId = "FElibrary"),
        MOVIE(browseId = "FEstorefront"),
        NOTIFICATIONS(browseId = "FEactivity"),
        PLAYLISTS(browseId = "FEplaylist_aggregation"),
        SUBSCRIPTIONS(browseId = "FEsubscriptions"),
        TRENDING(browseId = "FEtrending"),
        YOUR_CLIPS(browseId = "FEclips"),

        /**
         * Channel id, this can be used as a browseId.
         */
        COURSES(browseId = "UCtFRv9O2AHqOZjjynzrv-xg"),
        FASHION(browseId = "UCrpQ4p1Ql_hG8rKXIKM1MOQ"),
        GAMING(browseId = "UCOpNcN46UbXVtpKMrmU4Abg"),
        LIVE(browseId = "UC4R8DWoMoI7CAwX8_LjQHig"),
        MUSIC(browseId = "UC-9-kyTW8ZkZNDHQJ6FgpwQ"),
        NEWS(browseId = "UCYfdidRxbB8Qhf0Nx7ioOYw"),
        SHOPPING(browseId = "UCkYQyvc_i9hXEo4xic9Hh2g"),
        SPORTS(browseId = "UCEgdi0XIXXZ-qJOFPf4JSKw"),
        VIRTUAL_REALITY(browseId = "UCzuqhhs6NWbgTzMuM09WKDQ"),

        /**
         * Playlist id, this can be used as a browseId.
         */
        LIKED_VIDEO(browseId = "VLLL"),
        WATCH_LATER(browseId = "VLWL"),

        /**
         * Intent action.
         */
        SEARCH(action = "com.google.android.youtube.action.open.search"),
        SHORTS(action = "com.google.android.youtube.action.open.shorts"),

        /**
         * URL.
         *
         * URL opens after the home feed is opened.
         * Use this only if browseId cannot be used.
         */
        PODCASTS(url = "www.youtube.com/podcasts");
    }

    class ChangeStartPageTypeAvailability : Availability {
        override fun isAvailable(): Boolean {
            return Settings.CHANGE_START_PAGE.get() != StartPage.ORIGINAL
        }

        override fun getParentSettings(): MutableList<Setting<*>> {
            return arrayOf(Settings.CHANGE_START_PAGE).toMutableList()
        }
    }
}
