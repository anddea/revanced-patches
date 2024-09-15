package app.revanced.patches.youtube.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.captions.BaseAutoCaptionsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object AutoCaptionsPatch : BaseBytecodePatch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = setOf(
        BaseAutoCaptionsPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}