package app.revanced.patches.music.misc.watchhistory

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.WATCH_HISTORY
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.trackingurlhook.hookWatchHistory
import app.revanced.patches.shared.trackingurlhook.trackingUrlHookPatch

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
        hookWatchHistory()

        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_watch_history_type"
        )
    }

}