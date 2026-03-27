package app.morphe.patches.music.misc.watchhistory

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.patch.PatchList.WATCH_HISTORY
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.trackingurlhook.hookWatchHistory
import app.morphe.patches.shared.trackingurlhook.trackingUrlHookPatch

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
