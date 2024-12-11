package app.revanced.patches.youtube.misc.watchhistory

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.WATCH_HISTORY
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.trackingurlhook.hookTrackingUrl
import app.revanced.patches.youtube.utils.trackingurlhook.trackingUrlHookPatch

@Suppress("unused")
val watchHistoryPatch = bytecodePatch(
    WATCH_HISTORY.title,
    WATCH_HISTORY.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        trackingUrlHookPatch,
    )

    execute {

        hookTrackingUrl("$MISC_PATH/WatchHistoryPatch;->replaceTrackingUrl(Landroid/net/Uri;)Landroid/net/Uri;")

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: WATCH_HISTORY"
            ),
            WATCH_HISTORY
        )

        // endregion

    }
}
