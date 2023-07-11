package app.revanced.patches.youtube.misc.debugging.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch(false)
@Name("Enable debug logging")
@Description("Adds debugging options.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.2")
class DebuggingPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_DEBUG_LOGGING"
            )
        )

        SettingsPatch.updatePatchStatus("enable-debug-logging")


        return PatchResultSuccess()
    }
}
