package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.captions.BaseAutoCaptionsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object AutoCaptionsPatch : BaseBytecodePatch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = setOf(
        BaseAutoCaptionsPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

    }
}