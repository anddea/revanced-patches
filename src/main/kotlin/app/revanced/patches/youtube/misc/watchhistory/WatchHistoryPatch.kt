package app.revanced.patches.youtube.misc.watchhistory

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.trackingurlhook.TrackingUrlHookPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object WatchHistoryPatch : BaseBytecodePatch(
    name = "Spoof watch history",
    description = "Adds an option to change the domain of the watch history or check its status.",
    dependencies = setOf(
        SettingsPatch::class,
        TrackingUrlHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    override fun execute(context: BytecodeContext) {

        TrackingUrlHookPatch.hookTrackingUrl("$MISC_PATH/WatchHistoryPatch;->replaceTrackingUrl(Landroid/net/Uri;)Landroid/net/Uri;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: WATCH_HISTORY"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
